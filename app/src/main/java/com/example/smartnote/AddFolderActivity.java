package com.example.smartnote;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartnote.model.Folder;
import com.example.smartnote.network.ApiClient;
import com.example.smartnote.network.ApiService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class AddFolderActivity extends AppCompatActivity {

    private TextInputEditText etFolderName;
    private TextInputLayout inputLayoutFolderName;
    private Button btnSave;
    private ProgressBar progressBar;
    private ApiService apiService;
    private OnBackPressedCallback backPressedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_folder);

        apiService = ApiClient.getApiService();
        initializeViews();
        setupInputValidation();
        setupBackPressedCallback();
    }

    private void initializeViews() {
        etFolderName = findViewById(R.id.et_folder_name);
        inputLayoutFolderName = findViewById(R.id.input_layout_folder_name);
        btnSave = findViewById(R.id.btn_save_folder);
        progressBar = findViewById(R.id.progress_bar);

        btnSave.setOnClickListener(v -> createFolder());
    }

    private void setupBackPressedCallback() {
        backPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                // Do nothing when the callback is enabled (during loading)
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    private void setupInputValidation() {
        etFolderName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateInput();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void validateInput() {
        String name = etFolderName.getText().toString().trim();
        boolean isValid = !name.isEmpty();

        btnSave.setEnabled(isValid);
        inputLayoutFolderName.setError(isValid ? null : "Folder name required");
    }

    private void createFolder() {
        String folderName = etFolderName.getText().toString().trim();
        if (folderName.isEmpty()) return;

        showLoading(true);
        disableForm();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Folder folder = new Folder();
                folder.setName(folderName);

                Log.d("FOLDER_CREATE", "Attempting to create folder: " + folderName);
                Log.d("CSRF_CHECK", "CSRF present: " + ApiClient.hasCsrfToken());

                Response<Folder> response = apiService.createFolder(folder).execute();

                if (response.isSuccessful()) {
                    Log.d("FOLDER_SUCCESS", "Created folder ID: " + response.body().getId());
                } else {
                    Log.e("FOLDER_ERROR", "Server response: " + response.code() + " - " + response.message());
                }

                runOnUiThread(() -> handleResponse(response));
            } catch (IOException e) {
                Log.e("NETWORK_ERROR", "Folder creation failed", e);
                runOnUiThread(() -> showError("Network error: " + e.getMessage()));
            } catch (Exception e) {
                Log.e("GENERAL_ERROR", "Unexpected error", e);
                runOnUiThread(() -> showError("Unexpected error occurred"));
            }
        });
    }

    private void handleResponse(Response<Folder> response) {
        showLoading(false);
        enableForm();

        if (response.isSuccessful() && response.body() != null) {
            setResult(RESULT_OK);
            finish();
        } else {
            showError("Failed to create folder");
        }
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setVisibility(loading ? View.GONE : View.VISIBLE);
        backPressedCallback.setEnabled(loading); // Control back press behavior
    }

    private void disableForm() {
        etFolderName.setEnabled(false);
        btnSave.setEnabled(false);
    }

    private void enableForm() {
        etFolderName.setEnabled(true);
        validateInput();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}