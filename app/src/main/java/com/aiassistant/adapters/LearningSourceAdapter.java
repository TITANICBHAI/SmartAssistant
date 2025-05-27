package com.aiassistant.adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Context;

import models.LearningSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying learning sources in a RecyclerView
 */
public class LearningSourceAdapter extends RecyclerView.Adapter<LearningSourceAdapter.ViewHolder> {

    private final List<LearningSource> sources = new ArrayList<>();
    private final Context context;
    private OnSourceClickListener listener;
    private OnLearningSourceClickListener learningSourceClickListener;

    /**
     * Interface for source click events
     */
    public interface OnSourceClickListener {
        void onSourceClick(LearningSource source, int position);
        void onSourceLongClick(LearningSource source, int position, View view);
    }
    
    /**
     * Interface for learning source click events
     */
    public interface OnLearningSourceClickListener {
        void onLearningSourceClick(LearningSource source);
        void onLearningSourceLongClick(LearningSource source, int position);
    }

    /**
     * Create a new LearningSourceAdapter
     * 
     * @param context Application context
     * @param listener Click listener
     */
    public LearningSourceAdapter(Context context, OnSourceClickListener listener) {
        this.context = context;
        this.listener = listener;
    }
    
    /**
     * Create a new LearningSourceAdapter
     * 
     * @param context Application context
     * @param sources List of learning sources
     */
    public LearningSourceAdapter(Context context, List<LearningSource> sources) {
        this.context = context;
        if (sources != null) {
            this.sources.addAll(sources);
        }
    }
    
    /**
     * Set the learning source click listener
     * 
     * @param listener The listener to set
     */
    public void setOnLearningSourceClickListener(OnLearningSourceClickListener listener) {
        this.learningSourceClickListener = listener;
    }

    /**
     * Get the icon resource ID for a learning source.
     * 
     * @param sourceId The ID of the learning source.
     * @return The resource ID for the icon.
     */
    public int getIconResourceId(String sourceId) {
        // This would be implemented to return the actual icon resource ID
        return android.R.drawable.ic_menu_info_details;
    }

    /**
     * Get the learning progress for a source.
     * 
     * @param position The position in the list.
     * @return The learning progress (0-100).
     */
    public int getProgress(int position) {
        if (position < 0 || position >= sources.size()) {
            return 0;
        }
        
        LearningSource source = sources.get(position);
        return source.getProgress();
    }

    /**
     * Set the progress for a learning source.
     * 
     * @param position The position in the list.
     * @param progress The progress value (0-100).
     */
    public void setProgress(int position, int progress) {
        if (position >= 0 && position < sources.size()) {
            LearningSource source = sources.get(position);
            source.setProgress(progress);
            notifyItemChanged(position);
        }
    }

    /**
     * Add a learning source to the adapter.
     * 
     * @param source Learning source to add
     */
    public void addSource(LearningSource source) {
        if (source != null) {
            sources.add(source);
            notifyItemInserted(sources.size() - 1);
        }
    }
    
    /**
     * Add a learning source to the adapter.
     * This is an alias for addSource for compatibility.
     * 
     * @param source Learning source to add
     */
    public void addLearningSource(LearningSource source) {
        addSource(source);
    }
    
    /**
     * Remove a learning source at the specified position.
     * 
     * @param position Position of the source to remove
     * @return The removed learning source, or null if position is invalid
     */
    public LearningSource removeLearningSource(int position) {
        if (position >= 0 && position < sources.size()) {
            LearningSource removed = sources.remove(position);
            notifyItemRemoved(position);
            return removed;
        }
        return null;
    }

    /**
     * Set the list of learning sources.
     * 
     * @param sourceList List of learning sources
     */
    public void setSources(List<LearningSource> sourceList) {
        this.sources.clear();
        if (sourceList != null) {
            this.sources.addAll(sourceList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LearningSource source = sources.get(position);
        holder.title.setText(source.getName());
        holder.subtitle.setText(source.getDescription());
        
        // Set icon
        int iconResId = getIconResourceId(source.getId());
        if (iconResId != 0) {
            holder.icon.setImageResource(iconResId);
        }
        
        // Set progress
        int progress = source.getProgress();
        if (holder.progressBar != null) {
            holder.progressBar.setProgress(progress);
            holder.progressBar.setVisibility(progress > 0 ? View.VISIBLE : View.GONE);
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSourceClick(source, holder.getAdapterPosition());
            }
            
            if (learningSourceClickListener != null) {
                learningSourceClickListener.onLearningSourceClick(source);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onSourceLongClick(source, holder.getAdapterPosition(), v);
                return true;
            }
            
            if (learningSourceClickListener != null) {
                learningSourceClickListener.onLearningSourceLongClick(source, holder.getAdapterPosition());
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return sources.size();
    }

    /**
     * ViewHolder for learning source items
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView title;
        public final TextView subtitle;
        public final ImageView icon;
        @Nullable
        public final ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(android.R.id.text1);
            subtitle = itemView.findViewById(android.R.id.text2);
            icon = itemView.findViewById(android.R.id.icon);
            progressBar = itemView.findViewById(android.R.id.progress);
        }
    }
}