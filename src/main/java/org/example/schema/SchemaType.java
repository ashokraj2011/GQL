package org.example.schema;

import java.util.ArrayList;
import java.util.List;

public class SchemaType {
    private String name;
    private String namespace;
    private String sourceFile;
    private String apiUrl;
    private List<SchemaField> fields = new ArrayList<>();

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

    @Override
    public String toString() {
        return "SchemaType{" +
                "name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", sourceFile='" + sourceFile + '\'' +
                ", apiUrl='" + apiUrl + '\'' +
                '}';
    }
}
