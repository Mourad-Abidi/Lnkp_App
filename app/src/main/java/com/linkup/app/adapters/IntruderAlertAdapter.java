package com.linkup.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.models.IntruderAlert;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IntruderAlertAdapter extends RecyclerView.Adapter<IntruderAlertAdapter.ViewHolder> {

    private List<IntruderAlert> alerts;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());

    public IntruderAlertAdapter(List<IntruderAlert> alerts) {
        this.alerts = alerts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_intruder_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IntruderAlert alert = alerts.get(position);
        holder.tvDeviceName.setText(alert.getDeviceName());
        holder.tvAlertTime.setText(dateFormat.format(new Date(alert.getTimestamp())));
        // In a real app, you'd use Glide or Picasso to load the image from alert.getImagePath()
        // holder.ivIntruder.setImageURI(Uri.parse(alert.getImagePath()));
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIntruder;
        TextView tvDeviceName, tvAlertTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIntruder = itemView.findViewById(R.id.ivIntruder);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvAlertTime = itemView.findViewById(R.id.tvAlertTime);
        }
    }
}
