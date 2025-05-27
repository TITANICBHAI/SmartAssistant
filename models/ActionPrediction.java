package models;

/**
 * Represents a predicted action and its associated confidence and reasoning.
 * This class is part of the PredictiveActionSystem and is used to communicate
 * action predictions to clients.
 */
public class ActionPrediction {
    private String action;
    private double confidence;
    private String reasoning;
    
    /**
     * Create a new action prediction
     * 
     * @param action Action name
     * @param confidence Confidence level (0.0 to 1.0)
     * @param reasoning Reasoning or explanation for this prediction
     */
    public ActionPrediction(String action, double confidence, String reasoning) {
        this.action = action;
        this.confidence = confidence;
        this.reasoning = reasoning;
    }
    
    /**
     * Create a new action prediction with default confidence
     * 
     * @param action Action name
     */
    public ActionPrediction(String action) {
        this(action, 0.5, "Default prediction");
    }
    
    /**
     * Default constructor (for serialization)
     */
    public ActionPrediction() {
        this("no_action", 0.0, "Empty prediction");
    }
    
    /**
     * Get the action name
     * 
     * @return Action name
     */
    public String getAction() {
        return action;
    }
    
    /**
     * Set the action name
     * 
     * @param action New action name
     */
    public void setAction(String action) {
        this.action = action;
    }
    
    /**
     * Get the confidence level
     * 
     * @return Confidence level (0.0 to 1.0)
     */
    public double getConfidence() {
        return confidence;
    }
    
    /**
     * Set the confidence level
     * 
     * @param confidence New confidence level (0.0 to 1.0)
     */
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    /**
     * Get the reasoning or explanation
     * 
     * @return Reasoning text
     */
    public String getReasoning() {
        return reasoning;
    }
    
    /**
     * Set the reasoning or explanation
     * 
     * @param reasoning New reasoning text
     */
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    /**
     * Add additional reasoning to the existing reasoning
     * 
     * @param additionalReasoning Additional reasoning to append
     */
    public void addReasoning(String additionalReasoning) {
        if (reasoning == null || reasoning.isEmpty()) {
            reasoning = additionalReasoning;
        } else {
            reasoning += "; " + additionalReasoning;
        }
    }
    
    /**
     * Convert the prediction to a string representation
     */
    @Override
    public String toString() {
        return "ActionPrediction{action='" + action + "', confidence=" + confidence + 
               ", reasoning='" + reasoning + "'}";
    }
}