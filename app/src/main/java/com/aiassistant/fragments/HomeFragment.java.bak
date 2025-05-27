package com.aiassistant.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.aiassistant.R;

/**
 * Home Fragment - Main dashboard for the AI Assistant app.
 */
public class HomeFragment extends Fragment {

    private CardView aiModeCard;
    private CardView taskSchedulerCard;
    private CardView learningCard;
    private CardView settingsCard;
    private TextView statusText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Initialize UI components
        initViews(view);
        
        // Set up card click listeners for quick navigation
        setupClickListeners();
        
        // Update status display
        updateStatusDisplay();
        
        return view;
    }

    private void initViews(View view) {
        // Initialize card views for quick navigation
        aiModeCard = view.findViewById(R.id.card_ai_mode);
        taskSchedulerCard = view.findViewById(R.id.card_task_scheduler);
        learningCard = view.findViewById(R.id.card_learning);
        settingsCard = view.findViewById(R.id.card_settings);
        
        // Status text view
        statusText = view.findViewById(R.id.text_ai_status);
    }

    private void setupClickListeners() {
        // Set up click listeners for cards to navigate to other tabs
        aiModeCard.setOnClickListener(v -> navigateToTab(R.id.navigation_ai));
        taskSchedulerCard.setOnClickListener(v -> navigateToTab(R.id.navigation_task_scheduler));
        learningCard.setOnClickListener(v -> navigateToTab(R.id.navigation_learning));
        settingsCard.setOnClickListener(v -> navigateToTab(R.id.navigation_settings));
    }

    private void navigateToTab(int tabId) {
        // Navigate to the selected bottom navigation tab
        if (getActivity() != null) {
            getActivity().findViewById(R.id.navigation).findViewById(tabId).performClick();
        }
    }

    private void updateStatusDisplay() {
        // TODO: Update with actual AI status information
        // This will be updated with actual AI state once we implement the backend
        statusText.setText("AI Assistant: Passive mode - Observing and learning");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh status display when fragment resumes
        updateStatusDisplay();
    }
}