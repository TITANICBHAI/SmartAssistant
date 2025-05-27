package com.aiassistant;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.aiassistant.core.AIController;
import models.AIState;
import com.aiassistant.services.AIService;
import com.aiassistant.ui.fragments.AIFragment;
import com.aiassistant.ui.fragments.HomeFragment;
import com.aiassistant.ui.fragments.LearningFragment;
import com.aiassistant.ui.fragments.SettingsFragment;
import com.aiassistant.ui.fragments.TasksFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Main activity for the Self Learning AI Assistant app.
 * Handles navigation between fragments and app initialization.
 */
public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    
    private NavController navController;
    private BottomNavigationView navigationView;
    private AIController aiController;
    
    // Request codes
    private static final int REQUEST_ACCESSIBILITY = 1001;
    private static final int REQUEST_VIDEO_PICKER = 1002;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize AIController
        aiController = AIController.getInstance(this);
        
        // Initialize navigation
        navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(this);
        
        // Set up the Navigation Controller with the Bottom Navigation View
        navController = Navigation.findNavController(this, R.id.fragment_container);
        NavigationUI.setupWithNavController(navigationView, navController);
        
        // Set up App Bar configuration
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, 
                R.id.navigation_ai,
                R.id.navigation_learning,
                R.id.navigation_task_scheduler,
                R.id.navigation_settings
        ).build();
        
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        
        // Start AI service
        startAIService();
        
        // Check permissions and show welcome dialog
        checkPermissionsAndShowWelcome();
    }
    
    private void startAIService() {
        Intent serviceIntent = new Intent(this, AIService.class);
        startService(serviceIntent);
    }
    
    private void checkPermissionsAndShowWelcome() {
        AIState aiState = aiController.getAIState();
        
        // Check if this is first launch
        if (!aiState.isFirstLaunchCompleted()) {
            showWelcomeDialog();
            aiState.setFirstLaunchCompleted(true);
        }
        
        // Check accessibility service
        if (!aiState.isAccessibilityEnabled()) {
            showAccessibilityDialog();
        }
    }
    
    private void showWelcomeDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Welcome to AI Assistant")
                .setMessage("This app uses advanced AI to help automate tasks on your device. " +
                        "You'll need to grant several permissions for full functionality.")
                .setPositiveButton("Get Started", (dialog, which) -> {
                    // Navigate to settings
                    navigationView.setSelectedItemId(R.id.navigation_settings);
                })
                .setNegativeButton("Later", null)
                .show();
    }
    
    private void showAccessibilityDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Accessibility Service Required")
                .setMessage("This app requires Accessibility Service to function properly. " +
                        "Please enable it in Settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    openAccessibilitySettings();
                })
                .setNegativeButton("Later", null)
                .show();
    }
    
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(intent, REQUEST_ACCESSIBILITY);
    }
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation item selection
        int itemId = item.getItemId();
        
        if (itemId == R.id.navigation_home) {
            navController.navigate(R.id.navigation_home);
            return true;
        } else if (itemId == R.id.navigation_ai) {
            navController.navigate(R.id.navigation_ai);
            return true;
        } else if (itemId == R.id.navigation_learning) {
            navController.navigate(R.id.navigation_learning);
            return true;
        } else if (itemId == R.id.navigation_task_scheduler) {
            navController.navigate(R.id.navigation_task_scheduler);
            return true;
        } else if (itemId == R.id.navigation_settings) {
            navController.navigate(R.id.navigation_settings);
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, (AppBarConfiguration) null) 
                || super.onSupportNavigateUp();
    }
    
    /**
     * Open the video picker for selecting videos to learn from
     */
    public void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a Video"), REQUEST_VIDEO_PICKER);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No file manager found", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Get the bottom navigation view for use by fragments
     */
    public BottomNavigationView getBottomNavigationView() {
        return navigationView;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        switch (requestCode) {
            case REQUEST_ACCESSIBILITY:
                // Check if accessibility service is now enabled
                AIState aiState = aiController.getAIState();
                boolean accessibilityEnabled = aiController.checkAccessibilityServiceEnabled();
                aiState.setAccessibilityEnabled(accessibilityEnabled);
                
                if (accessibilityEnabled) {
                    Toast.makeText(this, "Accessibility service enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Accessibility service not enabled", Toast.LENGTH_SHORT).show();
                }
                break;
                
            case REQUEST_VIDEO_PICKER:
                if (resultCode == RESULT_OK && data != null) {
                    Uri videoUri = data.getData();
                    
                    // Navigate to learning fragment
                    navigationView.setSelectedItemId(R.id.navigation_learning);
                    
                    // Pass the video URI to the learning fragment
                    // (This would be implemented via a shared ViewModel in a production app)
                }
                break;
        }
    }
}