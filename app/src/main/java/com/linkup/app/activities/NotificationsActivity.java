package com.linkup.app.activities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.adapters.NotificationAdapter;
import com.linkup.app.models.NotificationModel;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends BaseActivity {

    private NotificationAdapter adapter;
    private TextView tvReadAll, tvDeleteAll, tvEmptyState;
    private ImageButton btnFilterUnread;
    private boolean isFiltered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        View toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setOnClickListener(v -> finish());
        }

        tvReadAll = findViewById(R.id.tvReadAll);
        tvDeleteAll = findViewById(R.id.tvDeleteAll);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnFilterUnread = findViewById(R.id.btnFilterUnread);

        if (tvReadAll != null) tvReadAll.setOnClickListener(v -> {
            if (adapter != null) adapter.markAllAsRead();
        });
        
        if (tvDeleteAll != null) tvDeleteAll.setOnClickListener(v -> {
            if (adapter != null) adapter.deleteAll();
        });
        
        if (btnFilterUnread != null) {
            btnFilterUnread.setOnClickListener(v -> {
                isFiltered = !isFiltered;
                if (adapter != null) {
                    adapter.toggleUnreadFilter();
                    btnFilterUnread.setColorFilter(isFiltered ? 
                        ContextCompat.getColor(this, R.color.secondary) : 
                        Color.WHITE);
                }
                
                Toast.makeText(this, isFiltered ? "Showing unread only" : "Showing all", Toast.LENGTH_SHORT).show();
            });
        }

        RecyclerView rvNotifications = findViewById(R.id.rvNotifications);
        if (rvNotifications != null) {
            rvNotifications.setLayoutManager(new LinearLayoutManager(this));

            List<NotificationModel> notifications = new ArrayList<>();
            notifications.add(new NotificationModel("Security Alert", "A new login was detected from a Linux device.", "10m ago", android.R.drawable.ic_lock_idle_lock, R.drawable.app_logo, false, SecurityCenterActivity.class));
            notifications.add(new NotificationModel("Anonymous User", "This message will self-destruct in 30 seconds.", "1h ago", android.R.drawable.stat_notify_chat, R.drawable.app_logo, false, FindPeopleActivity.class));
            notifications.add(new NotificationModel("System Update", "Ephemeral Identity system updated to v2.4.", "3h ago", android.R.drawable.ic_dialog_info, R.drawable.app_logo, true, SettingsActivity.class));
            notifications.add(new NotificationModel("Michael Jordan", "Shared a new encrypted memory with you.", "5h ago", android.R.drawable.ic_menu_myplaces, R.drawable.app_logo, true, MainActivity.class));
            notifications.add(new NotificationModel("Vault Security", "Your secure vault was successfully backed up.", "Yesterday", android.R.drawable.ic_menu_save, R.drawable.app_logo, true, VaultActivity.class));

            adapter = new NotificationAdapter(notifications);
            adapter.setOnDataChangedListener(this::updateUIState);
            rvNotifications.setAdapter(adapter);

            setupSwipeActions(rvNotifications);
        }
        updateUIState();
    }

    private void updateUIState() {
        if (adapter == null) return;
        int count = adapter.getItemCount();
        if (tvEmptyState != null) tvEmptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);

        if (count == 0) {
            if (tvReadAll != null) tvReadAll.setVisibility(View.GONE);
            if (tvDeleteAll != null) tvDeleteAll.setVisibility(View.GONE);
        } else if (adapter.hasUnread()) {
            if (tvReadAll != null) tvReadAll.setVisibility(View.VISIBLE);
            if (tvDeleteAll != null) tvDeleteAll.setVisibility(View.GONE);
        } else {
            if (tvReadAll != null) tvReadAll.setVisibility(View.GONE);
            if (tvDeleteAll != null) tvDeleteAll.setVisibility(View.VISIBLE);
        }
    }

    private void setupSwipeActions(RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                NotificationModel notification = adapter.getNotificationList().get(position);

                if (direction == ItemTouchHelper.LEFT) {
                    adapter.markAsRead(position);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    if (notification.isRead()) {
                        adapter.deleteNotification(position);
                    } else {
                        Toast.makeText(NotificationsActivity.this, "Mark as seen first to delete", Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                    }
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                final View itemView = viewHolder.itemView;
                final ColorDrawable background = new ColorDrawable();
                final Paint paint = new Paint();
                paint.setColor(Color.WHITE);
                paint.setTextSize(40);
                paint.setAntiAlias(true);
                Rect textBounds = new Rect();

                if (dX > 0) { // Swiping Right -> Delete
                    background.setColor(Color.RED);
                    background.setBounds(itemView.getLeft(), itemView.getTop(), (int) (itemView.getLeft() + dX), itemView.getBottom());
                    background.draw(c);

                    String text = "Delete";
                    paint.getTextBounds(text, 0, text.length(), textBounds);
                    float x = itemView.getLeft() + 48;
                    float y = itemView.getTop() + (itemView.getHeight() + textBounds.height()) / 2f;
                    if (dX > 100) c.drawText(text, x, y, paint);

                } else if (dX < 0) { // Swiping Left -> Mark as seen
                    background.setColor(ContextCompat.getColor(NotificationsActivity.this, R.color.primary));
                    background.setBounds((int) (itemView.getRight() + dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    background.draw(c);

                    String line1 = "Mark as";
                    String line2 = "seen";
                    
                    paint.getTextBounds(line1, 0, line1.length(), textBounds);
                    int line1Width = textBounds.width();
                    int line1Height = textBounds.height();
                    
                    paint.getTextBounds(line2, 0, line2.length(), textBounds);
                    int line2Width = textBounds.width();
                    int line2Height = textBounds.height();
                    
                    float spacing = 8f; 
                    float totalHeight = line1Height + line2Height + spacing;
                    float startY = itemView.getTop() + (itemView.getHeight() - totalHeight) / 2f + line1Height;
                    
                    if (dX < -100) {
                        float xLine1 = itemView.getRight() - line1Width - 48;
                        float xLine2 = itemView.getRight() - line2Width - 48;
                        c.drawText(line1, xLine1, startY, paint);
                        c.drawText(line2, xLine2, startY + line2Height + spacing, paint);
                    }
                }
                
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }
}
