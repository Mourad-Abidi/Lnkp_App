package com.linkup.app.activities;

import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.adapters.ChatAdapter;
import com.linkup.app.models.ChatModel;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class LANChatActivity extends BaseActivity {

    private static final String TAG = "LANChatActivity";
    private RecyclerView rvLocalPeers;
    private ChatAdapter adapter;
    private List<ChatModel> localPeersList = new ArrayList<>();
    private View llNoPeers, discoveryProgress;
    private TextView tvDiscoveryStatus;
    
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.RegistrationListener registrationListener;
    private String SERVICE_TYPE = "_linkup._tcp.";
    private String mServiceName = "LinkUp_" + android.os.Build.MODEL;
    private int mLocalPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lan_chat);

        initViews();
        setupNSD();
        registerService();
        startDiscovery();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        }

        rvLocalPeers = findViewById(R.id.rvLocalPeers);
        llNoPeers = findViewById(R.id.llNoPeers);
        discoveryProgress = findViewById(R.id.discoveryProgress);
        tvDiscoveryStatus = findViewById(R.id.tvDiscoveryStatus);

        rvLocalPeers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(localPeersList);
        rvLocalPeers.setAdapter(adapter);
    }

    private void setupNSD() {
        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        
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
            public void onDiscoveryStarted(String serviceType) {
                updateStatus("Searching for local users...");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {}

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                if (!serviceInfo.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + serviceInfo.getServiceType());
                } else if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (serviceInfo.getServiceName().startsWith("LinkUp_")) {
                    nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            Log.e(TAG, "Resolve failed" + errorCode);
                        }

                        @Override
                        public void onServiceResolved(NsdServiceInfo resolvedServiceInfo) {
                            String host = resolvedServiceInfo.getHost().getHostAddress();
                            addPeerToList(resolvedServiceInfo.getServiceName().replace("LinkUp_", ""), host);
                        }
                    });
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                removePeerFromList(serviceInfo.getServiceName().replace("LinkUp_", ""));
            }
        };
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
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                mServiceName = NsdServiceInfo.getServiceName();
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
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    private void updateStatus(String status) {
        runOnUiThread(() -> {
            if (tvDiscoveryStatus != null) tvDiscoveryStatus.setText(status);
        });
    }

    private synchronized void addPeerToList(String name, String ip) {
        runOnUiThread(() -> {
            boolean exists = false;
            for (ChatModel peer : localPeersList) {
                if (peer.getUserName().equals(name)) {
                    exists = true;
                    peer.setIpAddress(ip);
                    break;
                }
            }
            if (!exists) {
                ChatModel newPeer = new ChatModel(name, "Connected via LAN", "Local", 0, false, true);
                newPeer.setIpAddress(ip);
                localPeersList.add(newPeer);
                adapter.notifyItemInserted(localPeersList.size() - 1);
                updateEmptyState();
            }
        });
    }

    private synchronized void removePeerFromList(String name) {
        runOnUiThread(() -> {
            int index = -1;
            for (int i = 0; i < localPeersList.size(); i++) {
                if (localPeersList.get(i).getUserName().equals(name)) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                localPeersList.remove(index);
                adapter.notifyItemRemoved(index);
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        if (localPeersList.isEmpty()) {
            llNoPeers.setVisibility(View.VISIBLE);
            discoveryProgress.setVisibility(View.VISIBLE);
            updateStatus("Scanning for local peers...");
        } else {
            llNoPeers.setVisibility(View.GONE);
            discoveryProgress.setVisibility(View.GONE);
            updateStatus(localPeersList.size() + " user(s) found nearby");
        }
    }

    @Override
    protected void onPause() {
        if (nsdManager != null) {
            try { nsdManager.stopServiceDiscovery(discoveryListener); } catch (Exception ignored) {}
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (nsdManager != null) {
            try { nsdManager.unregisterService(registrationListener); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }
}
