package com.linkup.app.activities;

import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.linkup.app.R;
import java.util.Locale;

public class CallActivity extends BaseActivity {

    private boolean isMicOn = true;
    private boolean isCameraOn = false;
    private int seconds = 0;
    private boolean isCallActive = false;
    private Handler timerHandler = new Handler();

    private TextView tvCallStatus, tvCallDuration;
    private SurfaceView svVideoSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        String userName = getIntent().getStringExtra("user_name");
        TextView tvCallerName = findViewById(R.id.tvCallerName);
        if (tvCallerName != null) tvCallerName.setText(userName != null ? userName : "User Name");

        tvCallStatus = findViewById(R.id.tvCallStatus);
        tvCallDuration = findViewById(R.id.tvCallDuration);
        svVideoSurface = findViewById(R.id.svVideoSurface);

        FloatingActionButton btnEndCall = findViewById(R.id.btnEndCall);
        FloatingActionButton btnToggleMic = findViewById(R.id.btnToggleMic);
        FloatingActionButton btnToggleCamera = findViewById(R.id.btnToggleCamera);

        btnEndCall.setOnClickListener(v -> finish());

        btnToggleMic.setOnClickListener(v -> {
            isMicOn = !isMicOn;
            btnToggleMic.setImageResource(isMicOn ? 
                android.R.drawable.ic_btn_speak_now : 
                android.R.drawable.button_onoff_indicator_off);
            Toast.makeText(this, isMicOn ? "Microphone On" : "Microphone Off", Toast.LENGTH_SHORT).show();
        });

        btnToggleCamera.setOnClickListener(v -> {
            isCameraOn = !isCameraOn;
            svVideoSurface.setVisibility(isCameraOn ? View.VISIBLE : View.GONE);
            btnToggleCamera.setImageResource(isCameraOn ? 
                android.R.drawable.presence_video_online : 
                android.R.drawable.ic_menu_camera);
            Toast.makeText(this, isCameraOn ? "Camera On" : "Camera Off", Toast.LENGTH_SHORT).show();
        });

        // Simulate call connection after 2 seconds
        timerHandler.postDelayed(() -> {
            isCallActive = true;
            tvCallStatus.setText("Connected");
            tvCallDuration.setVisibility(View.VISIBLE);
            startTimer();
        }, 2000);
    }

    private void startTimer() {
        timerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isCallActive) {
                    int mins = seconds / 60;
                    int secs = seconds % 60;
                    tvCallDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
                    seconds++;
                    timerHandler.postDelayed(this, 1000);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isCallActive = false;
        timerHandler.removeCallbacksAndMessages(null);
    }
}
