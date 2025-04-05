package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.fasterxml.jackson.core.JsonParser;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class GQL {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<Map<String, Object>>> dataStore = new HashMap<>();
    private final Map<String, List<String>> schemaStore = new HashMap<>();
    private final Map<String, String> directiveStore = new HashMap<>();
    private final Map<String, List<String>> namespaceTypeMapping = new HashMap<>();
    private final List<SchemaType> schemaTypes = new ArrayList<>();
    private final Map<String, List<SchemaField>> typeFields = new HashMap<>();

    public static void main(String[] args) {
        SpringApplication.run(GQL.class, args);
    }

    public GQL() throws Exception {
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        loadSchemaFile();
        loadDataSource();
        
        System.out.println("Loaded data store keys: " + dataStore.keySet());
        System.out.println("Namespace mappings: " + namespaceTypeMapping);
    }

    private void loadSchemaFile() throws Exception {
        System.out.println("Loading schema file...");
        
        File schemaFile = new File("schema.graphql");
        if (!schemaFile.exists()) {
            System.out.println("Schema file not found at: " + schemaFile.getAbsolutePath());
            return;
        }
        
        List<String> lines = Files.readAllLines(Paths.get("schema.graphql"));
        SchemaType currentType = null;
        boolean inTypeDefinition = false;
        String currentNamespace = null;
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            if (line.startsWith("directive @")) {
                continue;
            }
            
            if (line.startsWith("type ")) {
                String typeDef = line.substring(5).trim();
                String typeName = typeDef.split(" ")[0].trim();
                
                if (typeName.equals("Query")) {
                    inTypeDefinition = true;
                    continue;
                }
                
                if (typeName.equals("Marketing") || typeName.equals("Finance") || typeName.equals("ExternalAPI")) {
                    currentNamespace = typeName;
                    currentType = new SchemaType(typeName);
                    schemaTypes.add(currentType);
                    
                    namespaceTypeMapping.putIfAbsent(currentNamespace, new ArrayList<>());
                    
                    if (line.contains("@params")) {
                    }
                    
                    inTypeDefinition = true;
                    System.out.println("Found namespace: " + currentNamespace);
                } 
                else if (!inTypeDefinition || line.contains("{")) {
                    SchemaType entityType = new SchemaType(typeName);
                    
                    if (line.contains("@source")) {
                        int startIdx = line.indexOf("@source(file: \"") + 16;
                        int endIdx = line.indexOf("\")", startIdx);
                        if (startIdx > 0 && endIdx > 0) {
                            String filePath = line.substring(startIdx, endIdx);
                            
                            String directiveKey = currentNamespace != null && 
                                    !typeName.startsWith(currentNamespace) ? 
                                    currentNamespace + typeName : typeName;
                            
                            directiveStore.put(directiveKey, filePath);
                            entityType.setSourceFile(filePath);
                            
                            System.out.println("Added directive for: " + directiveKey + " -> " + filePath);
                        }
                    }
                    else if (line.contains("@api")) {
                        int startIdx = line.indexOf("@api(url: \"") + 11;
                        int endIdx = line.indexOf("\")", startIdx);
                        if (startIdx > 0 && endIdx > 0) {
                            String apiUrl = line.substring(startIdx, endIdx);
                            
                            String directiveKey = currentNamespace != null ? 
                                    currentNamespace + typeName + ".api" : typeName + ".api";
                            
                            directiveStore.put(directiveKey, apiUrl);
                            entityType.setApiUrl(apiUrl);
                            
                            System.out.println("Added API directive for: " + directiveKey + " -> " + apiUrl);
                        }
                    }
                    
                    if (currentNamespace != null) {
                        namespaceTypeMapping.computeIfAbsent(currentNamespace, k -> new ArrayList<>()).add(typeName);
                        entityType.setNamespace(currentNamespace);
                    }
                    
                    schemaTypes.add(entityType);
                    inTypeDefinition = true;
                    currentType = entityType;
                    
                    typeFields.put(typeName, new ArrayList<>());
                }
            }
            else if (inTypeDefinition && line.contains(":")) {
                int colonIndex = line.indexOf(':');
                if (colonIndex > 0 && currentType != null) {
                    String fieldName = line.substring(0, colonIndex).trim();
                    String fieldType = line.substring(colonIndex + 1).trim();
                    
                    if (fieldType.endsWith(",")) {
                        fieldType = fieldType.substring(0, fieldType.length() - 1).trim();
                    }
                    
                    SchemaField field = new SchemaField(fieldName, fieldType);
                    currentType.addField(field);
                    
                    typeFields.computeIfAbsent(currentType.getName(), k -> new ArrayList<>()).add(field);
                }
            }
            else if (inTypeDefinition && line.equals("}")) {
                inTypeDefinition = false;
                currentType = null;
            }
        }
        
        System.out.println("Schema loading complete. Directives: " + directiveStore.size());
        System.out.println("Schema types: " + schemaTypes.size());
        System.out.println("Type fields mapped: " + typeFields.size());
    }

    private void loadDataSource() throws Exception {
        String sourceType = System.getProperty("source.type", "file");
        switch (sourceType.toLowerCase()) {
            case "db":
                break;
            case "api":
                break;
            default:
                loadDataFiles();
                break;
        }
        loadAPIData();
    }

    private void loadAPIData() throws Exception {
        for (String key : directiveStore.keySet()) {
            if (key.endsWith(".api")) {
                String typeName = key.substring(0, key.length() - 4);
                String apiUrl = directiveStore.get(key);
                System.out.println("Loading API data for " + typeName + " from " + apiUrl);
                
                try {
                    List<Map<String, Object>> sampleData = new ArrayList<>();
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", "api-1");
                    item.put("name", "API User");
                    item.put("email", "user@example.com");
                    sampleData.add(item);
                    
                    dataStore.put(typeName, sampleData);
                    
                    if (typeName.contains(".")) {
                        String simpleName = typeName.substring(typeName.lastIndexOf(".") + 1);
                        dataStore.put(simpleName, sampleData);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error loading API data for " + typeName + ": " + e.getMessage());
                }
            }
        }
    }

    private void loadDataFiles() throws Exception {
        File dataDir = new File("data");
        if (dataDir.exists() && dataDir.isDirectory()) {
            File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    String typeName = file.getName().replaceFirst("\\.json$", "");
                    typeName = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);
                    List<Map<String, Object>> data = objectMapper.readValue(file, List.class);
                    dataStore.put(typeName, data);
                    System.out.println("Loaded default data for: " + typeName + " from " + file.getName());
                    
                    dataStore.put(file.getName().replaceFirst("\\.json$", ""), data);
                }
            }
        }

        for (String typeName : directiveStore.keySet()) {
            String customFile = directiveStore.get(typeName);
            if (customFile != null && !customFile.isEmpty() && customFile.endsWith(".json")) {
                File file = new File(customFile);
                if (file.exists()) {
                    List<Map<String, Object>> data = objectMapper.readValue(file, List.class);
                    dataStore.put(typeName, data);
                    System.out.println("Loaded directive data for: " + typeName + " from " + customFile);
                    
                    if (typeName.contains(".")) {
                        String[] parts = typeName.split("\\.");
                        String namespace = parts[0];
                        String type = parts[parts.length - 1];
                        
                        namespaceTypeMapping.computeIfAbsent(namespace, k -> new ArrayList<>()).add(type);
                        
                        dataStore.put(type, data);
                        
                        String fileName = customFile.substring(customFile.lastIndexOf("/")+1);
                        fileName = fileName.replaceFirst("\\.json$", "");
                        dataStore.put(fileName, data);
                        
                        System.out.println("Added alias mapping: " + type + " -> " + typeName + " -> " + fileName);
                    }
                }
            }
        }
    }

    @PostMapping("/query")
    public Map<String, Object> handleQuery(@RequestBody Map<String, Object> body) {
        Map<String, Object> query = (Map<String, Object>) body.get("query");
        Map<String, Object> result = new HashMap<>();

        System.out.println("Processing query: " + query);
        
        if (query.containsKey("metadata")) {
            System.out.println("Processing metadata query");
            Map<String, Object> metadataQuery = (Map<String, Object>) query.get("metadata");
            List<String> fields = (List<String>) metadataQuery.get("fields");
            
            Map<String, Object> metadata = getSchemaMetadata();
            Map<String, Object> metadataData = (Map<String, Object>) metadata.get("data");
            Map<String, Object> metadataContent = (Map<String, Object>) metadataData.get("metadata");
            
            if (fields != null && !fields.isEmpty()) {
                Map<String, Object> filteredMetadata = new HashMap<>();
                for (String field : fields) {
                    if (metadataContent.containsKey(field)) {
                        filteredMetadata.put(field, metadataContent.get(field));
                    }
                }
                result.put("metadata", filteredMetadata);
            } else {
                result.put("metadata", metadataContent);
            }
            
            return Map.of("data", result);
        }

        if (query.containsKey("collection")) {
            String collection = (String) query.get("collection");
            List<String> fields = (List<String>) query.get("fields");
            Map<String, Object> where = (Map<String, Object>) query.get("where");

            List<Map<String, Object>> data = findDataForCollection(collection, null);

            List<Map<String, Object>> filteredData = data.stream()
                .filter(row -> where == null || where.entrySet().stream()
                        .allMatch(e -> e.getValue().equals(row.get(e.getKey()))))
                .map(row -> row.entrySet().stream()
                        .filter(e -> fields == null || fields.isEmpty() || fields.contains(e.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .collect(Collectors.toList());

            if (where != null && !filteredData.isEmpty()) {
                result.put(collection, filteredData.get(0));
            } else {
                result.put(collection, filteredData);
            }

            return Map.of("data", result);
        }
        
        for (String namespace : query.keySet()) {
            Object value = query.get(namespace);
            System.out.println("Processing namespace: " + namespace + " with value: " + value);
            
            if (value instanceof Map) {
                Map<String, Object> nestedCollections = (Map<String, Object>) value;
                Map<String, Object> namespaceResult = new HashMap<>();
                
                for (String collection : nestedCollections.keySet()) {
                    System.out.println("Processing nested collection: " + namespace + "." + collection);
                    
                    if (!(nestedCollections.get(collection) instanceof Map)) {
                        continue;
                    }
                    
                    Map<String, Object> queryDetails = (Map<String, Object>) nestedCollections.get(collection);
                    List<String> fields = (List<String>) queryDetails.get("fields");
                    Map<String, Object> where = (Map<String, Object>) queryDetails.get("where");
                    
                    List<Map<String, Object>> data = findDataForCollection(collection, namespace);
                    System.out.println("Retrieved data for " + namespace + "." + collection + ": " + data.size() + " records");
                    
                    List<Map<String, Object>> filteredData = data.stream()
                        .filter(row -> where == null || where.entrySet().stream()
                                .allMatch(e -> e.getValue().equals(row.get(e.getKey()))))
                        .map(row -> row.entrySet().stream()
                                .filter(e -> fields == null || fields.isEmpty() || fields.contains(e.getKey()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                        .collect(Collectors.toList());
                    
                    if (where != null && !filteredData.isEmpty()) {
                        namespaceResult.put(collection, filteredData.get(0));
                    } else {
                        namespaceResult.put(collection, filteredData);
                    }
                }
                
                result.put(namespace, namespaceResult);
            } else {
                Map<String, Object> queryDetails = (Map<String, Object>) query.get(namespace);
                List<String> fields = (List<String>) queryDetails.get("fields");
                Map<String, Object> where = (Map<String, Object>) queryDetails.get("where");
                
                List<Map<String, Object>> data = findDataForCollection(namespace, null);
                
                Optional<Map<String, Object>> match = data.stream()
                        .filter(row -> where == null || where.entrySet().stream()
                                .allMatch(e -> e.getValue().equals(row.get(e.getKey()))))
                        .findFirst();

                result.put(namespace, match.map(row -> row.entrySet().stream()
                                .filter(e -> fields.contains(e.getKey()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                        .orElse(null));
            }
        }

        System.out.println("Query result: " + result);
        return Map.of("data", result);
    }

    private List<Map<String, Object>> findDataForCollection(String collection, String namespace) {
        System.out.println("Finding data for collection: " + collection + " in namespace: " + namespace);
        
        if (namespace != null) {
            String fullName = namespace + "." + collection;
            String filePrefix = null;
            
            for (String key : directiveStore.keySet()) {
                if (key.equalsIgnoreCase(fullName)) {
                    String filePath = directiveStore.get(key);
                    if (filePath != null && filePath.contains("/") && filePath.endsWith(".json")) {
                        String fileName = filePath.substring(filePath.lastIndexOf("/")+1).replace(".json", "");
                        System.out.println("Found directive file name: " + fileName + " for " + fullName);
                        
                        if (dataStore.containsKey(fileName)) {
                            System.out.println("Found data using file name key: " + fileName);
                            return dataStore.get(fileName);
                        }
                        
                        if (fileName.contains(".")) {
                            filePrefix = fileName;
                        }
                    }
                }
            }
            
            if (filePrefix != null) {
                System.out.println("Looking for data with file prefix: " + filePrefix);
                if (dataStore.containsKey(filePrefix)) {
                    return dataStore.get(filePrefix);
                }
            }
            
            String inferredPrefix = namespace.toLowerCase() + "." + collection.toLowerCase();
            String marketingPrefix = "marketing." + collection.toLowerCase();
            String financePrefix = "finance." + collection.toLowerCase();
            
            if (namespace.equalsIgnoreCase("Marketing") && dataStore.containsKey(marketingPrefix)) {
                System.out.println("Found Marketing data using marketing prefix: " + marketingPrefix);
                return dataStore.get(marketingPrefix);
            } else if (namespace.equalsIgnoreCase("finance") && dataStore.containsKey(financePrefix)) {
                System.out.println("Found finance data using finance prefix: " + financePrefix);
                return dataStore.get(financePrefix);
            }
            
            if (dataStore.containsKey(inferredPrefix)) {
                System.out.println("Found data using inferred key: " + inferredPrefix);
                return dataStore.get(inferredPrefix);
            }
            
            for (String key : dataStore.keySet()) {
                if (key.toLowerCase().equals(inferredPrefix)) {
                    System.out.println("Found case-insensitive match: " + key);
                    return dataStore.get(key);
                }
            }
            
            if (dataStore.containsKey(fullName)) {
                System.out.println("Found fully qualified match for: " + fullName);
                return dataStore.get(fullName);
            }
            
            for (String key : dataStore.keySet()) {
                if (key.equalsIgnoreCase(fullName)) {
                    System.out.println("Found case-insensitive fully qualified match for: " + key);
                    return dataStore.get(key);
                }
            }
        }
        
        if (dataStore.containsKey(collection)) {
            System.out.println("Found direct match for: " + collection);
            return dataStore.get(collection);
        }
        
        for (String key : dataStore.keySet()) {
            if (key.equalsIgnoreCase(collection)) {
                System.out.println("Found case-insensitive match for: " + key);
                return dataStore.get(key);
            }
        }
        
        System.out.println("No data found for collection: " + collection + " in namespace: " + namespace);
        return new ArrayList<>();
    }

    @GetMapping("/debug/datastore")
    public Map<String, Object> getDataStoreDebug() {
        Map<String, Object> debug = new HashMap<>();
        debug.put("dataStoreKeys", dataStore.keySet());
        debug.put("namespaceTypeMapping", namespaceTypeMapping);
        debug.put("directiveStore", directiveStore);
        
        Map<String, Object> dataSamples = new HashMap<>();
        for (String key : dataStore.keySet()) {
            List<Map<String, Object>> data = dataStore.get(key);
            if (data != null && !data.isEmpty()) {
                dataSamples.put(key, Map.of(
                    "count", data.size(),
                    "sample", data.size() > 0 ? data.get(0) : "empty"
                ));
            } else {
                dataSamples.put(key, "empty");
            }
        }
        debug.put("dataSamples", dataSamples);
        
        return debug;
    }

    @GetMapping("/debug/schema")
    public Map<String, Object> getSchemaDebug() {
        Map<String, Object> debug = new HashMap<>();
        debug.put("schemaTypes", schemaTypes.stream().map(type -> {
            Map<String, Object> typeMap = new HashMap<>();
            typeMap.put("name", type.getName());
            typeMap.put("namespace", type.getNamespace());
            typeMap.put("sourceFile", type.getSourceFile());
            typeMap.put("apiUrl", type.getApiUrl());
            return typeMap;
        }).collect(Collectors.toList()));
        debug.put("directiveStore", directiveStore);
        debug.put("namespaceTypeMapping", namespaceTypeMapping);
        return debug;
    }

    @GetMapping("/metadata")
    public Map<String, Object> getSchemaMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        
        Map<String, Object> types = new HashMap<>();
        for (SchemaType type : schemaTypes) {
            types.put(type.getName(), buildTypeMetadata(type));
        }
        metadata.put("types", types);
        
        metadata.put("namespaces", namespaceTypeMapping);
        
        metadata.put("directives", directiveStore);
        
        metadata.put("relationships", buildRelationshipMetadata());
        
        return Map.of("data", Map.of("metadata", metadata));
    }
    
    private Map<String, Object> buildTypeMetadata(SchemaType type) {
        Map<String, Object> typeMetadata = new HashMap<>();
        typeMetadata.put("name", type.getName());
        typeMetadata.put("namespace", type.getNamespace());
        
        Map<String, Object> source = new HashMap<>();
        if (type.getSourceFile() != null) {
            source.put("type", "file");
            source.put("path", type.getSourceFile());
        } else if (type.getApiUrl() != null) {
            source.put("type", "api");
            source.put("url", type.getApiUrl());
        } else {
            source.put("type", "unknown");
        }
        typeMetadata.put("source", source);
        
        List<Map<String, Object>> fields = new ArrayList<>();
        List<SchemaField> typeFieldsList = typeFields.getOrDefault(type.getName(), type.getFields());
        
        for (SchemaField field : typeFieldsList) {
            Map<String, Object> fieldInfo = new HashMap<>();
            fieldInfo.put("name", field.name);
            fieldInfo.put("type", field.type);
            fieldInfo.put("required", field.type.endsWith("!"));
            fieldInfo.put("isList", field.type.startsWith("[") && field.type.endsWith("]"));
            
            String baseType = field.type
                .replace("[", "")
                .replace("]", "")
                .replace("!", "");
            fieldInfo.put("baseType", baseType);
            
            boolean isScalar = baseType.equals("ID") || 
                               baseType.equals("String") || 
                               baseType.equals("Int") || 
                               baseType.equals("Float") || 
                               baseType.equals("Boolean");
            fieldInfo.put("isScalar", isScalar);
            
            fields.add(fieldInfo);
        }
        typeMetadata.put("fields", fields);
        
        return typeMetadata;
    }
    
    private Map<String, Object> buildRelationshipMetadata() {
        Map<String, Object> relationships = new HashMap<>();
        
        for (SchemaType type : schemaTypes) {
            List<Map<String, Object>> typeRelations = new ArrayList<>();
            
            List<SchemaField> fields = typeFields.getOrDefault(type.getName(), type.getFields());
            for (SchemaField field : fields) {
                String fieldType = field.type
                    .replace("[", "")
                    .replace("]", "")
                    .replace("!", "");
                
                if (fieldType.equals("ID") || fieldType.equals("String") || 
                    fieldType.equals("Int") || fieldType.equals("Float") || 
                    fieldType.equals("Boolean")) {
                    continue;
                }
                
                Map<String, Object> relation = new HashMap<>();
                relation.put("fieldName", field.name);
                relation.put("targetType", fieldType);
                relation.put("isList", field.type.startsWith("[") && field.type.endsWith("]"));
                
                if (field.name.endsWith("Id") || field.name.endsWith("ID")) {
                    String possibleRefType = field.name.substring(0, field.name.length() - 2);
                    relation.put("possibleJoinField", possibleRefType);
                }
                
                typeRelations.add(relation);
            }
            
            if (!typeRelations.isEmpty()) {
                relationships.put(type.getName(), typeRelations);
            }
        }
        
        return relationships;
    }

    private static class SchemaType {
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
    
    private static class SchemaField {
        private String name;
        private String type;
        
        public SchemaField(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
}
