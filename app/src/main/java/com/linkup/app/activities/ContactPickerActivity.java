package com.linkup.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.adapters.ChatAdapter;
import com.linkup.app.adapters.SelectedUserAdapter;
import com.linkup.app.models.ChatModel;
import java.util.ArrayList;
import java.util.List;

public class ContactPickerActivity extends BaseActivity {

    private ChatAdapter adapter;
    private SelectedUserAdapter selectedAdapter;
    private List<ChatModel> contactList;
    private List<ChatModel> selectedUsersList;
    private Button btnDone;
    private RecyclerView rvSelectedUsers;
    private String forwardMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_picker);

        forwardMessage = getIntent().getStringExtra("forward_message");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (forwardMessage != null) {
                getSupportActionBar().setTitle("Forward Message");
            } else {
                getSupportActionBar().setTitle("Select Contacts");
            }
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rvContacts = findViewById(R.id.rvContacts);
        rvSelectedUsers = findViewById(R.id.rvSelectedUsers);
        EditText etSearch = findViewById(R.id.etSearch);
        btnDone = findViewById(R.id.btnDone);

        contactList = new ArrayList<>();
        selectedUsersList = new ArrayList<>();
        
        // Mock contacts
        contactList.add(new ChatModel("ANNA", "Online", "Now", 0, false, true));
        contactList.add(new ChatModel("Michael Jordan", "Available", "Now", 0, false, false));
        contactList.add(new ChatModel("Jane Doe", "At work", "Now", 0, false, true));
        contactList.add(new ChatModel("Sarah Connor", "Away", "Now", 0, false, false));
        contactList.add(new ChatModel("John Wick", "Focused", "Now", 0, false, true));
        contactList.add(new ChatModel("Bruce Wayne", "I'm Batman", "Now", 0, false, false));
        contactList.add(new ChatModel("Tony Stark", "I am Iron Man", "Now", 0, false, true));
        contactList.add(new ChatModel("Steve Rogers", "I can do this all day", "Now", 0, false, false));

        adapter = new ChatAdapter(contactList);
        adapter.setSelectionMode(true, count -> updateSelectionUI());

        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        rvContacts.setAdapter(adapter);

        selectedAdapter = new SelectedUserAdapter(selectedUsersList, user -> {
            user.setSelected(false);
            adapter.notifyDataSetChanged();
            updateSelectionUI();
        });
        rvSelectedUsers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSelectedUsers.setAdapter(selectedAdapter);

        btnDone.setEnabled(false);
        btnDone.setOnClickListener(v -> {
            ArrayList<String> selectedIds = new ArrayList<>();
            for (ChatModel chat : selectedUsersList) {
                selectedIds.add(chat.getUserName());
            }
            
            Intent resultIntent = new Intent();
            resultIntent.putStringArrayListExtra("selected_users", selectedIds);
            setResult(RESULT_OK, resultIntent);
            
            if (forwardMessage != null) {
                Toast.makeText(this, "Forwarded to " + selectedIds.size() + " contacts", Toast.LENGTH_SHORT).show();
            }
            
            finish();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateSelectionUI() {
        selectedUsersList.clear();
        for (ChatModel chat : contactList) {
            if (chat.isSelected()) {
                selectedUsersList.add(chat);
            }
        }
        
        int count = selectedUsersList.size();
        btnDone.setText(getString(R.string.confirm_selection, count));
        btnDone.setEnabled(count > 0);
        
        if (count > 0) {
            rvSelectedUsers.setVisibility(View.VISIBLE);
        } else {
            rvSelectedUsers.setVisibility(View.GONE);
        }
        selectedAdapter.notifyDataSetChanged();
        if (count > 0) {
            rvSelectedUsers.smoothScrollToPosition(count - 1);
        }
    }

    private void filter(String query) {
        List<ChatModel> filteredList = new ArrayList<>();
        for (ChatModel contact : contactList) {
            if (contact.getUserName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(contact);
            }
        }
        adapter.setChatList(filteredList);
    }
}
