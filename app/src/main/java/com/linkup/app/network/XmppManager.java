package com.linkup.app.network;

import android.content.Context;

/**
 * Deprecated. XMPP removed in favor of Supabase.
 */
public class XmppManager {
    private static XmppManager instance;

    public static synchronized XmppManager getInstance() {
        if (instance == null) instance = new XmppManager();
        return instance;
    }

    public void init(Context context) {}
    public boolean connect() { return false; }
    public boolean storeDonation(String amount) { return true; }
}
