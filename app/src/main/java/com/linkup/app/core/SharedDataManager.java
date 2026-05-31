package com.linkup.app.core;

import com.linkup.app.database.FirebaseDatabaseManager;
import com.linkup.app.models.ChatModel;
import com.linkup.app.models.Message;
import com.linkup.app.models.Post;
import com.linkup.app.models.NotificationModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SharedDataManager manages the application's global data state.
 * Optimized with Map-based lookups and persistent ID management.
 */
public class SharedDataManager {
    private static SharedDataManager instance;
    
    private final List<Post> posts = new ArrayList<>();
    private final Map<String, Post> postMap = new HashMap<>();
    
    private final List<ChatModel> groups = new ArrayList<>();
    private final Map<String, ChatModel> groupMap = new HashMap<>();
    private final Set<String> groupNamesLower = new HashSet<>();

    private final List<NotificationModel> notifications = new ArrayList<>();
    
    private final List<OnPostAddedListener> postListeners = new CopyOnWriteArrayList<>();
    private OnGroupChangedListener groupListener;
    
    private String activeChatUserId;

    public interface OnPostAddedListener {
        void onPostAdded(Post post);
    }

    public interface OnGroupChangedListener {
        void onGroupChanged(ChatModel group);
    }

    public static synchronized SharedDataManager getInstance() {
        if (instance == null) {
            instance = new SharedDataManager();
        }
        return instance;
    }

    private SharedDataManager() {
        resetToDefault();
    }

    public void setActiveChatUserId(String userId) {
        this.activeChatUserId = userId;
    }

    public String getActiveChatUserId() {
        return activeChatUserId;
    }

    public void clearData() {
        posts.clear();
        postMap.clear();
        groups.clear();
        groupMap.clear();
        groupNamesLower.clear();
        notifications.clear();
        resetToDefault();
    }

    private void resetToDefault() {
        addGroup(new ChatModel("LinkUp Support", "Welcome to LinkUp! How can we help you today?", "", 0, false));
    }

    // --- POST OPERATIONS ---

    public void addPost(Post post) {
        if (post == null || post.postId == null) return;
        if (System.currentTimeMillis() > post.getExpiryTimestampMillis()) return;

        Post existing = postMap.get(post.postId);
        if (existing != null) posts.remove(existing);
        
        posts.add(0, post);
        postMap.put(post.postId, post);
        sortPosts();
        notifyPostAdded(post);
    }

    private void sortPosts() {
        long now = System.currentTimeMillis();
        posts.removeIf(post -> {
            boolean expired = now > post.getExpiryTimestampMillis();
            if (expired) postMap.remove(post.postId);
            return expired;
        });

        Collections.sort(posts, (p1, p2) -> {
            if (p1.hasBeenSeen != p2.hasBeenSeen) return p1.hasBeenSeen ? 1 : -1;
            return Long.compare(p2.getTimestampMillis(), p1.getTimestampMillis());
        });
    }

    public List<Post> getPosts() {
        sortPosts();
        return new ArrayList<>(posts);
    }

    public List<Post> getFilteredPosts(Set<String> friendNames, String currentUserId) {
        List<Post> filtered = new ArrayList<>();
        for (Post p : posts) {
            boolean isMine = currentUserId != null && currentUserId.equals(p.userId);
            boolean isFriend = p.userName != null && friendNames.contains(p.userName.toLowerCase());
            if (isMine || isFriend) filtered.add(p);
        }
        return filtered;
    }

    public void addPostListener(OnPostAddedListener listener) {
        if (listener != null && !postListeners.contains(listener)) postListeners.add(listener);
    }

    public void removePostListener(OnPostAddedListener listener) {
        if (listener != null) postListeners.remove(listener);
    }
    
    private void notifyPostAdded(Post post) {
        for (OnPostAddedListener listener : postListeners) listener.onPostAdded(post);
    }

    // --- GROUP / CHAT OPERATIONS ---

    public void handleIncomingMessage(Message msg) {
        if (msg == null) return;
        
        // Fetch user details to update the name/photo in the inbox and show notification
        FirebaseDatabaseManager.getInstance().fetchUserById(msg.getSenderId(), users -> {
            String name = "Unknown";
            String photo = null;
            if (users != null && !users.isEmpty()) {
                name = users.get(0).getFullName();
                photo = users.get(0).getProfilePhoto();
            }

            ChatModel chat = new ChatModel(
                name,
                msg.getMessageText(),
                FirebaseDatabaseManager.getInstance().formatMessageTime(msg.getTimestamp()),
                1, // Mark as having a new message
                false
            );
            chat.setUserId(msg.getSenderId());
            chat.setLastMessageTimestamp(msg.getTimestamp());
            chat.setProfilePhoto(photo);
            
            addGroup(chat);

            // Show system notification
            NotificationHelper.showMessageNotification(
                FirebaseDatabaseManager.getContext(),
                msg.getSenderId(),
                name,
                msg.getMessageText()
            );
        });
    }

    /**
     * Adds or updates a conversation.
     * Ensures IDs and profile photos are preserved during updates.
     */
    public void addGroup(ChatModel group) {
        if (group == null || group.getUserName() == null) return;
        
        String key = group.getUserId() != null ? group.getUserId() : group.getUserName().toLowerCase();
        ChatModel existing = groupMap.get(key);
        
        if (existing != null) {
            // Only update if the new message is actually newer or from cloud sync
            if (group.getLastMessageTimestamp() < existing.getLastMessageTimestamp() && group.getLastMessageTimestamp() != 0) {
                return; 
            }

            groups.remove(existing);
            // Isolation Fix: If the update is missing ID or photo, preserve existing ones
            if (group.getUserId() == null) group.setUserId(existing.getUserId());
            if (group.getProfilePhoto() == null) group.setProfilePhoto(existing.getProfilePhoto());
            if (group.getLastMessageTimestamp() == 0) group.setLastMessageTimestamp(existing.getLastMessageTimestamp());
        }
        
        groups.add(0, group);
        groupMap.put(key, group);
        groupNamesLower.add(group.getUserName().toLowerCase());

        sortGroups();

        if (groupListener != null) groupListener.onGroupChanged(group);
    }

    private void sortGroups() {
        Collections.sort(groups, (g1, g2) -> Long.compare(g2.getLastMessageTimestamp(), g1.getLastMessageTimestamp()));
    }

    public List<ChatModel> getGroups() {
        return new ArrayList<>(groups);
    }
    
    public Set<String> getGroupNamesSet() {
        return new HashSet<>(groupNamesLower);
    }
    
    public ChatModel getGroup(String name) {
        if (name == null) return null;
        for (ChatModel chat : groups) {
            if (name.equalsIgnoreCase(chat.getUserName())) return chat;
        }
        return null;
    }

    public void setGroupListener(OnGroupChangedListener listener) {
        this.groupListener = listener;
    }

    // --- NOTIFICATION OPERATIONS ---

    public void addNotification(NotificationModel notification) {
        if (notification == null) return;
        notifications.add(0, notification);
    }

    public List<NotificationModel> getNotifications() {
        return new ArrayList<>(notifications);
    }
}
