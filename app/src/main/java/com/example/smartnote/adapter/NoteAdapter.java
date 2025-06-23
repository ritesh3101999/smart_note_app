package com.example.smartnote.adapter;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton; // Import ToggleButton

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartnote.R;
import com.example.smartnote.model.Note;

import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
        // This method is called when the bookmark toggle button is clicked
        void onBookmarkToggle(Note note, boolean isBookmarked);
    }

    private final List<Note> notes = new ArrayList<>();
    private final OnNoteClickListener listener;

    public NoteAdapter(OnNoteClickListener listener) {
        this.listener = listener;
    }

    public void setNotes(List<Note> newNotes) {
        List<Note> oldNotes = new ArrayList<>(notes);
        notes.clear();
        if (newNotes != null) {
            notes.addAll(newNotes);
        }
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new NoteDiffCallback(oldNotes, notes));
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.bind(note, listener);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    // Properly encapsulated ViewHolder
    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView contentTextView;
        private final ToggleButton bookmarkToggle; // Reference to the ToggleButton

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tv_note_title);
            contentTextView = itemView.findViewById(R.id.tv_note_content);
            bookmarkToggle = itemView.findViewById(R.id.tb_bookmark); // Initialize ToggleButton
        }

        void bind(Note note, OnNoteClickListener listener) {
            titleTextView.setText(note.getTitle());

            // Handle HTML content safely
            String content = Html.fromHtml(note.getContent(), Html.FROM_HTML_MODE_COMPACT).toString();
            String preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;
            contentTextView.setText(preview);

            // Set the checked state of the ToggleButton based on the note's bookmarked status
            bookmarkToggle.setChecked(note.isBookmarked());

            // Set listeners for note click and bookmark toggle
            itemView.setOnClickListener(v -> listener.onNoteClick(note));
            bookmarkToggle.setOnClickListener(v -> listener.onBookmarkToggle(note, bookmarkToggle.isChecked()));
        }
    }

    private static class NoteDiffCallback extends DiffUtil.Callback {
        private final List<Note> oldNotes;
        private final List<Note> newNotes;

        public NoteDiffCallback(List<Note> oldNotes, List<Note> newNotes) {
            this.oldNotes = oldNotes;
            this.newNotes = newNotes;
        }

        @Override
        public int getOldListSize() {
            return oldNotes.size();
        }

        @Override
        public int getNewListSize() {
            return newNotes.size();
        }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldNotes.get(oldPos).getId().equals(newNotes.get(newPos).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            Note oldNote = oldNotes.get(oldPos);
            Note newNote = newNotes.get(newPos);
            return oldNote.getTitle().equals(newNote.getTitle()) && oldNote.getContent().equals(newNote.getContent()) && oldNote.isBookmarked() == newNote.isBookmarked();
        }
    }
}
