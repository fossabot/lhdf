package de.lheinrich.lhdf.network;

import de.lheinrich.lhdf.security.AESKeySize;
import de.lheinrich.lhdf.security.Crypter;
import de.lheinrich.lhdf.security.ECKeySize;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * Copyright (c) 2018 Lennart Heinrich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

public class SecureNetwork {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private final String id;
    private final TreeMap<String, NetworkServer> network = new TreeMap<>();
    private final Map<String, NetworkHandler> handlerMap = new TreeMap<>();
    private final String authentication;
    private final String hashAlgorithm;
    private final AESKeySize aesKeySize;
    private final ECKeySize ecKeySize;

    private ServerSocket serverSocket;

    public SecureNetwork(int masterPort, String authentication, String hashAlgorithm, AESKeySize aesKeySize, ECKeySize ecKeySize) {
        this.id = "master";
        this.authentication = authentication;
        this.hashAlgorithm = hashAlgorithm;
        this.aesKeySize = aesKeySize;
        this.ecKeySize = ecKeySize;
        bind(masterPort);
    }

    public SecureNetwork(String host, int port, String authentication, String hashAlgorithm, AESKeySize aesKeySize, ECKeySize ecKeySize) {
        this.id = UUID.randomUUID().toString().replace("-", "");
        this.authentication = authentication;
        this.hashAlgorithm = hashAlgorithm;
        this.aesKeySize = aesKeySize;
        this.ecKeySize = ecKeySize;
        bind(0);
        connectToSlaves(host, port);
    }

    private void bind(int port) {
        try {
            this.serverSocket = new ServerSocket(port);
            new Thread(() -> {
                try {
                    while (serverSocket != null) {
                        var socket = this.serverSocket.accept();
                        socket.setKeepAlive(true);

                        executor.submit(() -> handleConnection(socket));
                    }
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                }
            }).start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        this.registerHandler("ping", new NetworkHandler() {
            @Override
            public void handle() {
                write("pong");
            }
        });
        this.registerHandler("Liste alle Slaves auf", new NetworkHandler() {
            @Override
            public void handle() {
                var servers = new TreeMap<String, TreeMap.SimpleEntry<String, Integer>>();
                network.forEach((id, server) -> servers.put(id, new TreeMap.SimpleEntry<>(server.getHost(), server.getPort())));

                write(servers);
            }
        });
    }

    private void connectToSlaves(String host, int port) {
        var masterKey = connectTo(this.id, this.serverSocket.getLocalPort(), host, port, false, null, this.authentication, "Liste alle Slaves auf", new NetworkHandler() {
            @Override
            public void handle() {
                var slaves = (TreeMap<String, TreeMap.SimpleEntry<String, Integer>>) read();
                for (var slave : slaves.entrySet()) {
                    network.put(slave.getKey(), new NetworkServer(id, serverSocket.getLocalPort(), slave.getKey(), slave.getValue().getKey(), slave.getValue().getValue(), authentication, null));
                }
            }
        });
        network.put("master", new NetworkServer(this.id, this.serverSocket.getLocalPort(), "master", host, port, this.authentication, masterKey));
    }

    protected static SecretKey connectTo(String networkId, int networkPort, String host, int port, boolean known, SecretKey key, String authentication, String handlerName, NetworkHandler handler) {
        var finalKey = key;

        try (var socket = new Socket(host, port);
             var out = new ObjectOutputStream(socket.getOutputStream());
             var in = new ObjectInputStream(socket.getInputStream())) {

            out.writeUTF(networkId);
            out.flush();

            out.writeBoolean(known);
            out.flush();

            if (!in.readBoolean() || !known) {
                out.writeInt(networkPort);
                out.flush();

                var keyPair = Crypter.generateECKeyPair(in.readInt());
                var privateKey = keyPair.getPrivate();
                var publicKey = (PublicKey) in.readObject();

                out.writeObject(keyPair.getPublic());
                out.flush();

                finalKey = Crypter.generateEC(in.readUTF(), in.readInt(), privateKey, publicKey);
            }

            handler.init(in, out, finalKey);
            handler.write(authentication);
            handler.write(handlerName);
            handler.handle();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return finalKey;
    }

    private void handleConnection(Socket socket) {
        try (var in = new ObjectInputStream(socket.getInputStream());
             var out = new ObjectOutputStream(socket.getOutputStream())) {
            var host = socket.getInetAddress().getHostAddress();
            var id = in.readUTF();
            SecretKey key;

            if (in.readBoolean() && this.network.containsKey(id)) {
                out.writeBoolean(true);
                out.flush();

                key = this.network.get(id).getKey();
            } else {
                out.writeBoolean(false);
                out.flush();

                var port = in.readInt();

                out.writeInt(this.ecKeySize.getSize());
                out.flush();

                var keyPair = Crypter.generateECKeyPair(this.ecKeySize.getSize());

                out.writeObject(keyPair.getPublic());
                out.flush();

                var privateKey = keyPair.getPrivate();
                var publicKey = (PublicKey) in.readObject();

                out.writeUTF(this.hashAlgorithm);
                out.flush();

                out.writeInt(this.aesKeySize.getSize());
                out.flush();

                key = Crypter.generateEC(this.hashAlgorithm, this.aesKeySize.getSize(), privateKey, publicKey);

                var server = new NetworkServer(this.id, this.serverSocket.getLocalPort(), id, host, port, authentication, key);
                this.network.remove(id);
                this.network.put(id, server);
            }

            var authentication = (String) Crypter.toObject(Crypter.decrypt("AES", (byte[]) in.readObject(), key));
            if (!this.authentication.equals(authentication))
                throw new Exception("Authentication failed");
            var handler = (String) Crypter.toObject(Crypter.decrypt("AES", (byte[]) in.readObject(), key));
            handleRequest(handler, in, out, key);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                // ignore (already closed)
            }
        }
    }

    private void handleRequest(String handlerName, ObjectInputStream in, ObjectOutputStream out, SecretKey key) {
        NetworkHandler handler;
        if (this.handlerMap.containsKey(handlerName))
            handler = handlerMap.get(handlerName);
        else if (this.handlerMap.containsKey(""))
            handler = handlerMap.get("");
        else
            return;

        handler.init(in, out, key);
        handler.handle();
    }

    public void unregisterHandler(String handlerName) {
        this.handlerMap.remove(handlerName);
    }

    public void registerHandler(String handlerName, NetworkHandler handler) {
        this.handlerMap.remove(handlerName);
        this.handlerMap.put(handlerName, handler);
    }

    public NetworkServer getNetworkServer(String id) {
        return this.network.get(id);
    }

    public Collection<NetworkServer> getNetworkServers() {
        return this.network.values();
    }

    public int getPort() {
        return this.serverSocket.getLocalPort();
    }

    public void close() {
        try {
            this.serverSocket.close();
            this.serverSocket = null;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getId() {
        return this.id;
    }
}