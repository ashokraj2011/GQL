package org.example;

/**
 * Contains information about relationships between types in the schema.
 * Maps to the RelationshipInfo type in the GraphQL schema.
 */
public class RelationshipInfo {
    private String sourceType;
    private String fieldName;
    private String targetType;
    private boolean isList;
    
    // Default constructor
    public RelationshipInfo() {
    }
    
    // Full constructor
    public RelationshipInfo(String sourceType, String fieldName, String targetType, boolean isList) {
        this.sourceType = sourceType;
        this.fieldName = fieldName;
        this.targetType = targetType;
        this.isList = isList;
    }
    
    // Getters and setters
    public String getSourceType() {
        return sourceType;
    }
    
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    
    public String getTargetType() {
        return targetType;
    }
    
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }
    
    public boolean isList() {
        return isList;
    }
    
    public void setList(boolean isList) {
        this.isList = isList;
    }
    
    @Override
    public String toString() {
        return "RelationshipInfo{" +
                "sourceType='" + sourceType + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", targetType='" + targetType + '\'' +
                ", isList=" + isList +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        RelationshipInfo that = (RelationshipInfo) o;
        
        if (isList != that.isList) return false;
        if (!sourceType.equals(that.sourceType)) return false;
        if (!fieldName.equals(that.fieldName)) return false;
        return targetType.equals(that.targetType);
    }
    
    @Override
    public int hashCode() {
        int result = sourceType.hashCode();
        result = 31 * result + fieldName.hashCode();
        result = 31 * result + targetType.hashCode();
        result = 31 * result + (isList ? 1 : 0);
        return result;
    }
}
