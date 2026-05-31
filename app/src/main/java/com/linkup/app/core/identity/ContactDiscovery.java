package com.linkup.app.core.identity;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements Encrypted Contact Discovery.
 * Converts phone numbers into cryptographic hashes and matches them with system contacts.
 */
public class ContactDiscovery {

    /**
     * Hashes a phone number using SHA-256 to protect privacy during discovery.
     */
    public String hashPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;
        try {
            // Normalize number (remove +, spaces, etc.)
            String normalized = phoneNumber.replaceAll("[^0-9]", "");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Retrieves all phone numbers from the device's contact list.
     */
    public List<String> getDeviceContacts(Context context) {
        List<String> contacts = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Cursor cur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

        if (cur != null) {
            while (cur.moveToNext()) {
                String phone = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                if (phone != null) contacts.add(phone);
            }
            cur.close();
        }
        return contacts;
    }

    /**
     * Discovers which device contacts are registered on the server.
     */
    public List<String> discoverRegisteredUsers(Context context, List<String> serverHashes) {
        List<String> localNumbers = getDeviceContacts(context);
        List<String> registeredNumbers = new ArrayList<>();
        for (String number : localNumbers) {
            String hash = hashPhoneNumber(number);
            if (serverHashes.contains(hash)) {
                registeredNumbers.add(number);
            }
        }
        return registeredNumbers;
    }
}
