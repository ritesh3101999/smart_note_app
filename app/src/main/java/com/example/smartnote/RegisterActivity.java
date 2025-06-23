package com.example.smartnote;

import android.app.AlertDialog;
import android.app.DatePickerDialog; // Import DatePickerDialog
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smartnote.model.User;
import com.example.smartnote.network.ApiClient;
import com.example.smartnote.network.ApiService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Calendar; // Import Calendar
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import retrofit2.Response;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword, etName, etEmail, etGender, etDob;
    private TextInputLayout tilUsername, tilPassword, tilName, tilEmail, tilGender, tilDob;
    private Button btnRegisterSubmit;
    private ProgressBar progressBar;
    private ApiService apiService;
    private AlertDialog progressDialog;

    private static final String TAG = "RegisterActivity";
    private static final String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
    private static final String DOB_PATTERN = "^\\d{4}-\\d{2}-\\d{2}$"; // YYYY-MM-DD format

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiService = ApiClient.getApiService();
        initializeViews();
        setupInputValidation();
    }

    private void initializeViews() {
        tilUsername = findViewById(R.id.til_reg_username);
        etUsername = findViewById(R.id.et_reg_username);
        tilPassword = findViewById(R.id.til_reg_password);
        etPassword = findViewById(R.id.et_reg_password);
        tilName = findViewById(R.id.til_reg_name);
        etName = findViewById(R.id.et_reg_name);
        tilEmail = findViewById(R.id.til_reg_email);
        etEmail = findViewById(R.id.et_reg_email);
        tilGender = findViewById(R.id.til_reg_gender);
        etGender = findViewById(R.id.et_reg_gender);
        tilDob = findViewById(R.id.til_reg_dob);
        etDob = findViewById(R.id.et_reg_dob);

        btnRegisterSubmit = findViewById(R.id.btn_register_submit);
        progressBar = findViewById(R.id.progress_bar_register);
        Button btnAlreadyHaveAccount = findViewById(R.id.btn_already_have_account);

        btnRegisterSubmit.setOnClickListener(v -> attemptRegistration());
        btnAlreadyHaveAccount.setOnClickListener(v -> finish()); // Go back to LoginActivity

        // Configure Date of Birth EditText to show DatePickerDialog
        etDob.setFocusable(false); // Prevent direct keyboard input
        etDob.setClickable(true); // Make it clickable
        etDob.setOnClickListener(v -> showDatePickerDialog());
    }

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Month is 0-indexed, so add 1
                    String date = String.format(Locale.getDefault(), "%d-%02d-%02d",
                            selectedYear, selectedMonth + 1, selectedDay);
                    etDob.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void setupInputValidation() {
        etUsername.addTextChangedListener(new ValidationTextWatcher(etUsername, tilUsername));
        etPassword.addTextChangedListener(new ValidationTextWatcher(etPassword, tilPassword));
        etName.addTextChangedListener(new ValidationTextWatcher(etName, tilName));
        etEmail.addTextChangedListener(new ValidationTextWatcher(etEmail, tilEmail));
        etGender.addTextChangedListener(new ValidationTextWatcher(etGender, tilGender));
        // No need for a TextWatcher for etDob if it's set via DatePickerDialog
        // but we keep it for consistency with the initial validation check.
        etDob.addTextChangedListener(new ValidationTextWatcher(etDob, tilDob));
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (etUsername.getText().toString().trim().isEmpty()) {
            tilUsername.setError("Username is required");
            isValid = false;
        } else {
            tilUsername.setError(null);
        }

        if (etPassword.getText().toString().trim().isEmpty()) {
            tilPassword.setError("Password is required");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        if (etName.getText().toString().trim().isEmpty()) {
            tilName.setError("Full Name is required");
            isValid = false;
        } else {
            tilName.setError(null);
        }

        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            isValid = false;
        } else if (!Pattern.compile(EMAIL_PATTERN).matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            isValid = false;
        } else {
            tilEmail.setError(null);
        }

        if (etGender.getText().toString().trim().isEmpty()) {
            tilGender.setError("Gender is required");
            isValid = false;
        } else {
            tilGender.setError(null);
        }

        String dob = etDob.getText().toString().trim();
        if (dob.isEmpty()) {
            tilDob.setError("Date of Birth is required");
            isValid = false;
        } else if (!Pattern.compile(DOB_PATTERN).matcher(dob).matches()) {
            tilDob.setError("Format: YYYY-MM-DD");
            isValid = false;
        } else {
            tilDob.setError(null);
        }
        return isValid;
    }

    private void attemptRegistration() {
        if (!validateForm()) {
            showToast("Please correct the errors in the form.");
            return;
        }

        showProgress("Registering...");
        disableForm();

        User user = new User();
        user.setUsername(etUsername.getText().toString().trim());
        user.setPassword(etPassword.getText().toString().trim());
        user.setName(etName.getText().toString().trim());
        user.setEmail(etEmail.getText().toString().trim());
        user.setGender(etGender.getText().toString().trim());
        user.setDateOfBirth(etDob.getText().toString().trim());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Assuming "USER" as the default role for new registrations
                Response<User> response = apiService.createUser(user, "USER").execute();

                runOnUiThread(() -> {
                    dismissProgress();
                    enableForm();
                    if (response.isSuccessful() && response.body() != null) {
                        showToast("Registration successful! Please login.");
                        finish(); // Go back to LoginActivity
                    } else {
                        String errorBody = "";
                        try {
                            if (response.errorBody() != null) {
                                errorBody = response.errorBody().string();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body: " + e.getMessage());
                        }
                        Log.e(TAG, "Registration failed: " + response.code() + " - " + response.message() + " | Error Body: " + errorBody);
                        showToast("Registration failed: " + response.message());
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    enableForm();
                    Log.e(TAG, "Network error during registration", e);
                    showToast("Network error: " + e.getMessage());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    enableForm();
                    Log.e(TAG, "Unexpected error during registration", e);
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

    private void disableForm() {
        etUsername.setEnabled(false);
        etPassword.setEnabled(false);
        etName.setEnabled(false);
        etEmail.setEnabled(false);
        etGender.setEnabled(false);
        etDob.setEnabled(false);
        btnRegisterSubmit.setEnabled(false);
    }

    private void enableForm() {
        etUsername.setEnabled(true);
        etPassword.setEnabled(true);
        etName.setEnabled(true);
        etEmail.setEnabled(true);
        etGender.setEnabled(true);
        etDob.setEnabled(true);
        btnRegisterSubmit.setEnabled(true);
    }

    private class ValidationTextWatcher implements TextWatcher {
        private final TextInputEditText editText;
        private final TextInputLayout textInputLayout;

        private ValidationTextWatcher(TextInputEditText editText, TextInputLayout textInputLayout) {
            this.editText = editText;
            this.textInputLayout = textInputLayout;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Clear error when text changes
            textInputLayout.setError(null);
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Re-validate after text changes to show immediate feedback if still invalid
            if (editText.getId() == R.id.et_reg_username) {
                if (s.toString().trim().isEmpty()) {
                    textInputLayout.setError("Username is required");
                }
            } else if (editText.getId() == R.id.et_reg_password) {
                if (s.toString().trim().isEmpty()) {
                    textInputLayout.setError("Password is required");
                }
            } else if (editText.getId() == R.id.et_reg_name) {
                if (s.toString().trim().isEmpty()) {
                    textInputLayout.setError("Full Name is required");
                }
            } else if (editText.getId() == R.id.et_reg_email) {
                String email = s.toString().trim();
                if (email.isEmpty()) {
                    textInputLayout.setError("Email is required");
                } else if (!Pattern.compile(EMAIL_PATTERN).matcher(email).matches()) {
                    textInputLayout.setError("Enter a valid email address");
                }
            } else if (editText.getId() == R.id.et_reg_gender) {
                if (s.toString().trim().isEmpty()) {
                    textInputLayout.setError("Gender is required");
                }
            } else if (editText.getId() == R.id.et_reg_dob) {
                String dob = s.toString().trim();
                // This validation is primarily for when text is programmatically set by DatePickerDialog
                // or if there's any unexpected direct input. The DatePicker handles formatting.
                if (dob.isEmpty()) {
                    textInputLayout.setError("Date of Birth is required");
                } else if (!Pattern.compile(DOB_PATTERN).matcher(dob).matches()) {
                    textInputLayout.setError("Format: YYYY-MM-DD");
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        dismissProgress();
        super.onDestroy();
    }
}
