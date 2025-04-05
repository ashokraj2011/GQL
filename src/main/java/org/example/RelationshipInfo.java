package org.example;

/**
 * Relationship information class for tracking entity connections.
 */
public class RelationshipInfo {
    private final String sourceType;
    private final String fieldName;
    private final String targetType;
    private final boolean isList;
    
    public RelationshipInfo(String sourceType, String fieldName, String targetType, boolean isList) {
        this.sourceType = sourceType;
        this.fieldName = fieldName;
        this.targetType = targetType;
        this.isList = isList;
    }
    
    public String getSourceType() {
        return sourceType;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public String getTargetType() {
        return targetType;
    }
    
    public boolean isList() {
        return isList;
    }
}
