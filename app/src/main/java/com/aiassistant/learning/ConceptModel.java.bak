package com.aiassistant.learning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a conceptual model learned by the AI.
 * This class stores information about abstract concepts and their relationships
 * that the AI has learned through observation and analysis.
 */
public class ConceptModel {
    
    private String id;
    private String name;
    private String description;
    private Map<String, Object> attributes;
    private Map<String, Float> relationships;
    private List<String> relatedConcepts;
    private float confidence;
    private String source;
    private long createdTimestamp;
    private long updatedTimestamp;
    private boolean validated;
    
    /**
     * Default constructor
     */
    public ConceptModel() {
        this.id = UUID.randomUUID().toString();
        this.attributes = new HashMap<>();
        this.relationships = new HashMap<>();
        this.relatedConcepts = new ArrayList<>();
        this.confidence = 0.0f;
        this.createdTimestamp = System.currentTimeMillis();
        this.updatedTimestamp = this.createdTimestamp;
        this.validated = false;
    }
    
    /**
     * Constructor with name and description
     * 
     * @param name Concept name
     * @param description Concept description
     */
    public ConceptModel(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }
    
    /**
     * Get concept ID
     * 
     * @return Concept ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set concept ID
     * 
     * @param id Concept ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get concept name
     * 
     * @return Concept name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set concept name
     * 
     * @param name Concept name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get concept description
     * 
     * @return Concept description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set concept description
     * 
     * @param description Concept description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get concept attributes
     * 
     * @return Concept attributes
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    /**
     * Set concept attributes
     * 
     * @param attributes Concept attributes
     */
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
    
    /**
     * Add or update an attribute
     * 
     * @param key Attribute key
     * @param value Attribute value
     */
    public void setAttribute(String key, Object value) {
        if (this.attributes == null) {
            this.attributes = new HashMap<>();
        }
        this.attributes.put(key, value);
        this.updatedTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Get concept relationships
     * 
     * @return Concept relationships
     */
    public Map<String, Float> getRelationships() {
        return relationships;
    }
    
    /**
     * Set concept relationships
     * 
     * @param relationships Concept relationships
     */
    public void setRelationships(Map<String, Float> relationships) {
        this.relationships = relationships;
    }
    
    /**
     * Add or update a relationship
     * 
     * @param conceptId Related concept ID
     * @param strength Relationship strength (0.0-1.0)
     */
    public void setRelationship(String conceptId, float strength) {
        if (this.relationships == null) {
            this.relationships = new HashMap<>();
        }
        this.relationships.put(conceptId, strength);
        if (!this.relatedConcepts.contains(conceptId)) {
            this.relatedConcepts.add(conceptId);
        }
        this.updatedTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Get related concepts
     * 
     * @return List of related concept IDs
     */
    public List<String> getRelatedConcepts() {
        return relatedConcepts;
    }
    
    /**
     * Set related concepts
     * 
     * @param relatedConcepts List of related concept IDs
     */
    public void setRelatedConcepts(List<String> relatedConcepts) {
        this.relatedConcepts = relatedConcepts;
    }
    
    /**
     * Add a related concept
     * 
     * @param conceptId Related concept ID
     */
    public void addRelatedConcept(String conceptId) {
        if (this.relatedConcepts == null) {
            this.relatedConcepts = new ArrayList<>();
        }
        if (!this.relatedConcepts.contains(conceptId)) {
            this.relatedConcepts.add(conceptId);
        }
        this.updatedTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Get confidence score
     * 
     * @return Confidence score
     */
    public float getConfidence() {
        return confidence;
    }
    
    /**
     * Set confidence score
     * 
     * @param confidence Confidence score
     */
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    /**
     * Get concept source
     * 
     * @return Concept source
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Set concept source
     * 
     * @param source Concept source
     */
    public void setSource(String source) {
        this.source = source;
    }
    
    /**
     * Get creation timestamp
     * 
     * @return Creation timestamp
     */
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    /**
     * Set creation timestamp
     * 
     * @param createdTimestamp Creation timestamp
     */
    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
    
    /**
     * Get last update timestamp
     * 
     * @return Last update timestamp
     */
    public long getUpdatedTimestamp() {
        return updatedTimestamp;
    }
    
    /**
     * Set last update timestamp
     * 
     * @param updatedTimestamp Last update timestamp
     */
    public void setUpdatedTimestamp(long updatedTimestamp) {
        this.updatedTimestamp = updatedTimestamp;
    }
    
    /**
     * Check if concept is validated
     * 
     * @return true if validated
     */
    public boolean isValidated() {
        return validated;
    }
    
    /**
     * Set validated status
     * 
     * @param validated Validated status
     */
    public void setValidated(boolean validated) {
        this.validated = validated;
    }
}