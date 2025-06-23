package com.example.smartnote;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.smartnote.model.Folder;
import com.example.smartnote.model.Note;
import com.example.smartnote.network.ApiClient;
import com.example.smartnote.network.ApiService;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Response;

public class AddNoteActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private Button btnSave; // Reference to the save button
    private ApiService apiService;
    private AlertDialog progressDialog;
    private Long folderId = null; // To store the folder ID if passed (for new notes)
    private Long noteId = null; // To store the note ID if passed (for editing existing notes)

    private static final String TAG = "AddNoteActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        apiService = ApiClient.getApiService();
        etTitle = findViewById(R.id.et_note_title);
        etContent = findViewById(R.id.et_note_content);
        btnSave = findViewById(R.id.btn_save_note); // Initialize the button

        // Get folderId from intent if it exists (for creating a new note in a folder)
        Intent intent = getIntent();
        if (intent.hasExtra("folder_id")) {
            folderId = intent.getLongExtra("folder_id", -1L);
            if (folderId == -1L) {
                folderId = null; // Reset to null if invalid ID was passed
            }
        }

        // Get noteId from intent if it exists (for editing an existing note)
        if (intent.hasExtra("note_id")) {
            noteId = intent.getLongExtra("note_id", -1L);
            if (noteId != -1L) {
                // If noteId is present, load existing note data for editing
                loadNoteForEditing(noteId);
                btnSave.setText("Update Note"); // Change button text to "Update Note"
            } else {
                noteId = null; // Reset to null if invalid ID was passed
            }
        } else {
            btnSave.setText("Save Note"); // Default button text for new notes
        }

        btnSave.setOnClickListener(v -> saveNote());
    }

    private void loadNoteForEditing(Long id) {
        showProgress("Loading Note for Editing...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Response<Note> response = apiService.getNoteWithVersions(id).execute();

                runOnUiThread(() -> {
                    dismissProgress();
                    if (response.isSuccessful() && response.body() != null) {
                        Note note = response.body();
                        etTitle.setText(note.getTitle());
                        etContent.setText(note.getContent());
                        // If the note has a folder, you might want to store its ID too
                        if (note.getFolder() != null) {
                            folderId = note.getFolder().getId();
                        }
                    } else {
                        Log.e(TAG, "Failed to load note for editing: " + response.code() + " - " + response.message());
                        showToast("Failed to load note for editing.");
                        finish(); // Close activity if note cannot be loaded for editing
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    Log.e(TAG, "Network error loading note for editing: " + e.getMessage(), e);
                    showToast("Network error: " + e.getMessage());
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    Log.e(TAG, "Unexpected error loading note for editing: " + e.getMessage(), e);
                    showToast("An unexpected error occurred.");
                    finish();
                });
            }
        });
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            showToast("Please fill all fields");
            return;
        }

        showProgress(noteId == null ? "Saving Note..." : "Updating Note...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Note noteToSave = new Note();
                noteToSave.setTitle(title);
                noteToSave.setContent(content);

                Response<Note> response;

                if (noteId == null) {
                    // Creating a new note
                    noteToSave.setId(null); // Ensure ID is null for new notes
                    if (folderId != null) {
                        Folder folder = new Folder();
                        folder.setId(folderId);
                        noteToSave.setFolder(folder);
                    }
                    response = apiService.createNote(noteToSave).execute();
                } else {
                    // Updating an existing note
                    noteToSave.setId(noteId); // Set the ID for update
                    // If the original note had a folder, maintain it
                    if (folderId != null) {
                        Folder folder = new Folder();
                        folder.setId(folderId);
                        noteToSave.setFolder(folder);
                    }
                    response = apiService.updateNote(noteId, noteToSave).execute();
                }

                runOnUiThread(() -> {
                    dismissProgress();
                    if (response.isSuccessful() && response.body() != null) {
                        showToast(noteId == null ? "Note saved successfully" : "Note updated successfully");
                        setResult(RESULT_OK); // Indicate success to the calling activity
                        finish();
                    } else {
                        String errorMessage = "Error: " + response.code() + " - " + response.message();
                        Log.e(TAG, (noteId == null ? "Error saving note: " : "Error updating note: ") + errorMessage);
                        showToast(noteId == null ? "Error saving note: " : "Error updating note: " + response.message());
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    Log.e(TAG, "Network Error during save/update: " + e.getMessage(), e);
                    showToast("Connection error: " + e.getMessage());
                });
            } catch (JsonSyntaxException e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    Log.e(TAG, "JSON Parsing Error: " + e.getMessage(), e);
                    showToast("Server response error. Try again.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    Log.e(TAG, "Unexpected error during save/update: " + e.getMessage(), e);
                    showToast("An unexpected error occurred.");
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
        dismissProgress();
        super.onDestroy();
    }
}
