package de.lheinrich.lhdf.security;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;

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

public class Crypter {

    public static byte[] encrypt(String algorithm, byte[] value, Key key) {
        try {
            var cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(value);
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException ex) {
            return null;
        }
    }

    public static byte[] decrypt(String algorithm, byte[] value, Key key) {
        try {
            var cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(value);
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException ex) {
            return null;
        }
    }

    public static byte[] toByteArray(Serializable object) {
        try {
            byte[] result;
            try (var bos = new ByteArrayOutputStream(); var out = new ObjectOutputStream(bos)) {
                out.writeObject(object);
                out.flush();
                result = bos.toByteArray();
            }
            return result;
        } catch (IOException ex) {
            return null;
        }
    }

    public static Object toObject(byte[] bytes) {
        try {
            Object object;
            try (var bis = new ByteArrayInputStream(bytes); var in = new ObjectInputStream(bis)) {
                object = in.readObject();
            }
            return object;
        } catch (ClassNotFoundException | IOException ex) {
            return null;
        }
    }

    public static SecretKey generateAESKey(int keySize) {
        try {
            var keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(keySize);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    public static KeyPair generateRSAKeyPair(int keySize) {
        try {
            var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    public static KeyPair generateECKeyPair(int keySize) {
        try {
            var keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyPairGenerator.initialize(keySize);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    public static SecretKey generateEC(String hashAlgorithm, int keySize, PrivateKey privateKey, PublicKey publicKey) {
        try {
            var keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(publicKey, true);
            var hash = hash(hashAlgorithm, keyAgreement.generateSecret());
            var rawKey = Arrays.copyOfRange(hash, hash.length - keySize / 8, hash.length);
            return new SecretKeySpec(rawKey, 0, rawKey.length, "AES");
        } catch (IllegalStateException | InvalidKeyException | NoSuchAlgorithmException ex) {
            return null;
        }
    }

    public static String hash(String algorithm, String value) {
        try {
            var md = MessageDigest.getInstance(algorithm);
            md.update(value.getBytes(StandardCharsets.UTF_8));
            var digest = md.digest();
            return String.format("%064x", new java.math.BigInteger(1, digest));
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    public static byte[] hash(String algorithm, byte[] value) {
        try {
            var md = MessageDigest.getInstance(algorithm);
            md.update(value);
            return md.digest();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }
}
