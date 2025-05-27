package com.aiassistant.adapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;

import models.LearningSource;

import java.util.List;

/**
 * Adapter for the learning sources view pager
 */
public class LearningPagerAdapter extends PagerAdapter {
    
    private final Context context;
    private final List<LearningSource> learningSources;
    private final OnSourceClickListener listener;
    
    /**
     * Interface for learning source click events
     */
    public interface OnSourceClickListener {
        void onSourceClick(LearningSource source);
        void onSourceEnableToggle(LearningSource source, boolean enabled);
        void onSourceSettingsClick(LearningSource source);
    }
    
    /**
     * Constructor
     * 
     * @param context Android context
     * @param learningSources List of learning sources
     * @param listener Click listener
     */
    public LearningPagerAdapter(Context context, List<LearningSource> learningSources, OnSourceClickListener listener) {
        this.context = context;
        this.learningSources = learningSources;
        this.listener = listener;
    }
    
    @Override
    public int getCount() {
        return learningSources != null ? learningSources.size() : 0;
    }
    
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(android.R.layout.simple_list_item_2, container, false);
        
        // Get learning source for this position
        final LearningSource source = learningSources.get(position);
        
        // Setup views
        TextView titleView = view.findViewById(android.R.id.text1);
        TextView descriptionView = view.findViewById(android.R.id.text2);
        
        // Set data
        if (titleView != null) {
            titleView.setText(source.getName());
        }
        
        if (descriptionView != null) {
            descriptionView.setText(source.getDescription());
        }
        
        // Set click listener
        view.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSourceClick(source);
            }
        });
        
        // Add to container
        container.addView(view);
        return view;
    }
    
    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
    
    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }
    
    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        LearningSource source = learningSources.get(position);
        return source != null ? source.getName() : "";
    }
    
    /**
     * Get learning source at position
     * 
     * @param position Position
     * @return Learning source
     */
    public LearningSource getLearningSource(int position) {
        if (position >= 0 && position < learningSources.size()) {
            return learningSources.get(position);
        }
        return null;
    }
    
    /**
     * Update learning sources
     * 
     * @param newSources New learning sources
     */
    public void updateSources(List<LearningSource> newSources) {
        this.learningSources.clear();
        this.learningSources.addAll(newSources);
        notifyDataSetChanged();
    }
    
    /**
     * Static class for the fragment-based implementation
     */
    public static class FragmentImpl extends FragmentPagerAdapter {
        
        private final List<Fragment> fragments;
        private final List<String> titles;
        
        /**
         * Constructor
         * 
         * @param fm Fragment manager
         * @param fragments List of fragments
         * @param titles List of titles
         */
        public FragmentImpl(FragmentManager fm, List<Fragment> fragments, List<String> titles) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.fragments = fragments;
            this.titles = titles;
        }
        
        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }
        
        @Override
        public int getCount() {
            return fragments.size();
        }
        
        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }
    }
}