package com.aiassistant.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aiassistant.R;
import models.AIMode;

/**
 * AI Fragment - Manages AI mode selection and settings
 */
public class AIFragment extends Fragment {

    private RadioGroup modeRadioGroup;
    private RadioButton autoAIRadio;
    private RadioButton copilotRadio;
    private RadioButton passiveRadio;
    private TextView descriptionText;
    private AIMode currentMode = AIMode.PASSIVE; // Default to passive mode

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai, container, false);
        
        // Initialize UI components
        initViews(view);
        
        // Set up radio group listener
        setupRadioListener();
        
        // Update UI based on current mode
        updateUIForMode(currentMode);
        
        return view;
    }

    private void initViews(View view) {
        modeRadioGroup = view.findViewById(R.id.radio_group_ai_mode);
        autoAIRadio = view.findViewById(R.id.radio_auto_ai);
        copilotRadio = view.findViewById(R.id.radio_copilot);
        passiveRadio = view.findViewById(R.id.radio_passive);
        descriptionText = view.findViewById(R.id.text_mode_description);
    }

    private void setupRadioListener() {
        modeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_auto_ai) {
                changeMode(AIMode.AUTO_AI);
            } else if (checkedId == R.id.radio_copilot) {
                changeMode(AIMode.COPILOT);
            } else if (checkedId == R.id.radio_passive) {
                changeMode(AIMode.PASSIVE);
            }
        });
    }

    /**
     * Change the AI mode and update the UI
     */
    private void changeMode(AIMode newMode) {
        if (newMode != currentMode) {
            currentMode = newMode;
            updateUIForMode(currentMode);
            
            // Show toast message
            String modeName = getModeName(currentMode);
            Toast.makeText(getContext(), 
                    getString(R.string.ai_mode_activated, modeName), 
                    Toast.LENGTH_SHORT).show();
            
            // TODO: Implement actual mode change in AI service
        }
    }

    /**
     * Update the UI based on the selected mode
     */
    private void updateUIForMode(AIMode mode) {
        switch (mode) {
            case AUTO_AI:
                autoAIRadio.setChecked(true);
                descriptionText.setText(R.string.auto_ai_description);
                break;
            case COPILOT:
                copilotRadio.setChecked(true);
                descriptionText.setText(R.string.copilot_description);
                break;
            case PASSIVE:
                passiveRadio.setChecked(true);
                descriptionText.setText(R.string.passive_description);
                break;
        }
    }

    /**
     * Get human-readable name for the AI mode
     */
    private String getModeName(AIMode mode) {
        switch (mode) {
            case AUTO_AI:
                return getString(R.string.auto_ai);
            case COPILOT:
                return getString(R.string.copilot);
            case PASSIVE:
                return getString(R.string.passive);
            default:
                return "";
        }
    }
}