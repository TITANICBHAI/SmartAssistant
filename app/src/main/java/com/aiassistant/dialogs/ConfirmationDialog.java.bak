package com.aiassistant.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * A general confirmation dialog for confirming user actions.
 */
public class ConfirmationDialog extends DialogFragment {
    
    private String title;
    private String message;
    private String positiveButtonText;
    private String negativeButtonText;
    private Runnable onConfirmAction;
    
    /**
     * Default constructor required for DialogFragment.
     */
    public ConfirmationDialog() {
        // Required empty constructor
    }
    
    /**
     * Constructor with all parameters.
     */
    public ConfirmationDialog(String title, String message, String positiveButtonText,
                             String negativeButtonText, Runnable onConfirmAction) {
        this.title = title;
        this.message = message;
        this.positiveButtonText = positiveButtonText;
        this.negativeButtonText = negativeButtonText;
        this.onConfirmAction = onConfirmAction;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        
        // Set title and message if provided
        if (title != null) {
            builder.setTitle(title);
        }
        
        if (message != null) {
            builder.setMessage(message);
        }
        
        // Set positive button (confirm)
        builder.setPositiveButton(positiveButtonText != null ? positiveButtonText : "OK", 
                (dialog, which) -> {
                    dialog.dismiss();
                    if (onConfirmAction != null) {
                        onConfirmAction.run();
                    }
                });
        
        // Set negative button (cancel)
        builder.setNegativeButton(negativeButtonText != null ? negativeButtonText : "Cancel",
                (dialog, which) -> dialog.dismiss());
        
        return builder.create();
    }
}