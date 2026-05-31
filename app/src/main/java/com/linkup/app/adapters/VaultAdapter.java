package com.linkup.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.models.VaultFileModel;
import java.util.List;

public class VaultAdapter extends RecyclerView.Adapter<VaultAdapter.ViewHolder> {

    private List<VaultFileModel> fileList;

    public VaultAdapter(List<VaultFileModel> fileList) {
        this.fileList = fileList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vault_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VaultFileModel file = fileList.get(position);
        holder.tvFileName.setText(file.getFileName());
        holder.tvFileInfo.setText(file.getFileSize() + " • " + file.getDateAdded());
        holder.ivFileIcon.setImageResource(file.getIconRes());

        holder.btnFileOptions.setOnClickListener(v -> {
            showOptionsDialog(v, position);
        });

        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Decrypting " + file.getFileName() + "...", Toast.LENGTH_SHORT).show();
        });
    }

    private void showOptionsDialog(View v, int position) {
        String[] options = {"🔓 Decrypt & Open", "📤 Export", "🗑️ Delete"};
        new AlertDialog.Builder(v.getContext())
                .setTitle("File Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Toast.makeText(v.getContext(), "File decrypted successfully", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            Toast.makeText(v.getContext(), "Exporting file...", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            fileList.remove(position);
                            notifyItemRemoved(position);
                            Toast.makeText(v.getContext(), "File deleted from vault", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFileIcon;
        TextView tvFileName, tvFileInfo;
        ImageButton btnFileOptions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileInfo = itemView.findViewById(R.id.tvFileInfo);
            btnFileOptions = itemView.findViewById(R.id.btnFileOptions);
        }
    }
}
