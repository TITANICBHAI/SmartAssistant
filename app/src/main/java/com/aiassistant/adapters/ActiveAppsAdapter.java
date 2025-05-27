package com.aiassistant.adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Context;

import models.AppInfo;
import models.GameType;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying active apps in a RecyclerView
 */
public class ActiveAppsAdapter extends RecyclerView.Adapter<ActiveAppsAdapter.ViewHolder> {

    private final List<AppInfo> apps = new ArrayList<>();
    private final Context context;
    private final OnAppClickListener listener;

    /**
     * Interface for app click events
     */
    public interface OnAppClickListener {
        void onAppClick(AppInfo app, int position);
        void onAppLongClick(AppInfo app, int position, View view);
    }

    /**
     * Create a new ActiveAppsAdapter
     * 
     * @param context Application context
     * @param listener Click listener
     */
    public ActiveAppsAdapter(Context context, OnAppClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /**
     * Get the app icon as an ImageView.
     * 
     * @param position The position in the list.
     * @return The icon ImageView for the app at the given position.
     */
    @Nullable
    public ImageView getIcon(int position) {
        if (position < 0 || position >= apps.size()) {
            return null;
        }
        
        // This method would be implemented with a reference to the ViewHolder,
        // but for demonstration we're just returning null
        return null;
    }

    /**
     * Get the usage count for an app.
     * 
     * @param position The position in the list.
     * @return The usage count for the app at the given position.
     */
    public int getUsageCount(int position) {
        if (position < 0 || position >= apps.size()) {
            return 0;
        }
        
        AppInfo app = apps.get(position);
        return app.getUsageCount();
    }

    /**
     * Get the confidence score for an app.
     * 
     * @param position The position in the list.
     * @return The confidence score for the app at the given position.
     */
    public float getConfidenceScore(int position) {
        if (position < 0 || position >= apps.size()) {
            return 0.0f;
        }
        
        AppInfo app = apps.get(position);
        return app.getConfidenceScore();
    }

    /**
     * Check if the app is a game app.
     * 
     * @param position The position in the list.
     * @return True if the app is a game app, false otherwise.
     */
    public boolean isGameApp(int position) {
        if (position < 0 || position >= apps.size()) {
            return false;
        }
        
        AppInfo app = apps.get(position);
        GameType gameType = GameType.fromPackageName(app.getPackageName());
        return gameType != GameType.NONE;
    }

    /**
     * Check if the app has learning data.
     * 
     * @param position The position in the list.
     * @return True if the app has learning data, false otherwise.
     */
    public boolean isLearned(int position) {
        if (position < 0 || position >= apps.size()) {
            return false;
        }
        
        AppInfo app = apps.get(position);
        return app.isLearned();
    }

    /**
     * Add an app to the adapter.
     * 
     * @param app App to add
     */
    public void addApp(AppInfo app) {
        if (app != null) {
            apps.add(app);
            notifyItemInserted(apps.size() - 1);
        }
    }

    /**
     * Set the list of apps.
     * 
     * @param appList List of apps
     */
    public void setApps(List<AppInfo> appList) {
        this.apps.clear();
        if (appList != null) {
            this.apps.addAll(appList);
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
        AppInfo app = apps.get(position);
        holder.title.setText(app.getAppName());
        holder.subtitle.setText(app.getPackageName());
        
        // Set app icon if available
        if (app.getAppIcon() != null) {
            holder.icon.setImageDrawable(app.getAppIcon());
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppClick(app, holder.getAdapterPosition());
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onAppLongClick(app, holder.getAdapterPosition(), v);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    /**
     * ViewHolder for app items
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView title;
        public final TextView subtitle;
        public final ImageView icon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(android.R.id.text1);
            subtitle = itemView.findViewById(android.R.id.text2);
            icon = itemView.findViewById(android.R.id.icon);
        }
    }
}