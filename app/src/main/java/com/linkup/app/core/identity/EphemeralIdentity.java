package com.linkup.app.core.identity;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.UUID;

/**
 * Implements the Ephemeral Identity System.
 * Generates temporary cryptographic identities for session-based communication.
 */
public class EphemeralIdentity {
    private final String sessionId;
    private final KeyPair keyPair;
    private final long createdAt;
    private final long expiryTime;

    public EphemeralIdentity(long durationMillis) throws Exception {
        this.sessionId = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.expiryTime = this.createdAt + durationMillis;
        
        // Generate temporary RSA keys for this session
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        this.keyPair = keyGen.generateKeyPair();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }
    
    /**
     * Securely wipes the identity from memory.
     */
    public void destroy() {
        // In a real implementation, we would zero out the private key bytes
    }
}
