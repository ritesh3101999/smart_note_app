package com.example.smartnote;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;

public class DialogUtils {
    public static AlertDialog createProgressDialog(@NonNull Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null);

        TextView messageText = view.findViewById(R.id.progress_text);
        ProgressBar progressBar = view.findViewById(R.id.progress_bar);

        messageText.setText(message);
        builder.setView(view);
        builder.setCancelable(false);

        return builder.create();
    }
}