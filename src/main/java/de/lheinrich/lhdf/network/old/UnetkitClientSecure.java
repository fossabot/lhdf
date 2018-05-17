package de.lheinrich.lhdf.network.old;

import de.lheinrich.lhdf.security.AESKeySize;
import de.lheinrich.lhdf.security.Crypter;
import de.lheinrich.lhdf.security.ECKeySize;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
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

public class UnetkitClientSecure {

    private static TreeMap<String, SecretKey> keyStore = new TreeMap<>();

    /**
     * Sends a Serializable Object (encrypted)
     *
     * @param host   Server hostname or ip address
     * @param port   Server port
     * @param object Serializable Object to send
     * @return Object responded by server
     */
    public static Object send(String host, int port, Serializable object) {
        Socket socket = null;

        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        Object response = null;

        try {
            socket = new Socket(host, port);
            socket.setKeepAlive(true);

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            String keyStoreName = host + " on " + port;
            SecretKey key = null;
            boolean hasKey = keyStore.containsKey(keyStoreName);
            byte[] keyHash;

            if (hasKey) {
                key = keyStore.get(keyStoreName);
                keyHash = Crypter.hash("SHA3-224", key.getEncoded());
            } else {
                keyHash = "client".getBytes();
            }

            byte[] clientKey = (byte[]) in.readObject();
            boolean correctKey = Arrays.equals(clientKey, keyHash);

            out.writeObject(keyHash);

            if (!correctKey) {
                KeyPair keyPair = Crypter.generateECKeyPair(ECKeySize.LOWEST.getSize());
                out.writeObject(keyPair.getPublic());

                PublicKey publicKey = (PublicKey) in.readObject();
                key = Crypter.generateEC("SHA3-224", AESKeySize.LOW.getSize(), keyPair.getPrivate(), publicKey);

                if (hasKey) {
                    keyStore.remove(keyStoreName);
                }
                keyStore.put(keyStoreName, key);
            }

            byte[] byteObject = Crypter.toByteArray(object);
            byte[] encryptedObject = Crypter.encrypt("AES", byteObject, key);

            out.writeObject(encryptedObject);

            byte[] decryptedResponse = Crypter.decrypt("AES", (byte[]) in.readObject(), key);
            response = Crypter.toObject(decryptedResponse);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }

                if (in != null) {
                    in.close();
                }

                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return response;
    }
}
