package de.lheinrich.lhdef;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;

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
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(bos)) {
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
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInput in = new ObjectInputStream(bis)) {
                object = in.readObject();
            }
            return object;
        } catch (ClassNotFoundException | IOException ex) {
            return null;
        }
    }

    public static SecretKey generateAESKey(AESKeySize keySize) {
        try {
            var keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(keySize.getSize());
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    public static KeyPair generateRSAKeyPair(RSAKeySize keySize) {
        try {
            var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize.getSize());
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    public static KeyPair generateECKeyPair(ECKeySize keySize) {
        try {
            var keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyPairGenerator.initialize(keySize.getSize());
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    public static SecretKey generateEC(String algorithm, String hashAlgorithm, AESKeySize keySize, PrivateKey privateKey, PublicKey publicKey) {
        try {
            var keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(publicKey, true);
            var hash = hash(hashAlgorithm, keyAgreement.generateSecret());
            var rawKey = Arrays.copyOfRange(hash, hash.length - keySize.getSub(), hash.length);
            return new SecretKeySpec(rawKey, 0, rawKey.length, algorithm);
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
