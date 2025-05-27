package com.aiassistant.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aiassistant.R;
import com.aiassistant.adapters.ActiveAppsAdapter;
import com.aiassistant.core.AIController;
import models.AIMode;
import models.AIState;
import models.AppState;

import java.util.ArrayList;
import java.util.List;

/**
 * Home Fragment
 * Shows status information and quick actions
 */
public class HomeFragment extends Fragment {
    private TextView textCurrentMode;
    private TextView textServiceStatus;
    private TextView textLearningStatus;
    private ProgressBar progressBar;
    private RecyclerView recyclerActiveApps;
    private Button buttonToggleAI;
    private Button buttonStartLearning;
    private Button buttonQuickTask;
    
    private AIController aiController;
    private ActiveAppsAdapter appsAdapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize AIController
        aiController = AIController.getInstance(requireContext());
        
        // Initialize views
        initializeViews(view);
        
        // Set up recycler view
        setupRecyclerView();
        
        // Set up button listeners
        setupButtonListeners();
        
        // Update UI with current status
        updateServiceStatus();
    }
    
    private void initializeViews(View view) {
        textCurrentMode = view.findViewById(R.id.text_current_mode);
        textServiceStatus = view.findViewById(R.id.text_service_status);
        textLearningStatus = view.findViewById(R.id.text_learning_status);
        progressBar = view.findViewById(R.id.progress_bar);
        recyclerActiveApps = view.findViewById(R.id.recycler_active_apps);
        buttonToggleAI = view.findViewById(R.id.button_toggle_ai);
        buttonStartLearning = view.findViewById(R.id.button_start_learning);
        buttonQuickTask = view.findViewById(R.id.button_quick_task);
    }
    
    private void setupRecyclerView() {
        recyclerActiveApps.setLayoutManager(new LinearLayoutManager(requireContext()));
        appsAdapter = new ActiveAppsAdapter(new ArrayList<>());
        recyclerActiveApps.setAdapter(appsAdapter);
    }
    
    private void setupButtonListeners() {
        // Toggle AI button
        buttonToggleAI.setOnClickListener(v -> {
            AIState state = aiController.getAIState();
            AIMode newMode;
            
            // Cycle through modes: PASSIVE -> COPILOT -> AUTO_AI -> PASSIVE
            if (state.getAiMode() == AIMode.PASSIVE) {
                newMode = AIMode.COPILOT;
            } else if (state.getAiMode() == AIMode.COPILOT) {
                newMode = AIMode.AUTO_AI;
            } else {
                newMode = AIMode.PASSIVE;
            }
            
            aiController.setAIMode(newMode);
            updateServiceStatus();
        });
        
        // Start Learning button
        buttonStartLearning.setOnClickListener(v -> {
            // For now, just toggle learning state
            AIState state = aiController.getAIState();
            state.setLearningActive(!state.isLearningActive());
            updateServiceStatus();
        });
        
        // Quick Task button
        buttonQuickTask.setOnClickListener(v -> {
            // Open task creation dialog or navigate to tasks fragment
            if (getActivity() != null) {
                // Navigate to the Tasks tab
                if (getActivity() instanceof com.aiassistant.ui.MainActivity) {
                    ((com.aiassistant.ui.MainActivity) getActivity())
                            .getBottomNavigationView().setSelectedItemId(R.id.navigation_task_scheduler);
                }
            }
        });
    }
    
    /**
     * Update the UI to reflect the current service status
     * This is called when the fragment becomes visible or when the status changes
     */
    public void updateServiceStatus() {
        if (aiController == null || !isAdded()) {
            return;
        }
        
        AIState aiState = aiController.getAIState();
        
        // Update mode text
        textCurrentMode.setText("Current Mode: " + getModeString(aiState.getAiMode()));
        
        // Update service status
        boolean serviceEnabled = aiState.isServiceEnabled() && 
                aiState.isAccessibilityEnabled() && 
                aiState.isDeviceAdminEnabled();
        textServiceStatus.setText("Service: " + (serviceEnabled ? "Enabled" : "Disabled"));
        
        // Update learning status
        boolean learningActive = aiState.isLearningActive();
        textLearningStatus.setText("Learning: " + (learningActive ? "Active" : "Inactive"));
        
        // Update progress bar
        int progress = (int)(aiState.getLearningProgress() * 100);
        progressBar.setProgress(progress);
        
        // Update button text based on current status
        updateButtonLabels(aiState);
        
        // Update active apps
        updateActiveApps(aiState.getActiveApps());
    }
    
    private void updateButtonLabels(AIState aiState) {
        // Update AI button text
        switch (aiState.getAiMode()) {
            case PASSIVE:
                buttonToggleAI.setText("Enable AI");
                break;
            case COPILOT:
                buttonToggleAI.setText("Go Autonomous");
                break;
            case AUTO_AI:
                buttonToggleAI.setText("Disable AI");
                break;
        }
        
        // Update learning button text
        buttonStartLearning.setText(aiState.isLearningActive() ? "Stop Learning" : "Start Learning");
    }
    
    private void updateActiveApps(List<AppState> activeApps) {
        if (appsAdapter != null) {
            appsAdapter.updateApps(activeApps);
        }
    }
    
    private String getModeString(AIMode mode) {
        switch (mode) {
            case AUTO_AI:
                return "Autonomous";
            case COPILOT:
                return "Copilot";
            case PASSIVE:
            default:
                return "Passive";
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateServiceStatus();
    }
}