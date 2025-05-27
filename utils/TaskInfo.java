package utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Class representing task information
 * Contains metadata about a task including its type, status, and properties
 */
public class TaskInfo {
    /**
     * Task status values
     */
    public enum TaskStatus {
        CREATED,
        PENDING,
        RUNNING,
        PAUSED,
        COMPLETED,
        FAILED,
        CANCELLED;
        
        public boolean isTerminal() {
            return this == COMPLETED || this == FAILED || this == CANCELLED;
        }
        
        public boolean isActive() {
            return this == RUNNING || this == PAUSED;
        }
    }
    
    /**
     * Task priority levels
     */
    public enum TaskPriority {
        LOWEST(0),
        LOW(1),
        NORMAL(2),
        HIGH(3),
        HIGHEST(4),
        CRITICAL(5);
        
        private final int value;
        
        TaskPriority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static TaskPriority fromValue(int value) {
            for (TaskPriority priority : values()) {
                if (priority.getValue() == value) {
                    return priority;
                }
            }
            return NORMAL;
        }
    }
    
    /**
     * Task trigger types
     */
    public enum TaskTriggerType {
        MANUAL,
        SCHEDULED,
        EVENT_DRIVEN,
        CONDITION_BASED,
        DEPENDENCY_COMPLETION,
        PERIODIC,
        SYSTEM
    }
    
    private String taskId;
    private String taskName;
    private String taskType;
    private TaskStatus status;
    private TaskPriority priority;
    private TaskTriggerType triggerType;
    private Date createdAt;
    private Date startedAt;
    private Date completedAt;
    private Date deadlineAt;
    private int progress;
    private String errorMessage;
    private Map<String, Object> parameters;
    private Map<String, Object> results;
    
    /**
     * Default constructor
     */
    public TaskInfo() {
        this.taskId = UUID.randomUUID().toString();
        this.taskName = "";
        this.taskType = "";
        this.status = TaskStatus.CREATED;
        this.priority = TaskPriority.NORMAL;
        this.triggerType = TaskTriggerType.MANUAL;
        this.createdAt = new Date();
        this.progress = 0;
        this.parameters = new HashMap<>();
        this.results = new HashMap<>();
    }
    
    /**
     * Constructor with task name and type
     * 
     * @param taskName Task name
     * @param taskType Task type
     */
    public TaskInfo(String taskName, String taskType) {
        this();
        this.taskName = taskName;
        this.taskType = taskType;
    }
    
    /**
     * Get task ID
     * 
     * @return Task ID
     */
    public String getTaskId() {
        return taskId;
    }
    
    /**
     * Set task ID
     * 
     * @param taskId Task ID to set
     */
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    /**
     * Get task name
     * 
     * @return Task name
     */
    public String getTaskName() {
        return taskName;
    }
    
    /**
     * Set task name
     * 
     * @param taskName Task name to set
     */
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    /**
     * Get task type
     * 
     * @return Task type
     */
    public String getTaskType() {
        return taskType;
    }
    
    /**
     * Set task type
     * 
     * @param taskType Task type to set
     */
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
    
    /**
     * Get task status
     * 
     * @return Task status
     */
    public TaskStatus getStatus() {
        return status;
    }
    
    /**
     * Set task status
     * 
     * @param status Task status to set
     */
    public void setStatus(TaskStatus status) {
        this.status = status;
        
        // Update timestamps based on status changes
        if (status == TaskStatus.RUNNING && this.startedAt == null) {
            this.startedAt = new Date();
        } else if (status.isTerminal() && this.completedAt == null) {
            this.completedAt = new Date();
        }
    }
    
    /**
     * Get task priority
     * 
     * @return Task priority
     */
    public TaskPriority getPriority() {
        return priority;
    }
    
    /**
     * Set task priority
     * 
     * @param priority Task priority to set
     */
    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }
    
    /**
     * Get trigger type
     * 
     * @return Trigger type
     */
    public TaskTriggerType getTriggerType() {
        return triggerType;
    }
    
    /**
     * Set trigger type
     * 
     * @param triggerType Trigger type to set
     */
    public void setTriggerType(TaskTriggerType triggerType) {
        this.triggerType = triggerType;
    }
    
    /**
     * Get created date
     * 
     * @return Created date
     */
    public Date getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Set created date
     * 
     * @param createdAt Created date to set
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Get started date
     * 
     * @return Started date
     */
    public Date getStartedAt() {
        return startedAt;
    }
    
    /**
     * Set started date
     * 
     * @param startedAt Started date to set
     */
    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }
    
    /**
     * Get completed date
     * 
     * @return Completed date
     */
    public Date getCompletedAt() {
        return completedAt;
    }
    
    /**
     * Set completed date
     * 
     * @param completedAt Completed date to set
     */
    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }
    
    /**
     * Get deadline date
     * 
     * @return Deadline date
     */
    public Date getDeadlineAt() {
        return deadlineAt;
    }
    
    /**
     * Set deadline date
     * 
     * @param deadlineAt Deadline date to set
     */
    public void setDeadlineAt(Date deadlineAt) {
        this.deadlineAt = deadlineAt;
    }
    
    /**
     * Get progress percentage
     * 
     * @return Progress percentage (0-100)
     */
    public int getProgress() {
        return progress;
    }
    
    /**
     * Set progress percentage
     * 
     * @param progress Progress percentage to set (0-100)
     */
    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        
        // If progress reaches 100, update status if not already terminal
        if (this.progress == 100 && !this.status.isTerminal()) {
            this.status = TaskStatus.COMPLETED;
            this.completedAt = new Date();
        }
    }
    
    /**
     * Get error message
     * 
     * @return Error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Set error message
     * 
     * @param errorMessage Error message to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        
        // If an error message is set, update status if not already terminal
        if (errorMessage != null && !errorMessage.isEmpty() && !this.status.isTerminal()) {
            this.status = TaskStatus.FAILED;
            this.completedAt = new Date();
        }
    }
    
    /**
     * Get task parameters
     * 
     * @return Parameters map
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    /**
     * Set task parameters
     * 
     * @param parameters Parameters map to set
     */
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }
    
    /**
     * Get a parameter value
     * 
     * @param key Parameter key
     * @return Parameter value or null if not found
     */
    public Object getParameter(String key) {
        return parameters.get(key);
    }
    
    /**
     * Set a parameter value
     * 
     * @param key Parameter key
     * @param value Parameter value
     */
    public void setParameter(String key, Object value) {
        if (this.parameters == null) {
            this.parameters = new HashMap<>();
        }
        this.parameters.put(key, value);
    }
    
    /**
     * Get task results
     * 
     * @return Results map
     */
    public Map<String, Object> getResults() {
        return results;
    }
    
    /**
     * Set task results
     * 
     * @param results Results map to set
     */
    public void setResults(Map<String, Object> results) {
        this.results = results != null ? results : new HashMap<>();
    }
    
    /**
     * Get a result value
     * 
     * @param key Result key
     * @return Result value or null if not found
     */
    public Object getResult(String key) {
        return results.get(key);
    }
    
    /**
     * Set a result value
     * 
     * @param key Result key
     * @param value Result value
     */
    public void setResult(String key, Object value) {
        if (this.results == null) {
            this.results = new HashMap<>();
        }
        this.results.put(key, value);
    }
    
    /**
     * Check if the task is active
     * 
     * @return True if the task is active
     */
    public boolean isActive() {
        return this.status.isActive();
    }
    
    /**
     * Check if the task is completed
     * 
     * @return True if the task is completed
     */
    public boolean isCompleted() {
        return this.status == TaskStatus.COMPLETED;
    }
    
    /**
     * Check if the task has failed
     * 
     * @return True if the task has failed
     */
    public boolean isFailed() {
        return this.status == TaskStatus.FAILED;
    }
    
    /**
     * Check if the task is cancelled
     * 
     * @return True if the task is cancelled
     */
    public boolean isCancelled() {
        return this.status == TaskStatus.CANCELLED;
    }
    
    /**
     * Check if the task is terminal (completed, failed, or cancelled)
     * 
     * @return True if the task is terminal
     */
    public boolean isTerminal() {
        return this.status.isTerminal();
    }
    
    /**
     * Check if the task is past deadline
     * 
     * @return True if the task is past deadline
     */
    public boolean isPastDeadline() {
        return this.deadlineAt != null && new Date().after(this.deadlineAt);
    }
    
    /**
     * Get task duration in milliseconds
     * 
     * @return Task duration or -1 if not started
     */
    public long getDuration() {
        if (this.startedAt == null) {
            return -1;
        }
        
        Date endTime = this.completedAt != null ? this.completedAt : new Date();
        return endTime.getTime() - this.startedAt.getTime();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TaskInfo{");
        sb.append("taskId='").append(taskId).append('\'');
        sb.append(", taskName='").append(taskName).append('\'');
        sb.append(", taskType='").append(taskType).append('\'');
        sb.append(", status=").append(status);
        sb.append(", priority=").append(priority);
        sb.append(", progress=").append(progress).append('%');
        sb.append('}');
        return sb.toString();
    }
}