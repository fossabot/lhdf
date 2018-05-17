package de.lheinrich.lhdf.webserver;

import javax.net.ssl.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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
import java.util.regex.Matcher;
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
            public String process(Map<String, String> get, Map<String, String> head, Map<String, String> post_put, Map<String, String> cookies, String clientIp) {
                return "not found";
            }
        });
        executor = Executors.newFixedThreadPool(threads);

        boolean plain = bindPort != 0;
        boolean ssl = bindPortSSL != 0;

        if (plain) {
            serverSocket = new ServerSocket(bindPort, maxConnections);
            new Thread(() -> {
                try {
                    while (serverSocket != null && !serverSocket.isClosed()) {
                        Socket socket = serverSocket.accept();
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
                        SSLSocket socket = (SSLSocket) sslServerSocket.accept();
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
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); DataInputStream in = new DataInputStream(socket.getInputStream())) {
            byte[] raw = new byte[in.available()];
            in.read(raw);

            out.write("HTTP/1.1 200 OK\r\n");

            List<String> request = new ArrayList<>();
            request.addAll(Arrays.asList(new String(raw).split(System.lineSeparator())));
            Object[] getData = parseGetData(request.get(0));

            String requestType = (String) getData[0];
            String handlerName = ((String) getData[1]).toLowerCase();

            Map<String, String> getRequest = (Map<String, String>) getData[2];
            WebserverHandler handler;

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

            Map<String, String> headData = parseHeadData(request);
            Map<String, String> cookies = new TreeMap<>();
            String cookieName = headData.containsKey("cookie") ? "cookie" : "Cookie";
            if (headData.containsKey(cookieName)) {
                String[] rawCookies = headData.get(cookieName).split(";", 2);
                for (String rawCookie : rawCookies) {
                    String[] cookie = rawCookie.split("=", 2);
                    cookies.put(cookie[0].trim(), cookie[1]);
                }
                headData.remove(cookieName);
            }

            String response = handler.process(getRequest, headData, post_put, cookies, socket.getInetAddress().getHostAddress());

            out.write("Server: Unetkit (Java)\r\n");
            out.write("Access-Control-Allow-Origin: *\r\n");
            out.write("Content-Type: " + handler.getContentType() + "; charset=utf-8\r\n");
            out.write("Content-Length: " + response.length() + "\r\n");
            out.write(generateCookies(handler.getCookies()) + "\r\n");
            out.write(response);
            out.flush();
            in.close();
            out.close();
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
        handlerName = handlerName.toLowerCase();
        unregisterHandler(handlerName);
        handlers.put(handlerName, handler);
    }

    /**
     * Unregister a DynHandler
     *
     * @param handlerName Name of handler (not "not found" !!!)
     */
    public void unregisterHandler(String handlerName) {
        handlerName = handlerName.toLowerCase();
        handlers.remove(handlerName);
    }

    private String generateCookies(Map<String, String> cookies) {
        StringBuilder builder = new StringBuilder();
        cookies.forEach((name, value) -> builder.append("Set-Cookie: ").append(name).append("=").append(value).append("\r\n"));
        return builder.toString();
    }

    private SSLServerSocket createSSLServerSocket(int bindPort, int maxConnections, KeyStore keyStore) throws KeyManagementException, NoSuchAlgorithmException, UnrecoverableKeyException, IOException, KeyStoreException {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, "password".toCharArray());
        KeyManager[] km = keyManagerFactory.getKeyManagers();
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStore);
        TrustManager[] tm = trustManagerFactory.getTrustManagers();
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(km, tm, new SecureRandom());
        SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
        SSLServerSocket customServerSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(bindPort, maxConnections);
        customServerSocket.setEnabledProtocols(new String[]{"TLSv1.2"});
        customServerSocket.setEnabledCipherSuites(new String[]{"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"});
        return customServerSocket;
    }

    private Object[] parseGetData(String raw) {
        try {
            String[] splittedRequest = raw.split(" ");
            String[] preRawRequest = splittedRequest[1].split("\\?", 2);
            String handlerName = preRawRequest[0].substring(1);

            String rawRequest = preRawRequest.length > 1 ? preRawRequest[1] : "";
            String[] splittedRawRequest = rawRequest.split("&");

            Map<String, String> requestMap = new TreeMap<>();

            for (String rawSplittedRawRequest : splittedRawRequest) {
                String[] splittedSplittedRawRequest = rawSplittedRawRequest.split("=", 2);

                if (splittedSplittedRawRequest.length >= 2) {
                    requestMap.put(splittedSplittedRawRequest[0], splittedSplittedRawRequest[1]);
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
            Map<String, String> requestMap = new TreeMap<>();

            raw.stream().map(line -> line.split(":", 2)).filter(splittedLine -> splittedLine.length >= 2).forEach(splittedLine -> requestMap.put(splittedLine[0], splittedLine[1].trim()));

            return requestMap;
        } catch (ArrayIndexOutOfBoundsException ex) {
            return new TreeMap<>();
        }
    }

    private Map<String, String> parsePostData(String raw) {
        try {
            String[] splittedRawRequest = raw.split("&");

            Map<String, String> requestMap = new TreeMap<>();

            for (String rawSplittedRawRequest : splittedRawRequest) {
                String[] splittedSplittedRawRequest = rawSplittedRawRequest.split("=", 2);

                if (splittedSplittedRawRequest.length >= 2) {
                    requestMap.put(splittedSplittedRawRequest[0], splittedSplittedRawRequest[1]);
                }
            }

            return requestMap;
        } catch (ArrayIndexOutOfBoundsException ex) {
            return new TreeMap<>();
        }
    }

    private Map<String, String> parsePutData(List<String> raw) {
        Map<String, String> requestMap = new TreeMap<>();

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
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);

        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        FileInputStream is = new FileInputStream(certificate);
        PrivateKey privateKey = null;

        Pattern pattern = Pattern.compile("-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+([a-z0-9+/=\\r\\n]+)-+END\\s+.*PRIVATE\\s+KEY[^-]*-+", CASE_INSENSITIVE);
        String keyFile = new String(Files.readAllBytes(key.toPath()), StandardCharsets.UTF_8);
        Matcher matcher = pattern.matcher(keyFile);
        matcher.find();
        String matched = matcher.group(1);
        byte[] decodedKey = Base64.getMimeDecoder().decode(matched);

        try {
            PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(decodedKey);

            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                privateKey = keyFactory.generatePrivate(encodedKeySpec);
            } catch (InvalidKeySpecException ignore) {
                KeyFactory keyFactory = KeyFactory.getInstance("EC");
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
