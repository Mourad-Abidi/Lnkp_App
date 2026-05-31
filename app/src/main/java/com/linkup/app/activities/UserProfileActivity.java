package com.linkup.app.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.adapters.ProfilePostAdapter;
import com.linkup.app.core.SharedDataManager;
import com.linkup.app.models.ChatModel;
import com.linkup.app.models.Post;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class UserProfileActivity extends BaseActivity implements SharedDataManager.OnPostAddedListener {

    private RecyclerView rvPosts;
    private TextView tvUserNameDisplay, tvUserBio, tvUserPhone, tvHeaderStatus;
    private TextView tvUserLocation, tvUserProfession, tvUserEducation, tvUserHobbies, tvUserWebsite;
    private TextView tvPostsCount;
    private ImageView ivHeaderAvatar;
    private CollapsingToolbarLayout collapsingToolbar;
    private MaterialButton btnMessageAction, btnEditProfile;
    private View llBlockUser;
    private SharedPreferences appPrefs, usagePrefs;
    private TabLayout tabLayout;
    private ProfilePostAdapter profilePostAdapter;
    private List<Post> displayedPosts = new ArrayList<>();
    private String currentProfileName;
    private String currentProfileId;
    
    private final ActivityResultLauncher<Intent> editProfileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    refreshProfileData();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        appPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        usagePrefs = getSharedPreferences("LinkUpUsage", Context.MODE_PRIVATE);

        // Bind Views
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        ivHeaderAvatar = findViewById(R.id.ivHeaderAvatar);
        tvHeaderStatus = findViewById(R.id.tvHeaderStatus);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvUserNameDisplay = findViewById(R.id.tvUserNameDisplay);
        tvUserBio = findViewById(R.id.tvUserBio);
        
        tvUserLocation = findViewById(R.id.tvUserLocation);
        tvUserProfession = findViewById(R.id.tvUserProfession);
        tvUserEducation = findViewById(R.id.tvUserEducation);
        tvUserHobbies = findViewById(R.id.tvUserHobbies);
        tvUserWebsite = findViewById(R.id.tvUserWebsite);
        
        rvPosts = findViewById(R.id.rvPosts);
        tabLayout = findViewById(R.id.tabLayout);
        
        tvPostsCount = findViewById(R.id.tvPostsCount);
        
        btnMessageAction = findViewById(R.id.btnMessageAction);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        
        llBlockUser = findViewById(R.id.llBlockUser);

        refreshProfileData();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        setupMediaTabs();
        
        SharedDataManager.getInstance().addPostListener(this);
    }

    private void refreshProfileData() {
        String mySavedName = appPrefs.getString("user_full_name", getString(R.string.agent_phantom));
        String intentUserName = getIntent().getStringExtra("user_name");
        currentProfileId = getIntent().getStringExtra("user_id");
        
        currentProfileName = (intentUserName == null || "You".equalsIgnoreCase(intentUserName) || getString(R.string.agent_phantom).equalsIgnoreCase(intentUserName)) 
                                ? mySavedName : intentUserName;
        
        boolean isCurrentUser = mySavedName.equalsIgnoreCase(currentProfileName);

        if (collapsingToolbar != null) {
            collapsingToolbar.setTitle(currentProfileName);
        }

        String usernameFormatted = "@" + currentProfileName.toLowerCase().replace(" ", "_");
        if (tvUserNameDisplay != null) {
            tvUserNameDisplay.setText(usernameFormatted);
        }

        updateStats(currentProfileName, isCurrentUser);

        if (isCurrentUser) {
            // Load My Profile specific data from SharedPreferences
            String myAvatar = appPrefs.getString("user_avatar_uri", usagePrefs.getString("user_avatar_uri", null));
            if (myAvatar != null && ivHeaderAvatar != null) {
                Glide.with(this).load(Uri.parse(myAvatar)).centerCrop().placeholder(R.drawable.my_background_6).into(ivHeaderAvatar);
            }
            
            if (tvUserPhone != null) tvUserPhone.setText(appPrefs.getString("user_phone", "+1 000 000 000"));
            if (tvUserBio != null) tvUserBio.setText(appPrefs.getString("user_bio", getString(R.string.no_bio)));
            
            if (tvUserLocation != null) tvUserLocation.setText(appPrefs.getString("user_city", getString(R.string.not_set)));
            if (tvUserProfession != null) tvUserProfession.setText(appPrefs.getString("user_profession", getString(R.string.not_set)));
            if (tvUserEducation != null) tvUserEducation.setText(appPrefs.getString("user_education", getString(R.string.not_set)));
            if (tvUserHobbies != null) tvUserHobbies.setText(appPrefs.getString("user_hobbies", getString(R.string.not_set)));
            if (tvUserWebsite != null) tvUserWebsite.setText(appPrefs.getString("user_website", getString(R.string.no_website)));
            
            if (tvHeaderStatus != null) tvHeaderStatus.setText("My Profile");

            // Actions
            if (btnMessageAction != null) btnMessageAction.setVisibility(View.GONE);

            if (btnEditProfile != null) {
                btnEditProfile.setVisibility(View.VISIBLE);
                btnEditProfile.setOnClickListener(v -> {
                    Intent intent = new Intent(this, EditProfileActivity.class);
                    editProfileLauncher.launch(intent);
                });
            }
            
            if (llBlockUser != null) llBlockUser.setVisibility(View.GONE);

        } else {
            // Other User logic
            if (tvUserPhone != null) tvUserPhone.setText("Hidden");
            if (tvHeaderStatus != null) tvHeaderStatus.setText(getString(R.string.online));
            
            if (tvUserLocation != null) tvUserLocation.setText(getString(R.string.not_set));
            if (tvUserProfession != null) tvUserProfession.setText(getString(R.string.not_set));
            if (tvUserEducation != null) tvUserEducation.setText(getString(R.string.not_set));
            if (tvUserHobbies != null) tvUserHobbies.setText(getString(R.string.not_set));
            if (tvUserWebsite != null) tvUserWebsite.setText(getString(R.string.no_website));

            // Setup Message Button - Always visible for other users now
            if (btnMessageAction != null) {
                btnMessageAction.setVisibility(View.VISIBLE);
                btnMessageAction.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("user_name", currentProfileName);
                    intent.putExtra("user_id", currentProfileId);
                    startActivity(intent);
                });
            }
            
            if (btnEditProfile != null) btnEditProfile.setVisibility(View.GONE);
        }

        setupCopyListeners(usernameFormatted);
        setupPostsGrid(isCurrentUser, currentProfileName);
    }

    private void updateStats(String userName, boolean isCurrentUser) {
        int count = 0;
        if (isCurrentUser) {
            count = SharedDataManager.getInstance().getPosts().size();
        } else {
            boolean hasChat = false;
            for (ChatModel chat : SharedDataManager.getInstance().getGroups()) {
                if (chat.getUserName().equalsIgnoreCase(userName)) {
                    hasChat = true;
                    break;
                }
            }
            if (hasChat) {
               for (Post p : SharedDataManager.getInstance().getPosts()) {
                   if (p.userName.equalsIgnoreCase(userName)) count++;
               }
            }
        }
        if (tvPostsCount != null) {
            tvPostsCount.setText(String.valueOf(count));
        }
    }

    private void setupCopyListeners(String username) {
        View.OnLongClickListener copyListener = v -> {
            String textToCopy = "";
            if (v.getId() == R.id.llPhoneRow) textToCopy = tvUserPhone.getText().toString();
            else if (v.getId() == R.id.llUsernameRow) textToCopy = username;

            if (!textToCopy.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("LinkUp Info", textToCopy);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, getString(R.string.copied_to_clipboard, textToCopy), Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        };
        View phoneRow = findViewById(R.id.llPhoneRow);
        if (phoneRow != null) phoneRow.setOnLongClickListener(copyListener);
        View userRow = findViewById(R.id.llUsernameRow);
        if (userRow != null) userRow.setOnLongClickListener(copyListener);
    }

    private void setupMediaTabs() {
        if (tabLayout != null) {
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {}
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}
                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }
    }

    private void setupPostsGrid(boolean isCurrentUser, String userName) {
        if (rvPosts != null) {
            rvPosts.setLayoutManager(new GridLayoutManager(this, 3));
            displayedPosts.clear();
            
            boolean hasChat = false;
            for (ChatModel chat : SharedDataManager.getInstance().getGroups()) {
                if (chat.getUserName().equalsIgnoreCase(userName)) {
                    hasChat = true;
                    break;
                }
            }

            if (isCurrentUser || hasChat) {
                if (isCurrentUser) {
                    displayedPosts.addAll(SharedDataManager.getInstance().getPosts());
                } else {
                    for (Post p : SharedDataManager.getInstance().getPosts()) {
                        if (p.userName.equalsIgnoreCase(userName)) {
                            displayedPosts.add(p);
                        }
                    }
                }
            }
            profilePostAdapter = new ProfilePostAdapter(displayedPosts);
            rvPosts.setAdapter(profilePostAdapter);
        }
    }

    @Override
    public void onPostAdded(Post post) {
        runOnUiThread(() -> {
            String mySavedName = appPrefs.getString("user_full_name", getString(R.string.agent_phantom));
            boolean isCurrentUserProfile = mySavedName.equalsIgnoreCase(currentProfileName);
            
            updateStats(currentProfileName, isCurrentUserProfile);
            
            // Refresh posts list
            displayedPosts.clear();
            if (isCurrentUserProfile) {
                displayedPosts.addAll(SharedDataManager.getInstance().getPosts());
            } else {
                for (Post p : SharedDataManager.getInstance().getPosts()) {
                    if (p.userName.equalsIgnoreCase(currentProfileName)) {
                        displayedPosts.add(p);
                    }
                }
            }
            if (profilePostAdapter != null) {
                profilePostAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedDataManager.getInstance().removePostListener(this);
    }
}
