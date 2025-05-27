package com.aiassistant.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.aiassistant.R;

/**
 * Settings Fragment - Manages application settings
 */
public class SettingsFragment extends Fragment {

    private CardView accessibilityCard;
    private CardView aiSettingsCard;
    private CardView privacySettingsCard;
    private CardView storageSettingsCard;
    private CardView aboutCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        // Initialize UI components
        initViews(view);
        
        // Set up click listeners
        setupClickListeners();
        
        return view;
    }

    private void initViews(View view) {
        accessibilityCard = view.findViewById(R.id.card_accessibility_settings);
        aiSettingsCard = view.findViewById(R.id.card_ai_settings);
        privacySettingsCard = view.findViewById(R.id.card_privacy_settings);
        storageSettingsCard = view.findViewById(R.id.card_storage_settings);
        aboutCard = view.findViewById(R.id.card_about);
    }

    private void setupClickListeners() {
        // Accessibility settings - opens system accessibility settings
        accessibilityCard.setOnClickListener(v -> openAccessibilitySettings());
        
        // AI settings
        aiSettingsCard.setOnClickListener(v -> {
            // Will implement advanced AI settings in future iteration
            Toast.makeText(getContext(), "AI Settings (Coming Soon)", Toast.LENGTH_SHORT).show();
        });
        
        // Privacy settings
        privacySettingsCard.setOnClickListener(v -> {
            // Will implement privacy settings in future iteration
            Toast.makeText(getContext(), "Privacy Settings (Coming Soon)", Toast.LENGTH_SHORT).show();
        });
        
        // Storage settings
        storageSettingsCard.setOnClickListener(v -> {
            // Will implement storage settings in future iteration
            Toast.makeText(getContext(), "Storage Settings (Coming Soon)", Toast.LENGTH_SHORT).show();
        });
        
        // About
        aboutCard.setOnClickListener(v -> {
            // Will implement about page in future iteration
            Toast.makeText(getContext(), "About (Coming Soon)", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Open system accessibility settings to allow user to enable our service
     */
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        Toast.makeText(getContext(), R.string.accessibility_settings, Toast.LENGTH_LONG).show();
    }
}