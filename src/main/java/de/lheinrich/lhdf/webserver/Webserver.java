package de.lheinrich.lhdf.webserver;

import javax.net.ssl.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

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

public class Webserver {

    private final static Base64.Encoder ENCODER = Base64.getEncoder();
    private final ExecutorService executor;
    private final Map<String, WebserverHandler> handlers = new TreeMap<>();
    private ServerSocket serverSocket;
    private SSLServerSocket sslServerSocket;

    /**
     * Starts and initialises the web server
     *
     * @param bindPort       where to bind for HTTP (disable = 0)
     * @param bindPortSSL    where to bind for HTTPS (disable = 0)
     * @param threads        limit threads to n
     * @param timeout        socket timeout in seconds
     * @param maxConnections limit connections
     * @param keyStore       key store for the ssl certificate
     * @throws Exception when port is used or error SSL error
     */
    public Webserver(int bindPort, int bindPortSSL, int threads, int timeout, int maxConnections, KeyStore keyStore) throws Exception {
        handlers.put("not found", new WebserverHandler() {
            @Override
            public String[] process(String name, Map<String, String> get, Map<String, String> head, Map<String, String> post_put, Map<String, String> cookies, String clientIp) {
                return new String[]{"text/plain", "not found"};
            }
        });
        executor = Executors.newFixedThreadPool(threads);

        var plain = bindPort != 0;
        var ssl = bindPortSSL != 0;

        if (plain) {
            serverSocket = new ServerSocket(bindPort, maxConnections);
            new Thread(() -> {
                try {
                    while (serverSocket != null && !serverSocket.isClosed()) {
                        var socket = serverSocket.accept();
                        executor.submit(() -> {
                            try {
                                socket.setSoTimeout(timeout * 1000);
                                handleRequest(socket);
                            } catch (SocketException ex) {
                            }
                        });
                    }
                } catch (IOException ex) {
                }
            }).start();
        }

        if (ssl) {
            sslServerSocket = createSSLServerSocket(bindPortSSL, maxConnections, keyStore);
            new Thread(() -> {
                try {
                    while (sslServerSocket != null && !sslServerSocket.isClosed()) {
                        var socket = (SSLSocket) sslServerSocket.accept();
                        executor.submit(() -> {
                            try {
                                socket.setSoTimeout(timeout * 1000);
                                socket.startHandshake();
                                handleRequest(socket);
                            } catch (Exception ex) {
                            }
                        });
                    }
                } catch (IOException ex) {
                }
            }).start();
        }
    }

    private void handleRequest(Socket socket) {
        try (var out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); var in = new DataInputStream(socket.getInputStream())) {
            var raw = new byte[in.available()];
            in.read(raw);

            out.write("HTTP/1.1 200 OK\r\n");

            var request = new ArrayList<String>();
            request.addAll(Arrays.asList(new String(raw).split(System.lineSeparator())));
            var getData = parseGetData(request.get(0));

            var requestType = (String) getData[0];
            var handlerName = ((String) getData[1]).toLowerCase();

            var getRequest = (Map<String, String>) getData[2];
            WebserverHandler handler;

            if (handlerName.startsWith("/"))
                handlerName = handlerName.substring(2);

            if (handlers.containsKey(handlerName)) {
                handler = handlers.get(handlerName);
            } else {
                handler = handlers.get("not found");
            }

            Map<String, String> post_put;
            if (requestType.equalsIgnoreCase("put")) {
                post_put = parsePutData(request);
            } else {
                post_put = parsePostData(request.get(request.size() - 1));
            }

            var headData = parseHeadData(request);
            var cookies = new TreeMap<String, String>();
            var cookieName = headData.containsKey("cookie") ? "cookie" : "Cookie";
            if (headData.containsKey(cookieName)) {
                var rawCookies = headData.get(cookieName).split(";", 2);
                for (var rawCookie : rawCookies) {
                    var cookie = rawCookie.split("=", 2);
                    cookies.put(cookie[0].trim(), cookie[1]);
                }
                headData.remove(cookieName);
            }

            var response = handler.process(handlerName, getRequest, headData, post_put, cookies, socket.getInetAddress().getHostAddress());

            out.write("Server: lhdf (Java)\r\n");
            out.write("Access-Control-Allow-Origin: *\r\n");
            out.write("Content-Type: " + response[0] + "; charset=utf-8\r\n");
            out.write("Content-Length: " + response[1].length() + "\r\n");
            out.write(generateCookies(handler.getCookies()) + "\r\n");
            out.write(response[1]);
            out.flush();
        } catch (IOException ex) {
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Closes the socket
     */
    public void close() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
            }
            serverSocket = null;
        }
    }

    /**
     * Register a new DynHandler
     *
     * @param handlerName for example use HANDLERNAME for
     *                    http://localhost/HANDLERNAME
     * @param handler     WebserverHandler instance
     */
    public void registerHandler(String handlerName, WebserverHandler handler) {
        var handlerNameLower = handlerName.toLowerCase();
        unregisterHandler(handlerNameLower);
        handlers.put(handlerNameLower, handler);
    }

    /**
     * Unregister a DynHandler
     *
     * @param handlerName Name of handler (not "not found" !!!)
     */
    public void unregisterHandler(String handlerName) {
        var handlerNameLower = handlerName.toLowerCase();
        handlers.remove(handlerNameLower);
    }

    private String generateCookies(Map<String, String> cookies) {
        var builder = new StringBuilder();
        cookies.forEach((name, value) -> builder.append("Set-Cookie: ").append(name).append("=").append(value).append("\r\n"));
        return builder.toString();
    }

    private SSLServerSocket createSSLServerSocket(int bindPort, int maxConnections, KeyStore keyStore) throws KeyManagementException, NoSuchAlgorithmException, UnrecoverableKeyException, IOException, KeyStoreException {
        var keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, "password".toCharArray());
        var km = keyManagerFactory.getKeyManagers();
        var trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStore);
        var tm = trustManagerFactory.getTrustManagers();
        var sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(km, tm, new SecureRandom());
        var serverSocketFactory = sslContext.getServerSocketFactory();
        var customServerSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(bindPort, maxConnections);
        customServerSocket.setEnabledProtocols(new String[]{"TLSv1.2"});
        customServerSocket.setEnabledCipherSuites(new String[]{"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"});
        return customServerSocket;
    }

    private Object[] parseGetData(String raw) {
        try {
            var splittedRequest = raw.split(" ");
            var preRawRequest = splittedRequest[1].split("\\?", 2);
            var handlerName = preRawRequest[0].substring(1);

            var rawRequest = preRawRequest.length > 1 ? preRawRequest[1] : "";
            var splittedRawRequest = rawRequest.split("&");

            var requestMap = new TreeMap<>();

            for (var rawSplittedRawRequest : splittedRawRequest) {
                var splittedSplittedRawRequest = rawSplittedRawRequest.split("=", 2);

                if (splittedSplittedRawRequest.length >= 2) {
                    requestMap.put(splittedSplittedRawRequest[0], URLDecoder.decode(splittedSplittedRawRequest[1], StandardCharsets.UTF_8));
                } else {
                    requestMap.put(splittedSplittedRawRequest[0], "");
                }
            }

            return new Object[]{splittedRequest[0], handlerName, requestMap};
        } catch (ArrayIndexOutOfBoundsException ex) {
            return new Object[]{"GET", "/", new TreeMap<>()};
        }
    }

    private Map<String, String> parseHeadData(List<String> raw) {
        try {
            var requestMap = new TreeMap<String, String>();

            raw.stream().map(line -> line.split(":", 2)).filter(splittedLine -> splittedLine.length >= 2).forEach(splittedLine -> requestMap.put(splittedLine[0], URLDecoder.decode(splittedLine[1].trim(), StandardCharsets.UTF_8)));

            return requestMap;
        } catch (ArrayIndexOutOfBoundsException ex) {
            return new TreeMap<>();
        }
    }

    private Map<String, String> parsePostData(String raw) {
        try {
            var splittedRawRequest = raw.split("&");

            var requestMap = new TreeMap<String, String>();

            for (var rawSplittedRawRequest : splittedRawRequest) {
                var splittedSplittedRawRequest = rawSplittedRawRequest.split("=", 2);

                if (splittedSplittedRawRequest.length >= 2) {
                    requestMap.put(splittedSplittedRawRequest[0], URLDecoder.decode(splittedSplittedRawRequest[1], StandardCharsets.UTF_8));
                }
            }

            return requestMap;
        } catch (ArrayIndexOutOfBoundsException ex) {
            return new TreeMap<>();
        }
    }

    private Map<String, String> parsePutData(List<String> raw) {
        var requestMap = new TreeMap<String, String>();

        boolean read = false;
        int i = 0;

        for (String line : raw) {
            if (read) {
                requestMap.put(String.valueOf(i++), line);
                continue;
            }

            if (ENCODER.encodeToString(line.getBytes()).equals("DQ==")) {
                read = true;
            }
        }

        return requestMap;
    }

    public static KeyStore generateKeyStore(File certificate, File key) throws Exception {
        var keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);

        var fact = CertificateFactory.getInstance("X.509");
        var is = new FileInputStream(certificate);
        PrivateKey privateKey = null;

        var pattern = Pattern.compile("-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+([a-z0-9+/=\\r\\n]+)-+END\\s+.*PRIVATE\\s+KEY[^-]*-+", CASE_INSENSITIVE);
        var keyFile = new String(Files.readAllBytes(key.toPath()), StandardCharsets.UTF_8);
        var matcher = pattern.matcher(keyFile);
        matcher.find();
        var matched = matcher.group(1);
        var decodedKey = Base64.getMimeDecoder().decode(matched);

        try {
            var encodedKeySpec = new PKCS8EncodedKeySpec(decodedKey);

            try {
                var keyFactory = KeyFactory.getInstance("RSA");
                privateKey = keyFactory.generatePrivate(encodedKeySpec);
            } catch (InvalidKeySpecException ignore) {
                var keyFactory = KeyFactory.getInstance("EC");
                privateKey = keyFactory.generatePrivate(encodedKeySpec);
            }
        } catch (Exception ex) {
            if (ex.getMessage().contains("algid parse error, not a sequence")) {
                throw new InvalidKeyException("convert your private key: openssl pkcs8 -topk8 -inform PEM -outform DER -in PRIVATE_KEY_FILE -out tempcertificate.temp -nocrypt && openssl pkcs8 -topk8 -inform DER -outform PEM -in tempcertificate.temp -out PRIVATE_KEY_FILE -nocrypt && rm tempcertificate.temp");
            }
        }

        keyStore.setKeyEntry("mykey", privateKey, "password".toCharArray(), new Certificate[]{fact.generateCertificate(is)});
        return keyStore;
    }
}
