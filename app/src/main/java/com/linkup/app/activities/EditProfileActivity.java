package com.linkup.app.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.linkup.app.R;
import com.bumptech.glide.Glide;

public class EditProfileActivity extends BaseActivity {

    private ImageView ivEditAvatar;
    private EditText etDisplayName, etBio, etCity, etProfession, etBirthDate, etEducation, etHobbies, etWebsite, etPhone;
    private Uri selectedImageUri;
    private SharedPreferences appPrefs;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        selectedImageUri = uri;
                        Glide.with(this).load(selectedImageUri).centerCrop().into(ivEditAvatar);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        appPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        
        ivEditAvatar = findViewById(R.id.ivEditAvatar);
        etDisplayName = findViewById(R.id.etDisplayName);
        etBio = findViewById(R.id.etBio);
        etCity = findViewById(R.id.etCity);
        etProfession = findViewById(R.id.etProfession);
        etBirthDate = findViewById(R.id.etBirthDate);
        etEducation = findViewById(R.id.etEducation);
        etHobbies = findViewById(R.id.etHobbies);
        etWebsite = findViewById(R.id.etWebsite);
        etPhone = findViewById(R.id.etPhone);

        loadProfileData();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.fabChangePhoto).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> saveProfile());
    }

    private void loadProfileData() {
        etDisplayName.setText(appPrefs.getString("user_full_name", ""));
        etBio.setText(appPrefs.getString("user_bio", ""));
        etCity.setText(appPrefs.getString("user_city", ""));
        etProfession.setText(appPrefs.getString("user_profession", ""));
        etBirthDate.setText(appPrefs.getString("user_birth_date", ""));
        etEducation.setText(appPrefs.getString("user_education", ""));
        etHobbies.setText(appPrefs.getString("user_hobbies", ""));
        etWebsite.setText(appPrefs.getString("user_website", ""));
        etPhone.setText(appPrefs.getString("user_phone", ""));

        String avatarUriStr = appPrefs.getString("user_avatar_uri", null);
        if (avatarUriStr == null) {
            avatarUriStr = getSharedPreferences("LinkUpUsage", Context.MODE_PRIVATE).getString("user_avatar_uri", null);
        }
        
        if (avatarUriStr != null) {
            selectedImageUri = Uri.parse(avatarUriStr);
            Glide.with(this).load(selectedImageUri).centerCrop().into(ivEditAvatar);
        }
    }

    private void saveProfile() {
        String name = etDisplayName.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String profession = etProfession.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();
        String education = etEducation.getText().toString().trim();
        String hobbies = etHobbies.getText().toString().trim();
        String website = etWebsite.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString("user_full_name", name);
        editor.putString("user_bio", bio);
        editor.putString("user_city", city);
        editor.putString("user_profession", profession);
        editor.putString("user_birth_date", birthDate);
        editor.putString("user_education", education);
        editor.putString("user_hobbies", hobbies);
        editor.putString("user_website", website);
        editor.putString("user_phone", phone);
        
        if (selectedImageUri != null) {
            String uriStr = selectedImageUri.toString();
            editor.putString("user_avatar_uri", uriStr);
            getSharedPreferences("LinkUpUsage", Context.MODE_PRIVATE).edit()
                .putString("user_avatar_uri", uriStr).apply();
        }
        editor.apply();

        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("updated_name", name);
        resultIntent.putExtra("updated_bio", bio);
        if (selectedImageUri != null) {
            resultIntent.putExtra("updated_avatar", selectedImageUri.toString());
        }
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
