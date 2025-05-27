package com.aiassistant.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aiassistant.R;
import com.aiassistant.core.AIController;
import models.AIMode;
import models.AIState;

/**
 * AI Fragment
 * Controls the AI mode and settings
 */
public class AIFragment extends Fragment {
    private RadioButton radioAutoAI;
    private RadioButton radioCopilot;
    private RadioButton radioPassive;
    private SeekBar seekbarInactivity;
    private TextView textInactivityValue;
    private Button buttonApplySettings;
    private TextView textAIStatus;
    
    private AIController aiController;
    
    // Inactivity times in minutes
    private final int[] inactivityTimes = {1, 2, 3, 5, 10, 15, 20, 30, 45, 60};
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize AIController
        aiController = AIController.getInstance(requireContext());
        
        // Initialize views
        initializeViews(view);
        
        // Set up seekbar listener
        setupSeekbar();
        
        // Set up button listener
        setupButtonListener();
        
        // Update UI with current settings
        updateAIState();
    }
    
    private void initializeViews(View view) {
        radioAutoAI = view.findViewById(R.id.radio_auto_ai);
        radioCopilot = view.findViewById(R.id.radio_copilot);
        radioPassive = view.findViewById(R.id.radio_passive);
        seekbarInactivity = view.findViewById(R.id.seekbar_inactivity);
        textInactivityValue = view.findViewById(R.id.text_inactivity_value);
        buttonApplySettings = view.findViewById(R.id.button_apply_ai_settings);
        textAIStatus = view.findViewById(R.id.text_ai_status);
    }
    
    private void setupSeekbar() {
        // Set initial value to match the default inactivity threshold (2 minutes)
        int defaultIndex = 1; // Index of 2 minutes in inactivityTimes array
        seekbarInactivity.setProgress(defaultIndex);
        updateInactivityText(defaultIndex);
        
        seekbarInactivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateInactivityText(progress);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not needed
            }
        });
    }
    
    private void updateInactivityText(int progress) {
        if (progress < inactivityTimes.length) {
            int minutes = inactivityTimes[progress];
            if (minutes == 1) {
                textInactivityValue.setText(minutes + " minute");
            } else {
                textInactivityValue.setText(minutes + " minutes");
            }
        }
    }
    
    private void setupButtonListener() {
        buttonApplySettings.setOnClickListener(v -> applySettings());
    }
    
    private void applySettings() {
        // Get selected AI mode
        AIMode selectedMode;
        if (radioAutoAI.isChecked()) {
            selectedMode = AIMode.AUTO_AI;
        } else if (radioCopilot.isChecked()) {
            selectedMode = AIMode.COPILOT;
        } else {
            selectedMode = AIMode.PASSIVE;
        }
        
        // Get selected inactivity threshold
        int progress = seekbarInactivity.getProgress();
        int inactivityMinutes = inactivityTimes[progress < inactivityTimes.length ? progress : 0];
        
        // Calculate inactivity threshold in milliseconds
        int inactivityThresholdMs = inactivityMinutes * 60 * 1000;
        
        // Apply settings
        aiController.setInactivityThreshold(inactivityThresholdMs);
        aiController.setAIMode(selectedMode);
        
        // Update UI
        updateAIState();
        
        // Show confirmation
        Toast.makeText(requireContext(), "AI settings applied", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Update the UI to reflect the current AI state
     * This is called when the fragment becomes visible or when settings change
     */
    public void updateAIState() {
        if (aiController == null || !isAdded()) {
            return;
        }
        
        AIState aiState = aiController.getAIState();
        
        // Update mode radio buttons
        switch (aiState.getAiMode()) {
            case AUTO_AI:
                radioAutoAI.setChecked(true);
                break;
            case COPILOT:
                radioCopilot.setChecked(true);
                break;
            case PASSIVE:
                radioPassive.setChecked(true);
                break;
        }
        
        // Update status text
        textAIStatus.setText("Current Mode: " + getModeString(aiState.getAiMode()));
        
        // Update inactivity seekbar to match current setting
        int inactivityMs = aiController.getInactivityThresholdMs();
        int inactivityMinutes = inactivityMs / (60 * 1000);
        
        // Find closest inactivity time in our array
        int closestIndex = 0;
        int minDiff = Integer.MAX_VALUE;
        
        for (int i = 0; i < inactivityTimes.length; i++) {
            int diff = Math.abs(inactivityTimes[i] - inactivityMinutes);
            if (diff < minDiff) {
                minDiff = diff;
                closestIndex = i;
            }
        }
        
        seekbarInactivity.setProgress(closestIndex);
        updateInactivityText(closestIndex);
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
        updateAIState();
    }
}