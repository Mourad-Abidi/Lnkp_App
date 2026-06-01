package com.linkup.app.security;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.cert.Certificate;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SecurityUtils {

    private static final String AES_KEY = "LnkpSecureKey123456789012345678"; // 32 chars for AES-256
    private static final String IV = "LnkpStaticIV1234"; // 16 chars for AES block size
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALIAS = "LnkpIdentityKey";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    /**
     * Generates a permanent RSA KeyPair in the Android Keystore for user identity.
     */
    public static void generateIdentityKeyPair() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE);
                kpg.initialize(new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PSS)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                        .build());
                kpg.generateKeyPair();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the Base64 encoded Public Key of this device.
     */
    public static String getPublicKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            Certificate cert = keyStore.getCertificate(KEY_ALIAS);
            if (cert == null) return null;
            return Base64.encodeToString(cert.getPublicKey().getEncoded(), Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Signs data using the Private Key stored in Keystore.
     */
    public static String signData(String data) {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            Signature s = Signature.getInstance("SHA256withRSA/PSS");
            s.initSign(privateKeyEntry.getPrivateKey());
            s.update(data.getBytes());
            byte[] signature = s.sign();
            return Base64.encodeToString(signature, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Hashes a password using SHA-256.
     */
    public static String hashPassword(String password) {
        if (password == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Encrypts a string using AES/CBC/PKCS5Padding.
     */
    public static String encrypt(String input) {
        if (input == null || input.isEmpty()) return input;
        try {
            byte[] keyBytes = new byte[16];
            byte[] b = AES_KEY.getBytes("UTF-8");
            int len = b.length;
            if (len > keyBytes.length) len = keyBytes.length;
            System.arraycopy(b, 0, keyBytes, 0, len);
            
            SecretKeySpec skeySpec = new SecretKeySpec(keyBytes, "AES");
            javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(IV.getBytes("UTF-8"));
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            
            byte[] encrypted = cipher.doFinal(input.getBytes("UTF-8"));
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return input;
        }
    }

    /**
     * Decrypts a string using AES/CBC/PKCS5Padding.
     */
    public static String decrypt(String input) {
        if (input == null || input.isEmpty()) return input;
        try {
            byte[] keyBytes = new byte[16];
            byte[] b = AES_KEY.getBytes("UTF-8");
            int len = b.length;
            if (len > keyBytes.length) len = keyBytes.length;
            System.arraycopy(b, 0, keyBytes, 0, len);
            
            SecretKeySpec skeySpec = new SecretKeySpec(keyBytes, "AES");
            javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(IV.getBytes("UTF-8"));
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            
            byte[] decodedValue = Base64.decode(input, Base64.NO_WRAP);
            byte[] decrypted = cipher.doFinal(decodedValue);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            // If decryption fails, it might be an unencrypted message
            return input;
        }
    }

    /**
     * Checks if the device is rooted.
     */
    public static boolean isDeviceRooted() {
        String[] paths = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    /**
     * Checks if a VPN or Proxy is active.
     */
    public static boolean isVpnActive(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = cm.getActiveNetwork();
            NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
        } else {
            return cm.getNetworkInfo(ConnectivityManager.TYPE_VPN).isConnected();
        }
    }
}
