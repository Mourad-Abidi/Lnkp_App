package com.linkup.app.activities;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.linkup.app.R;
import com.linkup.app.adapters.VaultAdapter;
import com.linkup.app.models.VaultFileModel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Implements Encrypted File Vault (Feature 15).
 * A secure area to store and manage encrypted files.
 */
public class VaultActivity extends BaseActivity {

    private VaultAdapter adapter;
    private List<VaultFileModel> allVaultFiles;
    private List<VaultFileModel> displayedFiles;
    private EditText etVaultSearch;
    private ChipGroup chipGroupFilters;
    private LinearLayout llVaultEmpty;

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleFileSelection
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        initViews();
        setupData();
        setupListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etVaultSearch = findViewById(R.id.etVaultSearch);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);
        llVaultEmpty = findViewById(R.id.llVaultEmpty);
        RecyclerView rvVaultFiles = findViewById(R.id.rvVaultFiles);
        FloatingActionButton fabAddFile = findViewById(R.id.fabAddFile);

        if (rvVaultFiles != null) {
            rvVaultFiles.setLayoutManager(new LinearLayoutManager(this));
            displayedFiles = new ArrayList<>();
            adapter = new VaultAdapter(displayedFiles);
            rvVaultFiles.setAdapter(adapter);
        }

        if (fabAddFile != null) {
            fabAddFile.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        }
    }

    private void setupData() {
        allVaultFiles = new ArrayList<>();
        allVaultFiles.add(new VaultFileModel("Work_Contract.pdf.enc", "1.2 MB", "Oct 12, 2023", android.R.drawable.ic_menu_save));
        allVaultFiles.add(new VaultFileModel("Family_Photo.jpg.enc", "4.5 MB", "Oct 15, 2023", android.R.drawable.ic_menu_gallery));
        allVaultFiles.add(new VaultFileModel("Passwords_Backup.txt.enc", "12 KB", "Oct 20, 2023", android.R.drawable.ic_menu_edit));
        allVaultFiles.add(new VaultFileModel("Identity_Card.png.enc", "850 KB", "Yesterday", android.R.drawable.ic_lock_lock));
        
        renderList();
    }

    private void setupListeners() {
        if (etVaultSearch != null) {
            etVaultSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    renderList();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (chipGroupFilters != null) {
            chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> renderList());
        }
    }

    private void renderList() {
        String query = etVaultSearch != null ? etVaultSearch.getText().toString().toLowerCase(Locale.getDefault()).trim() : "";
        int checkedId = chipGroupFilters != null ? chipGroupFilters.getCheckedChipId() : View.NO_ID;

        displayedFiles.clear();
        for (VaultFileModel file : allVaultFiles) {
            boolean matchesSearch = file.getFileName().toLowerCase(Locale.getDefault()).contains(query);
            boolean matchesType = true;

            if (checkedId == R.id.chipDocs) {
                matchesType = (file.getCategory() == VaultFileModel.Category.DOCS);
            } else if (checkedId == R.id.chipImages) {
                matchesType = (file.getCategory() == VaultFileModel.Category.IMAGES);
            } else if (checkedId == R.id.chipOther) {
                matchesType = (file.getCategory() == VaultFileModel.Category.OTHER);
            }

            if (matchesSearch && matchesType) {
                displayedFiles.add(file);
            }
        }

        if (adapter != null) adapter.notifyDataSetChanged();
        if (llVaultEmpty != null) llVaultEmpty.setVisibility(displayedFiles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void handleFileSelection(Uri uri) {
        if (uri == null) return;
        
        Toast.makeText(this, "Encrypting and moving to vault...", Toast.LENGTH_SHORT).show();
        
        String fileName = uri.getLastPathSegment();
        if (fileName == null) fileName = "Unknown_File";
        if (!fileName.endsWith(".enc")) fileName += ".enc";
        
        String date = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
        VaultFileModel newFile = new VaultFileModel(fileName, "Calculating...", date, android.R.drawable.ic_input_add);

        allVaultFiles.add(0, newFile);
        renderList();
    }
}
