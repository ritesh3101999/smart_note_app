package com.example.smartnote;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartnote.adapter.NoteAdapter;
import com.example.smartnote.model.Note;
import com.example.smartnote.network.ApiClient;
import com.example.smartnote.network.ApiService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class FolderNotesActivity extends AppCompatActivity implements NoteAdapter.OnNoteClickListener {

    private RecyclerView rvNotes;
    private NoteAdapter noteAdapter;
    private TextView tvFolderName;
    private ApiService apiService;
    private AlertDialog progressDialog;
    private Long folderId;
    private boolean isBookmarkedFolder = false; // New flag to indicate if it's the bookmarked folder

    // ActivityResultLauncher for handling results from AddNoteActivity
    private ActivityResultLauncher<Intent> addNoteLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_notes);

        apiService = ApiClient.getApiService();
        initializeViews();
        getIntentData();
        setupRecyclerView();
        setupListeners();
        loadFolderNotes();
        // Adjust the FAB visibility based on whether it's the bookmarked folder
        // The bookmarked folder is a view, not a place to add new notes.
        findViewById(R.id.fab_add_note).setVisibility(isBookmarkedFolder ? android.view.View.GONE : android.view.View.VISIBLE);

        // Initialize ActivityResultLauncher
        addNoteLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // If a note was successfully added/edited, refresh the list
                        loadFolderNotes();
                    }
                }
        );
    }

    private void initializeViews() {
        tvFolderName = findViewById(R.id.tv_folder_name);
        rvNotes = findViewById(R.id.rv_notes);
        // fabAddNote = findViewById(R.id.fab_add_note); // Removed this, will get it dynamically later
    }

    private void getIntentData() {
        Intent intent = getIntent();
        isBookmarkedFolder = intent.getBooleanExtra("is_bookmarked_folder", false);
        String folderName = intent.getStringExtra("folder_name");

        if (isBookmarkedFolder) {
            tvFolderName.setText("Bookmarked Notes");
        } else {
            folderId = intent.getLongExtra("folder_id", -1);
            if (folderId == -1) {
                Toast.makeText(this, "Invalid folder", Toast.LENGTH_SHORT).show();
                finish();
            }
            tvFolderName.setText(folderName != null ? folderName : "Folder Notes");
        }
    }

    private void setupRecyclerView() {
        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        noteAdapter = new NoteAdapter(this);
        rvNotes.setAdapter(noteAdapter);
    }

    private void setupListeners() {
        // Set OnClickListener for the Floating Action Button
        findViewById(R.id.fab_add_note).setOnClickListener(v -> {
            Intent intent = new Intent(FolderNotesActivity.this, AddNoteActivity.class);
            // Only pass folder_id if it's a regular folder
            if (!isBookmarkedFolder) {
                intent.putExtra("folder_id", folderId);
            }
            addNoteLauncher.launch(intent); // Use the launcher to start activity
        });
    }

    private void loadFolderNotes() {
        showProgress("Loading Notes...");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Response<List<Note>> response;
                if (isBookmarkedFolder) {
                    // Call the API to get bookmarked notes
                    response = apiService.getBookmarkedNotes().execute();
                } else {
                    // Call the API to get notes by folder ID
                    response = apiService.getNotesByFolder(folderId).execute();
                }

                runOnUiThread(() -> {
                    dismissProgress();
                    if (response.isSuccessful() && response.body() != null) {
                        noteAdapter.setNotes(response.body());
                    } else {
                        showToast("Failed to load notes");
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    showToast("Connection error: " + e.getMessage());
                });
            }
        });
    }

    private void showProgress(String message) {
        runOnUiThread(() -> {
            progressDialog = DialogUtils.createProgressDialog(this, message);
            progressDialog.show();
        });
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onNoteClick(Note note) {
        Intent intent = new Intent(this, NoteDetailActivity.class);
        intent.putExtra("note_id", note.getId());
        startActivity(intent);
    }

    // This method is called from NoteAdapter when a bookmark icon is toggled
    @Override
    public void onBookmarkToggle(Note note, boolean isBookmarked) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Call the API to toggle the bookmark status
                Response<Note> response = apiService.toggleBookmark(note.getId()).execute();
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        // If successful, refresh the current list of notes
                        loadFolderNotes();
                    } else {
                        showToast("Failed to toggle bookmark");
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> showToast("Network error: " + e.getMessage()));
            }
        });
    }
}
