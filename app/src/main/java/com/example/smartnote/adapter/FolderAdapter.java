package com.example.smartnote.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartnote.R;
import com.example.smartnote.model.Folder;

import java.util.ArrayList;
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private List<Folder> folders = new ArrayList<>();
    private final OnFolderClickListener listener;

    public interface OnFolderClickListener {
        void onFolderClick(Folder folder);
        void onFolderDelete(Folder folder);
    }

    public FolderAdapter(OnFolderClickListener listener) {
        this.listener = listener;
    }

    public void setFolders(List<Folder> newFolders) {
        folders.clear();
        folders.addAll(newFolders);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        Folder folder = folders.get(position);
        holder.bind(folder, listener);
    }

    @Override
    public int getItemCount() { return folders.size(); }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvFolderName;
        private final ImageButton btnDelete;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFolderName = itemView.findViewById(R.id.tv_folder_name);
            btnDelete = itemView.findViewById(R.id.btn_delete_folder);
        }

        void bind(Folder folder, OnFolderClickListener listener) {
            tvFolderName.setText(folder.getName());

            itemView.setOnClickListener(v -> listener.onFolderClick(folder));
            btnDelete.setOnClickListener(v -> listener.onFolderDelete(folder));
        }
    }
}