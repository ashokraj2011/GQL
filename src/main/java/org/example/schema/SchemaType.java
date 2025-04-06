package org.example.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SchemaType {
    private String name;
    private String namespace;
    private String sourceFile;
    private String apiUrl;
    private List<SchemaField> fields = new ArrayList<>();
    private boolean log;
    private String dbEntity; // Added field for database entity class name

    public SchemaType(String name) {
        this.name = name;
    }

    public void addField(SchemaField field) {
        this.fields.add(field);
    }

    public String getName() {
        return name;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public List<SchemaField> getFields() {
        return fields;
    }

    public boolean isLog() {
        return log;
    }

    public void setLog(boolean log) {
        this.log = log;
    }

    /**
     * Gets the database entity class name for this type
     * 
     * @return The database entity class name, or null if not a DB type
     */
    public String getDbEntity() {
        return dbEntity;
    }
    
    /**
     * Sets the database entity class name for this type
     * 
     * @param dbEntity The database entity class name
     */
    public void setDbEntity(String dbEntity) {
        this.dbEntity = dbEntity;
    }

    /**
     * Finds a field by its name
     * 
     * @param name The field name to look for
     * @return Optional containing the field if found, empty otherwise
     */
    public Optional<SchemaField> getFieldByName(String name) {
        return fields.stream()
            .filter(field -> field.getName().equals(name))
            .findFirst();
    }
    
    /**
     * Check if type has a field with the given name
     * 
     * @param name Field name to check
     * @return true if the field exists, false otherwise
     */
    public boolean hasField(String name) {
        return getFieldByName(name).isPresent();
    }

    @Override
    public String toString() {
        return "SchemaType{" +
                "name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", sourceFile='" + sourceFile + '\'' +
                ", apiUrl='" + apiUrl + '\'' +
                ", log=" + log +
                '}';
    }
}

