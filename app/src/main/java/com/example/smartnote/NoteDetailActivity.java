package com.example.smartnote;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartnote.model.Note;
import com.example.smartnote.network.ApiClient;
import com.example.smartnote.network.ApiService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class NoteDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvContent, tvBookmark, tvCreatedAt, tvUpdatedAt;
    private FloatingActionButton fabEditNote; // Added FloatingActionButton
    private ApiService apiService;
    private AlertDialog progressDialog;
    private Long noteId; // Store the noteId
    private static final String TAG = "NoteDetailActivity";

    // ActivityResultLauncher for handling results from AddNoteActivity (for editing)
    private ActivityResultLauncher<Intent> editNoteLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        apiService = ApiClient.getApiService();
        initializeViews();

        // Initialize ActivityResultLauncher for editing notes
        editNoteLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // If the note was successfully edited, reload its data
                        loadNoteData();
                    }
                }
        );

        // Get note ID from intent
        noteId = getIntent().getLongExtra("note_id", -1L);

        if (noteId == -1L) {
            showToast("Error: Note ID not found.");
            finish(); // Close the activity if no valid ID is provided
            return;
        }

        loadNoteData(); // Load note data initially
    }

    private void initializeViews() {
        tvTitle = findViewById(R.id.tv_note_title);
        tvContent = findViewById(R.id.tv_note_content);
        tvBookmark = findViewById(R.id.tv_bookmark_status);
        tvCreatedAt = findViewById(R.id.tv_created_at);
        tvUpdatedAt = findViewById(R.id.tv_updated_at);
        fabEditNote = findViewById(R.id.fab_edit_note); // Initialize the FAB

        // Set OnClickListener for the Floating Action Button
        fabEditNote.setOnClickListener(v -> {
            Intent intent = new Intent(NoteDetailActivity.this, AddNoteActivity.class);
            intent.putExtra("note_id", noteId); // Pass the current note ID for editing
            editNoteLauncher.launch(intent); // Use the launcher to start activity
        });
    }

    private void loadNoteData() {
        showProgress("Loading Note...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Make the API call to fetch the specific note with versions
                Response<Note> response = apiService.getNoteWithVersions(noteId).execute();

                runOnUiThread(() -> {
                    dismissProgress();
                    if (response.isSuccessful() && response.body() != null) {
                        Note note = response.body();
                        displayNoteData(note);
                    } else {
                        Log.e(TAG, "Failed to load note: " + response.code() + " - " + response.message());
                        showToast("Failed to load note details.");
                        // Only finish if it's the initial load and it fails.
                        // If it's a refresh after edit, keep the old data visible.
                        if (tvTitle.getText().toString().isEmpty()) { // Simple check if initial load failed
                            finish();
                        }
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    Log.e(TAG, "Network error fetching note: " + e.getMessage(), e);
                    showToast("Network error: " + e.getMessage());
                    if (tvTitle.getText().toString().isEmpty()) { // Simple check if initial load failed
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    Log.e(TAG, "Unexpected error fetching note: " + e.getMessage(), e);
                    showToast("An unexpected error occurred.");
                    if (tvTitle.getText().toString().isEmpty()) { // Simple check if initial load failed
                        finish();
                    }
                });
            }
        });
    }

    private void displayNoteData(Note note) {
        tvTitle.setText(note.getTitle());
        tvContent.setText(note.getContent());
        tvBookmark.setText("Bookmarked: " + (note.isBookmarked() ? "Yes" : "No"));

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        if (note.getCreatedAt() != null) {
            tvCreatedAt.setText("Created: " + dateFormat.format(note.getCreatedAt()));
        } else {
            tvCreatedAt.setText("Created: N/A");
        }

        if (note.getUpdatedAt() != null) {
            tvUpdatedAt.setText("Updated: " + dateFormat.format(note.getUpdatedAt()));
        } else {
            tvUpdatedAt.setText("Updated: N/A");
        }
    }

    private void showProgress(String message) {
        runOnUiThread(() -> {
            progressDialog = DialogUtils.createProgressDialog(this, message);
            progressDialog.show();
        });
    }

    private void dismissProgress() {
        if (!isFinishing() && progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showToast(String message) {
        runOnUiThread(() ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    protected void onDestroy() {
        dismissProgress(); // Ensure dialog is dismissed to prevent window leaks
        super.onDestroy();
    }
}
