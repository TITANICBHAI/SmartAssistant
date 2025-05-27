package com.aiassistant.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aiassistant.R;
import com.aiassistant.adapters.TaskAdapter;
import com.aiassistant.dialogs.AddTaskDialog;
import com.aiassistant.dialogs.ConfirmationDialog;
import models.TaskInfo;
import models.TaskStatus;
import com.aiassistant.services.TaskSchedulerService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for task scheduling functionality.
 */
public class TaskSchedulerFragment extends Fragment implements 
        TaskAdapter.TaskAdapterListener, 
        AddTaskDialog.AddTaskDialogListener {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private TextView emptyStateView;
    private FloatingActionButton addTaskButton;
    private TaskSchedulerService taskService;
    private List<TaskInfo> tasks = new ArrayList<>();

    public TaskSchedulerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the task service instance
        taskService = TaskSchedulerService.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_task_scheduler, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_view_tasks);
        emptyStateView = view.findViewById(R.id.tv_empty_state);
        addTaskButton = view.findViewById(R.id.fab_add_task);
        
        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskAdapter(getContext(), this);
        recyclerView.setAdapter(adapter);
        
        // Set up FloatingActionButton click listener
        addTaskButton.setOnClickListener(v -> showAddTaskDialog());
        
        // Load tasks
        loadTasks();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh tasks when the fragment resumes
        loadTasks();
    }

    /**
     * Loads tasks from the task service.
     */
    private void loadTasks() {
        // Get tasks from the service
        tasks = taskService.getAllTasks();
        
        // Update UI based on task data
        if (tasks.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
            adapter.updateTasks(tasks);
        }
    }

    /**
     * Shows the dialog for adding a new task.
     */
    private void showAddTaskDialog() {
        AddTaskDialog dialog = new AddTaskDialog();
        dialog.setListener(this);
        dialog.show(getChildFragmentManager(), "AddTaskDialog");
    }

    /**
     * Shows the dialog for editing an existing task.
     *
     * @param task The task to edit
     */
    private void showEditTaskDialog(TaskInfo task) {
        AddTaskDialog dialog = new AddTaskDialog();
        Bundle args = new Bundle();
        args.putSerializable("task", task);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(getChildFragmentManager(), "EditTaskDialog");
    }

    /**
     * Shows a confirmation dialog for deleting a task.
     *
     * @param task The task to delete
     */
    private void showDeleteConfirmationDialog(TaskInfo task) {
        ConfirmationDialog dialog = new ConfirmationDialog(
                getString(R.string.delete_task_title),
                getString(R.string.delete_task_message),
                getString(R.string.delete),
                getString(R.string.cancel),
                () -> deleteTask(task)
        );
        dialog.show(getChildFragmentManager(), "DeleteTaskDialog");
    }

    /**
     * Deletes a task.
     *
     * @param task The task to delete
     */
    private void deleteTask(TaskInfo task) {
        boolean success = taskService.deleteTask(task.getId());
        if (success) {
            adapter.removeTask(task.getId());
            Toast.makeText(getContext(), R.string.task_deleted, Toast.LENGTH_SHORT).show();
            
            // Check if the list is now empty
            if (adapter.getItemCount() == 0) {
                recyclerView.setVisibility(View.GONE);
                emptyStateView.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(getContext(), R.string.task_delete_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Executes a task.
     *
     * @param task The task to execute
     */
    private void executeTask(TaskInfo task) {
        // Check if the task can be executed
        if (task.getStatus() == TaskStatus.RUNNING) {
            Toast.makeText(getContext(), R.string.task_already_running, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (task.getStatus() == TaskStatus.COMPLETED) {
            Toast.makeText(getContext(), R.string.task_already_completed, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Execute the task
        boolean success = taskService.executeTask(task.getId());
        if (success) {
            // Update task status
            task.setStatus(TaskStatus.RUNNING);
            adapter.updateTask(task);
            Toast.makeText(getContext(), R.string.task_execution_started, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), R.string.task_execution_failed, Toast.LENGTH_SHORT).show();
        }
    }

    // TaskAdapter.TaskAdapterListener implementation

    @Override
    public void onTaskExecuteClick(TaskInfo task) {
        executeTask(task);
    }

    @Override
    public void onTaskEditClick(TaskInfo task) {
        showEditTaskDialog(task);
    }

    @Override
    public void onTaskDeleteClick(TaskInfo task) {
        showDeleteConfirmationDialog(task);
    }

    // AddTaskDialog.AddTaskDialogListener implementation

    @Override
    public void onTaskAdded(TaskInfo task) {
        // Add the task to the service and update UI
        boolean success = taskService.addTask(task);
        if (success) {
            // Show the recycler view and hide empty state
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
            
            // Add the task to the adapter
            adapter.addTask(task);
            
            // Show success message
            Toast.makeText(getContext(), R.string.task_scheduled_success, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), R.string.task_schedule_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onTaskUpdated(TaskInfo task) {
        // Update the task in the service and update UI
        boolean success = taskService.updateTask(task);
        if (success) {
            // Update the task in the adapter
            adapter.updateTask(task);
            
            // Show success message
            Toast.makeText(getContext(), R.string.task_updated_success, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), R.string.task_schedule_error, Toast.LENGTH_SHORT).show();
        }
    }
}