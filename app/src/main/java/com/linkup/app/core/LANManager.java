package com.linkup.app.core;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LANManager {
    private static final String TAG = "LANManager";
    private static final String SERVICE_TYPE = "_linkup._tcp.";
    private static final String SERVICE_NAME_PREFIX = "LinkUp_";
    
    private static LANManager instance;
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.RegistrationListener registrationListener;
    
    private String mServiceName;
    private int mLocalPort;
    private Set<OnPeerDiscoveryListener> listeners = new HashSet<>();
    private Map<String, String> activePeers = new HashMap<>();
    private boolean isDiscoveryStarted = false;

    public interface OnPeerDiscoveryListener {
        void onPeerFound(String name, String ip);
        void onPeerLost(String name);
    }

    private LANManager(Context context) {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mServiceName = SERVICE_NAME_PREFIX + android.os.Build.MODEL;
    }

    public static synchronized LANManager getInstance(Context context) {
        if (instance == null) {
            instance = new LANManager(context.getApplicationContext());
        }
        return instance;
    }

    public void start(OnPeerDiscoveryListener listener) {
        if (listener != null) listeners.add(listener);
        if (!isDiscoveryStarted) {
            registerService();
            startDiscovery();
            isDiscoveryStarted = true;
        }
    }

    public void stop(OnPeerDiscoveryListener listener) {
        if (listener != null) listeners.remove(listener);
        if (listeners.isEmpty() && isDiscoveryStarted) {
            stopDiscovery();
            unregisterService();
            isDiscoveryStarted = false;
        }
    }

    public String getIpForUser(String name) {
        return activePeers.get(name);
    }

    private void registerService() {
        try {
            ServerSocket socket = new ServerSocket(0);
            mLocalPort = socket.getLocalPort();
            socket.close();
        } catch (IOException e) {
            mLocalPort = 8888;
        }

        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(mLocalPort);

        registrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                mServiceName = nsdServiceInfo.getServiceName();
                Log.d(TAG, "Service registered: " + mServiceName);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {}

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {}

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {}
        };

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    private void startDiscovery() {
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {}

            @Override
            public void onDiscoveryStopped(String serviceType) {}

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                if (serviceInfo.getServiceName().equals(mServiceName)) return;
                
                if (serviceInfo.getServiceName().startsWith(SERVICE_NAME_PREFIX)) {
                    nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {}

                        @Override
                        public void onServiceResolved(NsdServiceInfo resolvedServiceInfo) {
                            String name = resolvedServiceInfo.getServiceName().replace(SERVICE_NAME_PREFIX, "");
                            String ip = resolvedServiceInfo.getHost().getHostAddress();
                            activePeers.put(name, ip);
                            for (OnPeerDiscoveryListener l : listeners) {
                                l.onPeerFound(name, ip);
                            }
                        }
                    });
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                String name = serviceInfo.getServiceName().replace(SERVICE_NAME_PREFIX, "");
                activePeers.remove(name);
                for (OnPeerDiscoveryListener l : listeners) {
                    l.onPeerLost(name);
                }
            }
        };

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    private void stopDiscovery() {
        if (nsdManager != null && discoveryListener != null) {
            try { nsdManager.stopServiceDiscovery(discoveryListener); } catch (Exception ignored) {}
        }
    }

    private void unregisterService() {
        if (nsdManager != null && registrationListener != null) {
            try { nsdManager.unregisterService(registrationListener); } catch (Exception ignored) {}
        }
    }
}
