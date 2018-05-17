package de.lheinrich.lhdf.network.old;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

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

public class UnetkitServerSecure {

    private TreeMap<String, SecretKey> keyStore = new TreeMap<>();
    private final Map<Integer, ArrayList<Runnable>> tasks = new TreeMap<>();
    private Thread thread;
    private UnetkitServerHandler handler;
    private ServerSocket serverSocket;

    /**
     * Binds the server to a specified port and process requests with the
     * handler (encrypted)
     *
     * @param port           Server port to bind
     * @param handler        Handler to process requests
     * @param handlerThreads Threads count to start for processing
     *                       (Runtime.getRuntime().availableProcessors() is enough, maybe double it *2
     *                       but more Threads, more CPU usage)
     */
    public UnetkitServerSecure(int port, UnetkitServerHandler handler, int handlerThreads) {
        this.handler = handler;

        try {
            serverSocket = new ServerSocket(port, Integer.MAX_VALUE);
            startThreads(handlerThreads);

            thread = new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        Socket socket = serverSocket.accept();
                        socket.setKeepAlive(true);

                        sortTask(() -> {
                            ObjectInputStream in = null;
                            ObjectOutputStream out = null;

                            try {
                                in = new ObjectInputStream(socket.getInputStream());
                                out = new ObjectOutputStream(socket.getOutputStream());
                                var client = socket.getInetAddress().getHostAddress();

                                var keyStoreName = client + " on " + port;
                                SecretKey key = null;
                                var hasKey = keyStore.containsKey(keyStoreName);
                                byte[] keyHash;

                                if (hasKey) {
                                    key = keyStore.get(keyStoreName);
                                    keyHash = Crypter.hash("SHA3-224", key.getEncoded());
                                } else {
                                    keyHash = "server".getBytes();
                                }
                                out.writeObject(keyHash);

                                var clientKey = (byte[]) in.readObject();
                                var correctKey = Arrays.equals(clientKey, keyHash);

                                if (!correctKey) {
                                    var keyPair = Crypter.generateECKeyPair(ECKeySize.LOWEST.getSize());
                                    var publicKey = (PublicKey) in.readObject();

                                    out.writeObject(keyPair.getPublic());
                                    key = Crypter.generateEC("SHA3-224", AESKeySize.LOW.getSize(), keyPair.getPrivate(), publicKey);

                                    if (hasKey) {
                                        keyStore.remove(keyStoreName);
                                    }
                                    keyStore.put(keyStoreName, key);
                                }

                                var decryptedObject = Crypter.decrypt("AES", (byte[]) in.readObject(), key);
                                var object = Crypter.toObject(decryptedObject);

                                var byteResponse = Crypter.toByteArray(this.handler.process(object));
                                var encryptedResponse = Crypter.encrypt("AES", byteResponse, key);

                                out.writeObject(encryptedResponse);
                                out.flush();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            } finally {
                                try {
                                    if (in != null) {
                                        in.close();
                                    }

                                    if (out != null) {
                                        out.close();
                                    }

                                    if (!socket.isClosed()) {
                                        socket.close();
                                    }
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });
                    } catch (IOException ex) {
                        if (!ex.getMessage().toLowerCase().contains("socket closed")) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
            thread.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Closes the ServerSockt and interrup the Thread
     */
    public void close() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                thread.interrupt();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Sets the handler
     *
     * @param handler Handler to process requests
     */
    public void setHandler(UnetkitServerHandler handler) {
        this.handler = handler;
    }

    /**
     * Check socket is closed
     *
     * @return boolean Socket is closed
     */
    public boolean isClosed() {
        return serverSocket == null || serverSocket.isClosed();
    }

    private void startThreads(int threads) {
        for (int i = 0; i < threads; i++) {
            int id = i;
            var myTasks = new ArrayList<Runnable>();
            tasks.put(id, myTasks);

            new Thread(() -> {
                while (serverSocket != null && !serverSocket.isClosed()) {
                    if (myTasks.isEmpty()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                        }
                    } else {
                        try {
                            var task = myTasks.get(0);
                            myTasks.remove(task);
                            task.run();
                        } catch (NullPointerException ex) {
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    private void sortTask(Runnable task) {
        var threadId = 0;
        var taskCount = Integer.MAX_VALUE;
        for (var lists : tasks.entrySet()) {
            if (lists.getValue().size() < taskCount) {
                threadId = lists.getKey();
            }
        }
        tasks.get(threadId).add(task);
    }
}
