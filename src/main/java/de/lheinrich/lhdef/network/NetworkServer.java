package de.lheinrich.lhdef.network;

import javax.crypto.SecretKey;

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

public class NetworkServer {

    private final String networkId;
    private final int networkPort;
    private final String id;
    private final String host;
    private final int port;
    private final String authentication;
    private SecretKey key;

    public NetworkServer(String networkId, int networkPort, String id, String host, int port, String authentication, SecretKey key) {
        this.networkId = networkId;
        this.networkPort = networkPort;
        this.id = id;
        this.host = host;
        this.port = port;
        this.authentication = authentication;
        this.key = key;
    }

    public String getId() {
        return this.id;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public SecretKey getKey() {
        return this.key;
    }

    public void connect(String handlerName, NetworkHandler handler) {
        this.key = SecureNetwork.connectTo(this.networkId, this.networkPort, this.host, this.port, this.key != null, this.key, this.authentication, handlerName, handler);
    }
}
