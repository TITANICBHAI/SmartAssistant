package com.aiassistant.dialogs;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.aiassistant.R;
import models.TaskInfo;
import models.TaskPriority;
import models.TaskScheduleType;
import models.TaskStatus;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Dialog for adding or editing a task.
 */
public class AddTaskDialog extends DialogFragment {

    private AddTaskDialogListener listener;
    private TaskInfo existingTask = null;

    // UI components
    private EditText taskNameEditText;
    private EditText taskDescriptionEditText;
    private EditText actionSequenceEditText;
    private EditText intervalEditText;
    private TextInputLayout intervalLayout;
    private Spinner prioritySpinner;
    private Spinner scheduleTypeSpinner;
    private Button selectDateButton;
    private Button selectTimeButton;
    private LinearLayout dateTimeLayout;

    // State
    private Calendar calendar;
    private boolean isEdit = false;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;

    /**
     * Interface for dialog callbacks.
     */
    public interface AddTaskDialogListener {
        void onTaskAdded(TaskInfo task);
        void onTaskUpdated(TaskInfo task);
    }

    /**
     * Default constructor required for DialogFragment.
     */
    public AddTaskDialog() {
        // Required empty constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get task if in edit mode
        Bundle args = getArguments();
        if (args != null && args.containsKey("task")) {
            existingTask = (TaskInfo) args.getSerializable("task");
            isEdit = true;
        }

        // Initialize calendar and date formats
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        timeFormat = new SimpleDateFormat("h:mm a", Locale.US);

        // Set up alert dialog style
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_Dialog);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Create the alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        // Inflate the custom view
        View view = requireActivity().getLayoutInflater()
                .inflate(R.layout.dialog_add_task, null);
        
        // Find views
        taskNameEditText = view.findViewById(R.id.et_task_name);
        taskDescriptionEditText = view.findViewById(R.id.et_task_description);
        actionSequenceEditText = view.findViewById(R.id.et_action_sequence);
        intervalEditText = view.findViewById(R.id.et_interval);
        intervalLayout = view.findViewById(R.id.til_interval);
        prioritySpinner = view.findViewById(R.id.spinner_priority);
        scheduleTypeSpinner = view.findViewById(R.id.spinner_schedule_type);
        selectDateButton = view.findViewById(R.id.btn_select_date);
        selectTimeButton = view.findViewById(R.id.btn_select_time);
        dateTimeLayout = view.findViewById(R.id.layout_date_time);

        // Set up the spinners
        setupSpinners();

        // Set up date and time buttons
        setupDateTimeButtons();

        // If editing, populate fields
        if (isEdit && existingTask != null) {
            populateFields();
        }

        // Set up the dialog
        builder.setView(view)
                .setTitle(isEdit ? R.string.edit_task : R.string.add_new_task)
                .setPositiveButton(isEdit ? R.string.save : R.string.add_task, null) // Set in onStart
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        
        // Override positive button to validate before dismissing
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                if (validateInput()) {
                    saveTask();
                    dialog.dismiss();
                }
            });
        }
    }

    /**
     * Sets the listener for dialog callbacks.
     *
     * @param listener The listener
     */
    public void setListener(AddTaskDialogListener listener) {
        this.listener = listener;
    }

    /**
     * Sets up the spinners and their listeners.
     */
    private void setupSpinners() {
        // Set up schedule type spinner listener
        scheduleTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateScheduleTypeUI(TaskScheduleType.fromSpinnerPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    /**
     * Sets up the date and time selection buttons.
     */
    private void setupDateTimeButtons() {
        // Set up date button
        selectDateButton.setOnClickListener(v -> showDatePicker());
        
        // Set up time button
        selectTimeButton.setOnClickListener(v -> showTimePicker());
        
        // Update button text
        updateDateTimeButtonText();
    }

    /**
     * Shows the date picker dialog.
     */
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateTimeButtonText();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    /**
     * Shows the time picker dialog.
     */
    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    updateDateTimeButtonText();
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    /**
     * Updates the text on the date and time buttons.
     */
    private void updateDateTimeButtonText() {
        selectDateButton.setText(dateFormat.format(calendar.getTime()));
        selectTimeButton.setText(timeFormat.format(calendar.getTime()));
    }

    /**
     * Updates the UI based on the selected schedule type.
     *
     * @param scheduleType The selected schedule type
     */
    private void updateScheduleTypeUI(TaskScheduleType scheduleType) {
        switch (scheduleType) {
            case ONCE:
            case DAILY:
            case WEEKLY:
            case MONTHLY:
                dateTimeLayout.setVisibility(View.VISIBLE);
                intervalLayout.setVisibility(View.GONE);
                break;
                
            case INTERVAL:
                dateTimeLayout.setVisibility(View.GONE);
                intervalLayout.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Populates the fields with existing task data.
     */
    private void populateFields() {
        taskNameEditText.setText(existingTask.getName());
        
        if (!TextUtils.isEmpty(existingTask.getDescription())) {
            taskDescriptionEditText.setText(existingTask.getDescription());
        }
        
        actionSequenceEditText.setText(existingTask.getActionSequence());
        
        // Set priority
        if (existingTask.getPriority() != null) {
            prioritySpinner.setSelection(existingTask.getPriority().toSpinnerPosition());
        }
        
        // Set schedule type
        if (existingTask.getScheduleType() != null) {
            scheduleTypeSpinner.setSelection(existingTask.getScheduleType().toSpinnerPosition());
            
            // Update UI based on schedule type
            updateScheduleTypeUI(existingTask.getScheduleType());
        }
        
        // Set interval if applicable
        if (existingTask.getScheduleType() == TaskScheduleType.INTERVAL) {
            intervalEditText.setText(String.valueOf(existingTask.getIntervalMinutes()));
        }
        
        // Set date and time if applicable
        if (existingTask.getScheduledDate() != null) {
            calendar.setTime(existingTask.getScheduledDate());
            updateDateTimeButtonText();
        }
    }

    /**
     * Validates the input fields.
     *
     * @return True if input is valid, false otherwise
     */
    private boolean validateInput() {
        // Validate task name
        String taskName = taskNameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(taskName)) {
            Toast.makeText(requireContext(), R.string.task_name_required, Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Validate action sequence
        String actionSequence = actionSequenceEditText.getText().toString().trim();
        if (TextUtils.isEmpty(actionSequence)) {
            Toast.makeText(requireContext(), R.string.action_sequence_required, Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Validate interval if applicable
        TaskScheduleType scheduleType = TaskScheduleType.fromSpinnerPosition(
                scheduleTypeSpinner.getSelectedItemPosition());
        
        if (scheduleType == TaskScheduleType.INTERVAL) {
            String intervalStr = intervalEditText.getText().toString().trim();
            if (TextUtils.isEmpty(intervalStr)) {
                Toast.makeText(requireContext(), "Interval is required", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            try {
                int interval = Integer.parseInt(intervalStr);
                if (interval <= 0) {
                    Toast.makeText(requireContext(), "Interval must be greater than 0", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid interval", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        
        return true;
    }

    /**
     * Saves the task data.
     */
    private void saveTask() {
        // Create or update the task
        TaskInfo task = isEdit ? existingTask : new TaskInfo();
        
        // Set basic properties
        task.setName(taskNameEditText.getText().toString().trim());
        task.setDescription(taskDescriptionEditText.getText().toString().trim());
        task.setActionSequence(actionSequenceEditText.getText().toString().trim());
        
        // Set priority
        task.setPriority(TaskPriority.fromSpinnerPosition(prioritySpinner.getSelectedItemPosition()));
        
        // Set schedule type and related properties
        TaskScheduleType scheduleType = TaskScheduleType.fromSpinnerPosition(
                scheduleTypeSpinner.getSelectedItemPosition());
        task.setScheduleType(scheduleType);
        
        if (scheduleType == TaskScheduleType.INTERVAL) {
            int interval = Integer.parseInt(intervalEditText.getText().toString().trim());
            task.setIntervalMinutes(interval);
        } else {
            task.setScheduledDate(calendar.getTime());
            
            // Set additional schedule properties based on type
            if (scheduleType == TaskScheduleType.WEEKLY) {
                task.setDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK));
            } else if (scheduleType == TaskScheduleType.MONTHLY) {
                task.setDayOfMonth(calendar.get(Calendar.DAY_OF_MONTH));
            }
        }
        
        // Notify the listener
        if (listener != null) {
            if (isEdit) {
                listener.onTaskUpdated(task);
            } else {
                listener.onTaskAdded(task);
            }
        }
    }
}