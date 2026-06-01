package com.linkup.app.adapters;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.linkup.app.R;
import com.linkup.app.activities.UserProfileActivity;
import com.linkup.app.core.AppExecutors;
import com.linkup.app.models.MessageModel;
import com.linkup.app.utils.GlideUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT_TEXT = 1;
    private static final int TYPE_RECEIVED_TEXT = 2;
    private static final int TYPE_SENT_VOICE = 3;
    private static final int TYPE_RECEIVED_VOICE = 4;
    private static final int TYPE_SENT_IMAGE = 5;
    private static final int TYPE_RECEIVED_IMAGE = 6;
    private static final int TYPE_SENT_FILE = 7;
    private static final int TYPE_RECEIVED_FILE = 8;

    private List<MessageModel> messageList;
    private Handler playbackHandler = new Handler(Looper.getMainLooper());
    private OnMessageLongClickListener longClickListener;
    private boolean isSelectionMode = false;
    private int partnerFocusId = -1;
    private SharedPreferences settings;
    private String partnerAvatarUri;

    public interface OnMessageLongClickListener {
        default void onMessageLongClick(MessageModel message, View itemView) {}
        default void onSelectionChanged() {}
        default void onReplySwiped(MessageModel message) {}
        default void onReplyPreviewClick(MessageModel message) {}
        default void onMediaClick(MessageModel message) {}
    }

    public MessageAdapter(List<MessageModel> messageList, OnMessageLongClickListener listener) {
        this.messageList = messageList;
        this.longClickListener = listener;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        notifyDataSetChanged();
    }

    public void setPartnerFocusId(int partnerFocusId) {
        this.partnerFocusId = partnerFocusId;
        notifyDataSetChanged();
    }

    public void setPartnerAvatarUri(String uri) {
        this.partnerAvatarUri = uri;
        notifyDataSetChanged();
    }

    public void updateList(List<MessageModel> newList) {
        final List<MessageModel> oldList = new ArrayList<>(this.messageList);
        AppExecutors.getInstance().networkIO().execute(() -> {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MessageDiffCallback(oldList, newList));
            AppExecutors.getInstance().mainThread().execute(() -> {
                this.messageList.clear();
                this.messageList.addAll(newList);
                diffResult.dispatchUpdatesTo(this);
            });
        });
    }

    private static class MessageDiffCallback extends DiffUtil.Callback {
        private final List<MessageModel> oldList;
        private final List<MessageModel> newList;

        public MessageDiffCallback(List<MessageModel> oldList, List<MessageModel> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            MessageModel oldItem = oldList.get(oldItemPosition);
            MessageModel newItem = newList.get(newItemPosition);
            if (oldItem.getCloudId() != null && newItem.getCloudId() != null) {
                return Objects.equals(oldItem.getCloudId(), newItem.getCloudId());
            }
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            MessageModel oldItem = oldList.get(oldItemPosition);
            MessageModel newItem = newList.get(newItemPosition);
            return Objects.equals(oldItem.getMessage(), newItem.getMessage()) &&
                   Objects.equals(oldItem.getMediaUrl(), newItem.getMediaUrl()) &&
                   oldItem.isSeen() == newItem.isSeen() &&
                   oldItem.getType() == newItem.getType();
        }
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel message = messageList.get(position);
        if (message.isSent()) {
            if (message.getType() == MessageModel.MessageType.VOICE) return TYPE_SENT_VOICE;
            if (message.getType() == MessageModel.MessageType.IMAGE || message.getType() == MessageModel.MessageType.VIDEO) return TYPE_SENT_IMAGE;
            if (message.getType() == MessageModel.MessageType.FILE) return TYPE_SENT_FILE;
            return TYPE_SENT_TEXT;
        } else {
            if (message.getType() == MessageModel.MessageType.VOICE) return TYPE_RECEIVED_VOICE;
            if (message.getType() == MessageModel.MessageType.IMAGE || message.getType() == MessageModel.MessageType.VIDEO) return TYPE_RECEIVED_IMAGE;
            if (message.getType() == MessageModel.MessageType.FILE) return TYPE_RECEIVED_FILE;
            return TYPE_RECEIVED_TEXT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (settings == null) {
            settings = parent.getContext().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        }
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_SENT_TEXT:
                return new TextViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false));
            case TYPE_RECEIVED_TEXT:
                return new TextViewHolder(inflater.inflate(R.layout.item_message_received, parent, false));
            case TYPE_SENT_VOICE:
                return new VoiceViewHolder(inflater.inflate(R.layout.item_message_sent_voice, parent, false));
            case TYPE_RECEIVED_VOICE:
                return new VoiceViewHolder(inflater.inflate(R.layout.item_message_received_voice, parent, false));
            case TYPE_SENT_IMAGE:
                return new ImageViewHolder(inflater.inflate(R.layout.item_message_sent_image, parent, false));
            case TYPE_RECEIVED_IMAGE:
                return new ImageViewHolder(inflater.inflate(R.layout.item_message_received_image, parent, false));
            case TYPE_SENT_FILE:
                return new FileViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false));
            case TYPE_RECEIVED_FILE:
                return new FileViewHolder(inflater.inflate(R.layout.item_message_received, parent, false));
            default:
                return new TextViewHolder(inflater.inflate(R.layout.item_message_received, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel message = messageList.get(position);
        Context context = holder.itemView.getContext();
        
        boolean isCompact = settings != null && settings.getBoolean("chat_compact_mode", false);
        boolean isMasked = settings != null && settings.getBoolean("privacy_mask_active", false);
        int fontProgress = settings != null ? settings.getInt("font_size_progress", 1) : 1;
        float baseSize = 16f;
        if (fontProgress == 0) baseSize = 13f;
        else if (fontProgress == 2) baseSize = 20f;

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onMessageLongClick(message, v);
                return true;
            }
            return false;
        });

        if (message.getId() == partnerFocusId) {
            holder.itemView.setBackgroundColor(0x2200BCD4);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        if (holder instanceof TextViewHolder) {
            TextViewHolder tvh = (TextViewHolder) holder;
            if (isMasked) {
                tvh.tvMessage.setText("••••••••••••••••");
                tvh.tvMessage.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                tvh.tvMessage.getPaint().setMaskFilter(new BlurMaskFilter(10, BlurMaskFilter.Blur.NORMAL));
            } else {
                tvh.tvMessage.setText(message.getMessage());
                tvh.tvMessage.getPaint().setMaskFilter(null);
            }
            tvh.tvMessage.setTextSize(baseSize);
            if (tvh.tvTime != null) tvh.tvTime.setText(message.getTime());
            bindStatus(tvh.ivStatus, message);
            bindAvatar(tvh.ivUserAvatar, message, isCompact, context);

        } else if (holder instanceof VoiceViewHolder) {
            VoiceViewHolder vh = (VoiceViewHolder) holder;
            if (vh.tvDuration != null) vh.tvDuration.setText(message.getDuration());
            if (vh.tvTime != null) vh.tvTime.setText(message.getTime());
            if (vh.ivPlayPause != null) {
                vh.ivPlayPause.setOnClickListener(v -> {
                    if (vh.isPlaying) stopPlayback(vh);
                    else startPlayback(vh, message.getDuration());
                });
            }
            bindAvatar(vh.ivUserAvatar, message, isCompact, context);

        } else if (holder instanceof ImageViewHolder) {
            ImageViewHolder ivh = (ImageViewHolder) holder;
            if (ivh.tvTime != null) ivh.tvTime.setText(message.getTime());
            if (message.getMediaUrl() != null) {
                GlideUtils.loadImage(context, message.getMediaUrl(), ivh.ivMessageImage);
            }
            if (ivh.ivPlayVideo != null) {
                ivh.ivPlayVideo.setVisibility(message.getType() == MessageModel.MessageType.VIDEO ? View.VISIBLE : View.GONE);
            }
            bindAvatar(ivh.ivUserAvatar, message, isCompact, context);
            ivh.itemView.setOnClickListener(v -> { if (longClickListener != null) longClickListener.onMediaClick(message); });
            bindStatus(ivh.ivStatus, message);

        } else if (holder instanceof FileViewHolder) {
            FileViewHolder fvh = (FileViewHolder) holder;
            fvh.tvMessage.setText("📄 " + (message.getMessage() != null ? message.getMessage() : "File"));
            fvh.tvMessage.setTextSize(baseSize);
            if (fvh.tvTime != null) fvh.tvTime.setText(message.getTime());
            bindAvatar(fvh.ivUserAvatar, message, isCompact, context);
            bindStatus(fvh.ivStatus, message);
            fvh.itemView.setOnClickListener(v -> {
                if (message.getMediaUrl() != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(message.getMediaUrl()));
                    context.startActivity(intent);
                }
            });
        }
    }

    private void bindStatus(ImageView ivStatus, MessageModel message) {
        if (ivStatus == null) return;
        if (!message.isSent()) {
            ivStatus.setVisibility(View.GONE);
            return;
        }

        ivStatus.setVisibility(View.VISIBLE);
        String status = message.getStatus();
        if (status == null) status = message.isSeen() ? "READ" : "SENT";

        switch (status) {
            case "READ":
                ivStatus.setImageResource(R.drawable.ic_double_tick);
                ivStatus.setColorFilter(Color.parseColor("#4FC3F7")); // WhatsApp Blue
                break;
            case "DELIVERED":
                ivStatus.setImageResource(R.drawable.ic_double_tick);
                ivStatus.setColorFilter(Color.GRAY);
                break;
            case "SENT":
            default:
                ivStatus.setImageResource(R.drawable.ic_single_tick);
                ivStatus.setColorFilter(Color.GRAY);
                break;
        }
    }

    private void bindAvatar(ImageView ivUserAvatar, MessageModel message, boolean isCompact, Context context) {
        if (ivUserAvatar != null) {
            boolean showAvatar = !message.isSent() && !isCompact;
            ivUserAvatar.setVisibility(showAvatar ? View.VISIBLE : View.GONE);
            if (showAvatar) {
                if (partnerAvatarUri != null) Glide.with(context).load(partnerAvatarUri).circleCrop().placeholder(R.drawable.app_logo).into(ivUserAvatar);
                else ivUserAvatar.setImageResource(R.drawable.app_logo);
                ivUserAvatar.setOnClickListener(v -> {
                    Intent intent = new Intent(context, UserProfileActivity.class);
                    intent.putExtra("user_name", message.getChatPartnerName());
                    intent.putExtra("user_id", message.getChatPartnerId());
                    context.startActivity(intent);
                });
            }
        }
    }

    private void startPlayback(VoiceViewHolder vh, String durationStr) {
        vh.isPlaying = true;
        if (vh.ivPlayPause != null) vh.ivPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        if (vh.viewProgressLine != null) vh.viewProgressLine.setVisibility(View.VISIBLE);
        long durationMs = 5000; 
        try {
            if (durationStr != null && durationStr.contains(":")) {
                String[] parts = durationStr.split(":");
                durationMs = (Long.parseLong(parts[0]) * 60 + Long.parseLong(parts[1])) * 1000;
            }
        } catch (Exception ignored) {}
        if (vh.llWaveform != null && vh.viewProgressLine != null) {
            float targetX = vh.llWaveform.getWidth();
            vh.progressAnimator = ObjectAnimator.ofFloat(vh.viewProgressLine, "translationX", 0f, targetX);
            vh.progressAnimator.setDuration(durationMs);
            vh.progressAnimator.setInterpolator(new LinearInterpolator());
            vh.progressAnimator.start();
        }
        playbackHandler.postDelayed(() -> { if (vh.isPlaying) stopPlayback(vh); }, durationMs);
    }

    private void stopPlayback(VoiceViewHolder vh) {
        vh.isPlaying = false;
        if (vh.ivPlayPause != null) vh.ivPlayPause.setImageResource(android.R.drawable.ic_media_play);
        if (vh.viewProgressLine != null) {
            vh.viewProgressLine.setVisibility(View.GONE);
            vh.viewProgressLine.setTranslationX(0);
        }
        if (vh.progressAnimator != null) vh.progressAnimator.cancel();
        playbackHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivStatus, ivUserAvatar;
        public TextViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivStatus = itemView.findViewById(R.id.ivStatus);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
        }
    }

    static class VoiceViewHolder extends RecyclerView.ViewHolder {
        TextView tvDuration, tvTime;
        ImageView ivPlayPause, ivUserAvatar;
        View viewProgressLine, llWaveform;
        boolean isPlaying = false;
        ObjectAnimator progressAnimator;
        public VoiceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivPlayPause = itemView.findViewById(R.id.ivPlayPause);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            viewProgressLine = itemView.findViewById(R.id.viewProgressLine);
            llWaveform = itemView.findViewById(R.id.llWaveform);
        }
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMessageImage, ivStatus, ivUserAvatar, ivPlayVideo;
        TextView tvTime;
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
            ivStatus = itemView.findViewById(R.id.ivStatus);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivPlayVideo = itemView.findViewById(R.id.ivPlayVideo);
        }
    }

    static class FileViewHolder extends TextViewHolder {
        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
