package com.example.smartnote;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.SearchView; // Import SearchView
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Import Toolbar
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartnote.adapter.FolderAdapter;
import com.example.smartnote.adapter.NoteAdapter;
import com.example.smartnote.model.Folder;
import com.example.smartnote.model.Note;
import com.example.smartnote.network.ApiClient;
import com.example.smartnote.network.ApiService;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Import FloatingActionButton

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

import android.util.Log; // Keep Log import

public class MainActivity extends AppCompatActivity implements
        NoteAdapter.OnNoteClickListener,
        FolderAdapter.OnFolderClickListener {

    private RecyclerView rvNotes, rvFolders;
    private NoteAdapter noteAdapter;
    private FolderAdapter folderAdapter;
    private ApiService apiService;
    private AlertDialog progressDialog;
    private SearchView searchViewNotes; // New: Search View
    private FloatingActionButton fabAddNote, fabAddFolder; // New: FABs

    private static final String TAG = "MainActivity";
    private static final Long BOOKMARKED_FOLDER_ID = -99L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiService = ApiClient.getApiService();
        initializeUI();
        loadData(null); // Load all notes initially, no search query
    }

    private void initializeUI() {
        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // Set the toolbar as the action bar

        // Initialize Search View
        searchViewNotes = findViewById(R.id.search_view_notes);
        searchViewNotes.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // When user submits the query, perform search
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Optional: Perform search as text changes (live search)
                // If you want live search, uncomment and adjust the logic below
                // if (newText.isEmpty()) {
                //     loadData(null); // Load all notes if search query is cleared
                // } else {
                //     performSearch(newText);
                // }
                return true;
            }
        });

        // Initialize Folder RecyclerView
        rvFolders = findViewById(R.id.rv_folders);
        rvFolders.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        folderAdapter = new FolderAdapter(this);
        rvFolders.setAdapter(folderAdapter);

        // Initialize Note RecyclerView
        rvNotes = findViewById(R.id.rv_notes);
        // Use GridLayoutManager for a visually appealing grid of notes
        rvNotes.setLayoutManager(new GridLayoutManager(this, 2));
        noteAdapter = new NoteAdapter(this);
        rvNotes.setAdapter(noteAdapter);

        // Initialize Floating Action Buttons
        fabAddNote = findViewById(R.id.fab_add_note);
        fabAddFolder = findViewById(R.id.fab_add_folder);

        // Set up click listeners for FABs
        fabAddNote.setOnClickListener(v ->
                startActivity(new Intent(this, AddNoteActivity.class)));

        fabAddFolder.setOnClickListener(v ->
                startActivity(new Intent(this, AddFolderActivity.class)));

        // Handle click for "Bookmarked Notes" section
        findViewById(R.id.btn_view_bookmarked_notes).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FolderNotesActivity.class);
            intent.putExtra("is_bookmarked_folder", true);
            intent.putExtra("folder_name", "Bookmarked Notes"); // Pass a name for the title bar
            startActivity(intent);
        });
    }

    private void loadData(String searchQuery) {
        showProgress("Loading Data...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Load folders (always load all folders, search doesn't affect folders)
                Response<List<Folder>> folderResponse = apiService.getFolders().execute();

                // Load notes based on search query or all notes if no query
                Response<List<Note>> noteResponse;
                if (searchQuery != null && !searchQuery.isEmpty()) {
                    noteResponse = apiService.searchNotes(searchQuery).execute();
                } else {
                    noteResponse = apiService.getNotes().execute();
                }

                runOnUiThread(() -> {
                    dismissProgress();

                    if (folderResponse.isSuccessful() && folderResponse.body() != null) {
                        List<Folder> folders = new ArrayList<>(folderResponse.body());

                        // Add a "Bookmarked Notes" virtual folder to the list
                        Folder bookmarkedFolder = new Folder();
                        bookmarkedFolder.setId(BOOKMARKED_FOLDER_ID); // Use the special ID
                        bookmarkedFolder.setName("Bookmarked Notes");
                        folders.add(0, bookmarkedFolder); // Add at the beginning

                        folderAdapter.setFolders(folders);
                    } else {
                        showToast("Failed to load folders");
                    }

                    if (noteResponse.isSuccessful() && noteResponse.body() != null) {
                        noteAdapter.setNotes(noteResponse.body());
                    } else {
                        showToast("Failed to load notes");
                    }
                    // FIX: Clear the search view query and remove focus after data is loaded
                    searchViewNotes.setQuery("", false);
                    searchViewNotes.clearFocus();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    showToast(getSpecificNetworkErrorMessage(e));
                });
            }
        });
    }

    private void performSearch(String query) {
        // This method will be called when the user submits a search query
        // Call loadData with the search query to filter notes
        loadData(query);
    }

    // Helper method to provide more specific network error messages
    private String getSpecificNetworkErrorMessage(IOException e) {
        if (e instanceof SocketTimeoutException) {
            return "Connection timed out. Please check your network speed and try again.";
        } else if (e instanceof ConnectException) {
            return "Could not connect to the server. Please ensure the server is running and accessible.";
        } else if (e instanceof UnknownHostException) {
            return "Server address not found. Please check your network connection or server URL.";
        } else {
            return "Network error: " + e.getMessage();
        }
    }

    // Handles clicks on folders, including the special "Bookmarked Notes" folder
    @Override
    public void onFolderClick(Folder folder) {
        Intent intent = new Intent(this, FolderNotesActivity.class);
        if (folder.getId() != null && folder.getId().equals(BOOKMARKED_FOLDER_ID)) {
            intent.putExtra("is_bookmarked_folder", true);
            intent.putExtra("folder_name", "Bookmarked Notes");
        } else {
            intent.putExtra("folder_id", folder.getId());
            intent.putExtra("folder_name", folder.getName());
        }
        startActivity(intent);
    }

    // In FolderAdapter.OnFolderClickListener interface
    @Override
    public void onFolderDelete(Folder folder) {
        // Prevent deletion of the virtual bookmarked folder
        if (folder.getId() != null && folder.getId().equals(BOOKMARKED_FOLDER_ID)) {
            showToast("Cannot delete 'Bookmarked Notes' virtual folder.");
            return;
        }

        showProgress("Deleting Folder...");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Pass the folder ID as Long instead of int
                Response<Void> response = apiService.deleteFolder(folder.getId()).execute();
                runOnUiThread(() -> {
                    dismissProgress();
                    if (response.isSuccessful()) {
                        loadData(searchViewNotes.getQuery().toString()); // Refresh data
                        showToast("Folder deleted");
                    } else {
                        showToast("Failed to delete folder");
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    showToast("Delete failed: " + e.getMessage());
                });
            }
        });
    }

    // Existing Note click handlers
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
                        // If successful, reload all data to ensure consistent UI state
                        // This will fetch the latest bookmark status for all notes
                        loadData(searchViewNotes.getQuery().toString());
                    } else {
                        showToast("Bookmark update failed");
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> showToast("Network error"));
            }
        });
    }

    private void showProgress(String message) {
        runOnUiThread(() -> {
            if (progressDialog == null || !progressDialog.isShowing()) {
                progressDialog = DialogUtils.createProgressDialog(this, message);
                progressDialog.show();
            }
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
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from other activities, maintaining search query if any
        loadData(searchViewNotes.getQuery().toString());
    }

    @Override
    protected void onDestroy() {
        dismissProgress();
        super.onDestroy();
    }
}
