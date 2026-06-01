package com.linkup.app.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.adapters.MessageAdapter;
import com.linkup.app.models.MessageModel;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LANChatRoomActivity extends BaseActivity implements MessageAdapter.OnMessageLongClickListener {

    private static final String TAG = "LANChatRoomActivity";
    private static final int SERVER_PORT = 8888;

    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<MessageModel> messageList;
    private EditText etMessage;
    private String chatPartnerName;
    private String partnerIp;

    private ServerSocket serverSocket;
    private Thread serverThread;
    private Socket clientSocket;
    private PrintWriter out;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatPartnerName = getIntent().getStringExtra("user_name");
        partnerIp = getIntent().getStringExtra("ip_address");
        
        if (chatPartnerName == null) chatPartnerName = "LAN User";

        TextView tvUserNameToolbar = findViewById(R.id.tvUserNameToolbar);
        if (tvUserNameToolbar != null) tvUserNameToolbar.setText(chatPartnerName + " (LAN)");

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList, this);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSend).setOnClickListener(v -> {
            String content = etMessage.getText().toString().trim();
            if (!content.isEmpty()) {
                sendMessageOverLAN(content);
                etMessage.setText("");
            }
        });

        startServer();
        connectToPartner();
    }

    private void startServer() {
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = serverSocket.accept();
                    new Thread(new ReceiveTask(socket)).start();
                }
            } catch (Exception e) {
                Log.e(TAG, "Server Error", e);
            }
        });
        serverThread.start();
    }

    private void connectToPartner() {
        new Thread(() -> {
            try {
                if (partnerIp != null) {
                    InetAddress serverAddr = InetAddress.getByName(partnerIp);
                    clientSocket = new Socket(serverAddr, SERVER_PORT);
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);
                }
            } catch (Exception e) {
                Log.e(TAG, "Connect Error", e);
            }
        }).start();
    }

    private void sendMessageOverLAN(String content) {
        // Display locally
        displayMessage(content, true);

        // Send to partner
        new Thread(() -> {
            if (out != null) {
                out.println(content);
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Partner not reachable yet", Toast.LENGTH_SHORT).show());
                connectToPartner(); // Retry connection
            }
        }).start();
    }

    private void displayMessage(String content, boolean isSent) {
        runOnUiThread(() -> {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            messageList.add(new MessageModel(content, time, isSent, MessageModel.MessageType.TEXT, null, isSent ? "Me" : chatPartnerName));
            adapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.smoothScrollToPosition(messageList.size() - 1);
        });
    }

    class ReceiveTask implements Runnable {
        private Socket socket;
        private BufferedReader input;

        ReceiveTask(Socket socket) {
            this.socket = socket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            } catch (Exception e) {
                Log.e(TAG, "ReceiveTask Init Error", e);
            }
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    String message = input.readLine();
                    if (message == null) break;
                    displayMessage(message, false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Receive Error", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (serverSocket != null) serverSocket.close();
            if (clientSocket != null) clientSocket.close();
        } catch (Exception ignored) {}
        if (serverThread != null) serverThread.interrupt();
    }

    @Override public void onMessageLongClick(MessageModel message, View itemView) {}
    @Override public void onSelectionChanged() {}
    @Override public void onReplySwiped(MessageModel message) {}
    @Override public void onReplyPreviewClick(MessageModel message) {}
    @Override public void onMediaClick(MessageModel message) {}
}
