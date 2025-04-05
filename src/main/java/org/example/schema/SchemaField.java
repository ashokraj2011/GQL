package org.example.schema;

/**
 * Represents a field in a GraphQL schema type.
 */
public class SchemaField {
    private String name;
    private String type;
    private boolean required;
    private boolean isList;
    private boolean isScalar;

    /**
     * Creates a new schema field
     * 
     * @param name Field name
     * @param type Field type (may include ! for required and [] for lists)
     * @param required Whether the field is required
     * @param isList Whether the field is a list
     * @param isScalar Whether the field is a scalar type
     */
    public SchemaField(String name, String type, boolean required, boolean isList, boolean isScalar) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.isList = isList;
        this.isScalar = isScalar;
    }

    /**
     * Gets the field name
     * 
     * @return Field name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the field type
     * 
     * @return Field type (may include ! for required and [] for lists)
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the clean type name without modifiers (!, [])
     * 
     * @return The clean type name
     */
    public String getCleanType() {
        return type
            .replace("[", "")
            .replace("]", "")
            .replace("!", "");
    }

    /**
     * Checks if the field is required
     * 
     * @return true if the field is required, false otherwise
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Checks if the field is a list
     * 
     * @return true if the field is a list, false otherwise
     */
    public boolean isList() {
        return isList;
    }

    /**
     * Checks if the field is a scalar type
     * 
     * @return true if the field is a scalar type, false otherwise
     */
    public boolean isScalar() {
        return isScalar;
    }

    @Override
    public String toString() {
        return "SchemaField{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", required=" + required +
                ", isList=" + isList +
                ", isScalar=" + isScalar +
                '}';
    }
}

