package com.linkup.app.core.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implements Message Fragmentation (Feature 8).
 * Breaks down large encrypted messages into smaller chunks for resilient transmission.
 */
public class MessageFragmenter {
    private static final int MAX_FRAGMENT_SIZE = 1024; // 1KB fragments

    public static class Fragment {
        public String messageId;
        public int index;
        public int total;
        public String payload;

        public Fragment(String messageId, int index, int total, String payload) {
            this.messageId = messageId;
            this.index = index;
            this.total = total;
            this.payload = payload;
        }
    }

    public List<Fragment> fragmentMessage(String encryptedMessage) {
        String messageId = UUID.randomUUID().toString();
        List<Fragment> fragments = new ArrayList<>();
        int length = encryptedMessage.length();
        int total = (int) Math.ceil((double) length / MAX_FRAGMENT_SIZE);

        for (int i = 0; i < total; i++) {
            int start = i * MAX_FRAGMENT_SIZE;
            int end = Math.min(start + MAX_FRAGMENT_SIZE, length);
            String part = encryptedMessage.substring(start, end);
            fragments.add(new Fragment(messageId, i, total, part));
        }
        return fragments;
    }
}
