package org.example.schema;

public class SchemaField {
    private String name;
    private String type;
    private boolean required;
    private boolean isList;
    private boolean isScalar;

    // Constructor
    public SchemaField(String name, String type, boolean required, boolean isList, boolean isScalar) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.isList = isList;
        this.isScalar = isScalar;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isList() {
        return isList;
    }

    public void setList(boolean list) {
        isList = list;
    }

    public boolean isScalar() {
        return isScalar;
    }

    public void setScalar(boolean scalar) {
        isScalar = scalar;
    }
}
