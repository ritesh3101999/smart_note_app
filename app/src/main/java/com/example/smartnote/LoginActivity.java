package com.example.smartnote;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartnote.network.ApiClient;
import com.example.smartnote.network.ApiService;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private ApiService apiService;
    private AlertDialog progressDialog;

    private static final String TAG = "LoginActivity"; // For logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize CSRF token before any API calls
        ApiClient.initializeCsrfToken();

        apiService = ApiClient.getApiService();
        initializeViews();
    }

    private void initializeViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);
        Button btnRegister = findViewById(R.id.btn_register);

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showToast("Please fill all fields");
            return;
        }

        showProgress("Authenticating..."); // This will now use DialogUtils

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Response<ResponseBody> response = apiService.login(username, password).execute();
                runOnUiThread(() -> {
                    dismissProgress();
                    if (response.isSuccessful()) {
                        String contentType = response.headers().get("Content-Type");
                        // Check if CSRF token and session cookie are present
                        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
                            // Successful login if backend returns JSON success and cookies are set
                            if (ApiClient.hasCsrfToken() && response.headers().get("Set-Cookie") != null) {
                                Log.d(TAG, "Login successful, CSRF token and session cookie present.");
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Log.w(TAG, "Login response OK, but CSRF token or session cookie missing.");
                                showToast("Authentication failed: Session error. Please try again.");
                                ApiClient.initializeCsrfToken(); // Attempt to re-initialize CSRF
                            }
                        } else {
                            Log.w(TAG, "Login response OK, but unexpected Content-Type: " + contentType);
                            // Handle cases where login is successful but doesn't return JSON
                            // This might indicate a redirect to an HTML page by Spring Security if not configured for API
                            // For now, assume success if cookies are set
                            if (ApiClient.hasCsrfToken() && response.headers().get("Set-Cookie") != null) {
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                showToast("Unexpected server response. Please check server configuration.");
                            }
                        }
                    } else {
                        handleLoginError(response.code(), response.message());
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    Log.e(TAG, "Network error during login", e);
                    showToast("Network error: " + e.getMessage());
                });
            }
        });
    }


    private void handleLoginError(int statusCode, String message) {
        Log.e(TAG, "Login failed. Status: " + statusCode + ", Message: " + message);
        if (statusCode == 401) { // Unauthorized
            showToast("Invalid credentials. Please try again.");
        } else if (statusCode == 403) { // Forbidden (e.g. CSRF issue)
            showToast("Authentication error. Please try again.");
            ApiClient.initializeCsrfToken(); // Refresh CSRF token as it might be the cause
        } else {
            showToast("Login failed: " + (message != null && !message.isEmpty() ? message : "Unknown error"));
        }
    }

    private void showProgress(String message) {
        runOnUiThread(() -> {
            if (progressDialog == null || !progressDialog.isShowing()) {
                // Use DialogUtils for a styled progress dialog
                progressDialog = DialogUtils.createProgressDialog(LoginActivity.this, message);
            }
            // Ensure activity is not finishing before showing dialog
            if (!isFinishing() && !progressDialog.isShowing()) {
                progressDialog.show();
            }
        });
    }

    private void dismissProgress() {
        runOnUiThread(() -> {
            // Ensure activity is not finishing and dialog is valid and showing
            if (!isFinishing() && progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        });
    }

    private void showToast(String message) {
        runOnUiThread(() ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    protected void onDestroy() {
        dismissProgress(); // Dismiss dialog to prevent leaks
        super.onDestroy();
    }
}
