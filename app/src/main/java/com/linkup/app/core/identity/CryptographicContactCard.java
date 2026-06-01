package com.linkup.app.core.identity;

import android.util.Base64;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

/**
 * Implements Cryptographic Contact Cards (Feature 3).
 * A digitally signed identity card to confirm authenticity.
 */
public class CryptographicContactCard {
    private final String alias;
    private final PublicKey publicKey;
    private String signature;

    public CryptographicContactCard(String alias, PublicKey publicKey) {
        this.alias = alias;
        this.publicKey = publicKey;
    }

    /**
     * Signs the card using the user's private key.
     */
    public void signCard(PrivateKey privateKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(alias.getBytes());
        sig.update(publicKey.getEncoded());
        this.signature = Base64.encodeToString(sig.sign(), Base64.DEFAULT);
    }

    /**
     * Verifies the authenticity of a received contact card.
     */
    public boolean verifyCard(String signatureStr) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(alias.getBytes());
        sig.update(publicKey.getEncoded());
        byte[] sigBytes = Base64.decode(signatureStr, Base64.DEFAULT);
        return sig.verify(sigBytes);
    }

    public String getAlias() { return alias; }
    public PublicKey getPublicKey() { return publicKey; }
    public String getSignature() { return signature; }
}
