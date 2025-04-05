package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonParser;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.example.PermissionCheck;

@SpringBootApplication
@RestController
@RequestMapping("/api")
@EnableWebSocketMessageBroker
public class GQL implements WebSocketMessageBrokerConfigurer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<Map<String, Object>>> dataStore = new HashMap<>();
    private final Map<String, List<String>> schemaStore = new HashMap<>();
    private final Map<String, String> directiveStore = new HashMap<>();
    private final Map<String, List<String>> namespaceTypeMapping = new HashMap<>();
    private final List<SchemaType> schemaTypes = new ArrayList<>();
    private final Map<String, List<SchemaField>> typeFields = new HashMap<>();
    
    // New fields for optimization
    private final Map<String, RelationshipInfo> relationships = new HashMap<>();
    private final Map<String, Object> queryCache = new ConcurrentHashMap<>();
    private final Set<String> loadedRelationships = new HashSet<>();
    
    // Add a Set to track detected namespaces
    private final Set<String> detectedNamespaces = new HashSet<>();

    public static void main(String[] args) {
        SpringApplication.run(GQL.class, args);
    }

    public GQL() throws Exception {
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        loadSchemaFile();
        buildRelationships();
        loadDataSource();
        
        System.out.println("Loaded data store keys: " + dataStore.keySet());
        System.out.println("Namespace mappings: " + namespaceTypeMapping);
        System.out.println("Built relationships: " + relationships.size());
    }
    
    // New method to build relationship information
    private void buildRelationships() {
        System.out.println("Building relationship mapping...");
        
        for (SchemaType type : schemaTypes) {
            List<SchemaField> fields = typeFields.getOrDefault(type.getName(), type.getFields());
            for (SchemaField field : fields) {
                String fieldType = field.type
                    .replace("[", "")
                    .replace("]", "")
                    .replace("!", "");
                
                // Skip scalar types
                if (fieldType.equals("ID") || fieldType.equals("String") || 
                    fieldType.equals("Int") || fieldType.equals("Float") || 
                    fieldType.equals("Boolean")) {
                    continue;
                }
                
                // Create relationship key
                String relationshipKey = type.getName() + "." + field.name;
                RelationshipInfo info = new RelationshipInfo();
                info.sourceType = type.getName();
                info.targetType = fieldType;
                info.fieldName = field.name;
                info.isList = field.type.startsWith("[");
                
                // Try to detect join field - either by ID naming convention or common patterns
                if (field.name.endsWith("Id") || field.name.endsWith("ID")) {
                    // If field name is customerId, join field is "id" in target Customer type
                    info.sourceField = field.name; 
                    info.targetField = "id";
                } else if (field.name.toLowerCase().equals(fieldType.toLowerCase())) {
                    // If field name matches type name, assume it's a reference by ID
                    info.sourceField = field.name + "Id";
                    info.targetField = "id";
                } else {
                    // Default assumption - join by ID
                    info.sourceField = fieldType.toLowerCase() + "Id";
                    info.targetField = "id";
                }
                
                relationships.put(relationshipKey, info);
                System.out.println("Added relationship: " + relationshipKey + " -> " + info.targetType + 
                                   " [" + info.sourceField + " -> " + info.targetField + "]");
            }
        }
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
        
        // First pass: identify types with @params directive as namespaces
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            if (line.startsWith("type ") && line.contains("@params")) {
                String typeDef = line.substring(5).trim();
                String typeName = typeDef.split(" ")[0].trim();
                detectedNamespaces.add(typeName);
                System.out.println("Detected namespace: " + typeName);
            }
        }
        
        // Second pass: process the schema with namespace awareness
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
                
                if (detectedNamespaces.contains(typeName)) {
                    currentNamespace = typeName;
                    currentType = new SchemaType(typeName);
                    schemaTypes.add(currentType);
                    
                    namespaceTypeMapping.putIfAbsent(currentNamespace, new ArrayList<>());
                    
                    if (line.contains("@params")) {
                    }
                    
                    inTypeDefinition = true;
                    System.out.println("Processing namespace: " + currentNamespace);
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
        System.out.println("Detected namespaces: " + detectedNamespaces);
    }

    private void loadDataSource() throws Exception {
        String sourceType = System.getProperty("source.type", "file");
        switch (sourceType.toLowerCase()) {
            case "db":
                loadDatabaseData();
                break;
            case "api":
                break;
            default:
                loadDataFiles();
                break;
        }
        loadAPIData();
    }

    private void loadDatabaseData() throws Exception {
        System.out.println("Loading data from database...");
        // Placeholder for JPA / JDBC logic
        // ...add your queries or repositories...
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

    @PermissionCheck(entity = "Query")
    @PostMapping("/query")
    public Map<String, Object> handleQuery(@RequestBody Map<String, Object> body) {
        Map<String, Object> query = (Map<String, Object>) body.get("query");
        Map<String, Object> result = new HashMap<>();
        
        // Query optimization: Check cache first (simple cache implementation)
        String cacheKey = objectMapper.valueToTree(query).toString();
        if (queryCache.containsKey(cacheKey)) {
            System.out.println("Cache hit for query: " + cacheKey.substring(0, Math.min(50, cacheKey.length())));
            return (Map<String, Object>) queryCache.get(cacheKey);
        }
        
        System.out.println("Processing query: " + query);
        long startTime = System.currentTimeMillis();

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
            
            Map<String, Object> response = Map.of("data", result);
            queryCache.put(cacheKey, response);
            return response;
        }

        if (query.containsKey("collection")) {
            String collection = (String) query.get("collection");
            List<String> fields = (List<String>) query.get("fields");
            Map<String, Object> where = (Map<String, Object>) query.get("where");
            Map<String, Object> include = (Map<String, Object>) query.get("include");
            
            // Handle pagination
            Integer limit = (Integer) query.get("limit");
            Integer offset = (Integer) query.get("offset");
            
            // Handle sorting
            String sortBy = (String) query.get("sortBy");
            String sortOrder = (String) query.get("sortOrder");

            // Selective loading - only get required fields
            List<Map<String, Object>> data = findDataForCollection(collection, null);
            
            // Apply filters
            List<Map<String, Object>> filteredData = applyFilters(data, where, fields);
            
            // Apply sorting if requested
            if (sortBy != null && !sortBy.isEmpty()) {
                applySorting(filteredData, sortBy, sortOrder);
            }
            
            // Handle includes for relationships
            if (include != null && !include.isEmpty()) {
                filteredData = resolveRelationships(collection, filteredData, include);
            }
            
            // Apply pagination if requested
            if (limit != null || offset != null) {
                filteredData = applyPagination(filteredData, offset, limit);
            }

            if (where != null && !filteredData.isEmpty() && !where.isEmpty()) {
                result.put(collection, filteredData.get(0));
            } else {
                result.put(collection, filteredData);
            }

            Map<String, Object> response = Map.of("data", result);
            queryCache.put(cacheKey, response);
            
            // Log query execution time for optimization insights
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.println("Query executed in " + executionTime + "ms");
            
            return response;
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
                    Map<String, Object> include = (Map<String, Object>) queryDetails.get("include");
                    
                    List<Map<String, Object>> data = findDataForCollection(collection, namespace);
                    System.out.println("Retrieved data for " + namespace + "." + collection + ": " + data.size() + " records");
                    
                    // Apply filters and field selection
                    List<Map<String, Object>> filteredData = applyFilters(data, where, fields);
                    
                    // Resolve relationships if includes are specified
                    if (include != null && !include.isEmpty()) {
                        String fullTypeName = namespace + collection;
                        filteredData = resolveRelationships(fullTypeName, filteredData, include);
                    }
                    
                    if (where != null && !filteredData.isEmpty() && !where.isEmpty()) {
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
                Map<String, Object> include = (Map<String, Object>) queryDetails.get("include");
                
                List<Map<String, Object>> data = findDataForCollection(namespace, null);
                List<Map<String, Object>> filteredData = applyFilters(data, where, fields);
                
                if (include != null && !include.isEmpty()) {
                    filteredData = resolveRelationships(namespace, filteredData, include);
                }
                
                if (where != null && !filteredData.isEmpty() && !where.isEmpty()) {
                    result.put(namespace, filteredData.get(0));
                } else {
                    result.put(namespace, filteredData);
                }
            }
        }

        Map<String, Object> response = Map.of("data", result);
        
        // Log query execution time for optimization insights
        long executionTime = System.currentTimeMillis() - startTime;
        System.out.println("Query executed in " + executionTime + "ms");
        
        // Cache the result
        if (executionTime > 10) { // Only cache queries that take some time to process
            queryCache.put(cacheKey, response);
        }
        
        return response;
    }
    
    // New method for selective filtering and field selection
    private List<Map<String, Object>> applyFilters(List<Map<String, Object>> data, 
                                                 Map<String, Object> where,
                                                 List<String> fields) {
        // Apply filters                                  
        List<Map<String, Object>> filtered = data.stream()
            .filter(row -> where == null || where.isEmpty() || matchesFilter(row, where))
            .map(row -> selectFields(row, fields))
            .collect(Collectors.toList());
            
        return filtered;
    }
    
    // Enhanced filter matching with support for more operators
    private boolean matchesFilter(Map<String, Object> row, Map<String, Object> filter) {
        return filter.entrySet().stream().allMatch(entry -> {
            String key = entry.getKey();
            Object filterValue = entry.getValue();
            Object rowValue = row.get(key);
            
            if (filterValue instanceof Map) {
                // Handle operators like $gt, $lt, etc.
                Map<String, Object> operators = (Map<String, Object>) filterValue;
                return operators.entrySet().stream().allMatch(op -> {
                    String operator = op.getKey();
                    Object value = op.getValue();
                    
                    switch (operator) {
                        case "$gt":
                            return compareValues(rowValue, value) > 0;
                        case "$lt":
                            return compareValues(rowValue, value) < 0;
                        case "$gte":
                            return compareValues(rowValue, value) >= 0;
                        case "$lte":
                            return compareValues(rowValue, value) <= 0;
                        case "$ne":
                            return !Objects.equals(rowValue, value);
                        case "$contains":
                            return rowValue != null && rowValue.toString().toLowerCase().contains(value.toString().toLowerCase());
                        case "$startsWith":
                            return rowValue != null && rowValue.toString().toLowerCase().startsWith(value.toString().toLowerCase());
                        case "$endsWith":
                            return rowValue != null && rowValue.toString().toLowerCase().endsWith(value.toString().toLowerCase());
                        case "$in":
                            return value instanceof List && rowValue != null && ((List) value).contains(rowValue);
                        case "$nin":
                            return value instanceof List && (rowValue == null || !((List) value).contains(rowValue));
                        default:
                            return Objects.equals(rowValue, value);
                    }
                });
            } else {
                // Simple equality check
                return Objects.equals(rowValue, filterValue);
            }
        });
    }
    
    private int compareValues(Object val1, Object val2) {
        if (val1 == null || val2 == null) {
            return val1 == val2 ? 0 : (val1 == null ? -1 : 1);
        }
        
        if (val1 instanceof Number && val2 instanceof Number) {
            return Double.compare(((Number) val1).doubleValue(), ((Number) val2).doubleValue());
        }
        
        return val1.toString().compareTo(val2.toString());
    }
    
    // Selective field loading - only include requested fields
    private Map<String, Object> selectFields(Map<String, Object> row, List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return row;
        }
        
        return row.entrySet().stream()
            .filter(e -> fields.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    // New method to apply pagination
    private List<Map<String, Object>> applyPagination(List<Map<String, Object>> data, 
                                                    Integer offset, 
                                                    Integer limit) {
        int startIndex = offset != null ? offset : 0;
        int endIndex = limit != null ? Math.min(startIndex + limit, data.size()) : data.size();
        
        if (startIndex >= data.size()) {
            return new ArrayList<>();
        }
        
        return data.subList(startIndex, endIndex);
    }
    
    // New method to apply sorting
    private void applySorting(List<Map<String, Object>> data, String sortBy, String sortOrder) {
        boolean ascending = sortOrder == null || "asc".equalsIgnoreCase(sortOrder);
        
        data.sort((a, b) -> {
            Object valA = a.get(sortBy);
            Object valB = b.get(sortBy);
            
            if (valA == null && valB == null) return 0;
            if (valA == null) return ascending ? -1 : 1;
            if (valB == null) return ascending ? 1 : -1;
            
            int result;
            if (valA instanceof Number && valB instanceof Number) {
                result = Double.compare(((Number) valA).doubleValue(), ((Number) valB).doubleValue());
            } else {
                result = valA.toString().compareToIgnoreCase(valB.toString());
            }
            
            return ascending ? result : -result;
        });
    }
    
    // New method to resolve relationships - full relation support
    private List<Map<String, Object>> resolveRelationships(String sourceType, 
                                                         List<Map<String, Object>> sourceData, 
                                                         Map<String, Object> includes) {
        if (sourceData.isEmpty()) return sourceData;
        
        // Track loaded relationships for this query to avoid circular dependencies
        loadedRelationships.clear();
        
        // Make a deep copy to avoid modifying original data
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : sourceData) {
            result.add(new HashMap<>(item));
        }
        
        // Process each include directive
        for (String relationField : includes.keySet()) {
            Object includeValue = includes.get(relationField);
            List<String> includeFields = new ArrayList<>();
            Map<String, Object> nestedIncludes = null;
            
            if (includeValue instanceof List) {
                includeFields = (List<String>) includeValue;
            } else if (includeValue instanceof Map) {
                Map<String, Object> includeDetails = (Map<String, Object>) includeValue;
                if (includeDetails.containsKey("fields")) {
                    includeFields = (List<String>) includeDetails.get("fields");
                }
                if (includeDetails.containsKey("include")) {
                    nestedIncludes = (Map<String, Object>) includeDetails.get("include");
                }
            } else if (includeValue instanceof Boolean && (Boolean) includeValue) {
                // Include all fields with no nested includes
            }
            
            // Find the relationship info
            String relationshipKey = sourceType + "." + relationField;
            RelationshipInfo relInfo = relationships.get(relationshipKey);
            
            if (relInfo == null) {
                // Try with just the type name (not fully qualified)
                String simpleType = sourceType;
                if (sourceType.contains(".")) {
                    simpleType = sourceType.substring(sourceType.lastIndexOf(".") + 1);
                }
                relationshipKey = simpleType + "." + relationField;
                relInfo = relationships.get(relationshipKey);
            }
            
            if (relInfo != null) {
                // To avoid circular dependencies
                String loadKey = relationshipKey;
                if (loadedRelationships.contains(loadKey)) {
                    continue;
                }
                loadedRelationships.add(loadKey);
                
                resolveRelationship(result, relInfo, includeFields, nestedIncludes);
            } else {
                System.out.println("Relationship not found: " + relationshipKey);
            }
        }
        
        return result;
    }
    
    // Helper method to resolve a single relationship
    private void resolveRelationship(List<Map<String, Object>> sourceData, 
                                   RelationshipInfo relInfo,
                                   List<String> includeFields,
                                   Map<String, Object> nestedIncludes) {
        // Get the target type data
        List<Map<String, Object>> targetData = findDataForCollection(relInfo.targetType, null);
        
        // Create indexes for quick lookup
        Map<Object, Map<String, Object>> targetIndex = new HashMap<>();
        for (Map<String, Object> target : targetData) {
            if (target.containsKey(relInfo.targetField)) {
                targetIndex.put(target.get(relInfo.targetField), target);
            }
        }
        
        // For each source record, find and link the related records
        for (Map<String, Object> source : sourceData) {
            if (!source.containsKey(relInfo.sourceField) && relInfo.sourceField.endsWith("Id")) {
                // Try with lowercase 'id' if uppercase 'Id' not found
                String altField = relInfo.sourceField.substring(0, relInfo.sourceField.length() - 2) + "id";
                if (source.containsKey(altField)) {
                    relInfo.sourceField = altField;
                }
            }
            
            if (source.containsKey(relInfo.sourceField)) {
                Object sourceValue = source.get(relInfo.sourceField);
                
                if (relInfo.isList) {
                    // Handle one-to-many relationships
                    List<Map<String, Object>> relatedItems = new ArrayList<>();
                    
                    // If source field is a list of IDs
                    if (sourceValue instanceof List) {
                        for (Object id : (List<?>) sourceValue) {
                            if (targetIndex.containsKey(id)) {
                                Map<String, Object> relatedItem = new HashMap<>(targetIndex.get(id));
                                
                                // Apply field selection
                                if (includeFields != null && !includeFields.isEmpty()) {
                                    relatedItem = selectFields(relatedItem, includeFields);
                                }
                                
                                // Process nested includes if any
                                if (nestedIncludes != null && !nestedIncludes.isEmpty()) {
                                    List<Map<String, Object>> nestedSource = new ArrayList<>();
                                    nestedSource.add(relatedItem);
                                    nestedSource = resolveRelationships(relInfo.targetType, nestedSource, nestedIncludes);
                                    relatedItem = nestedSource.get(0);
                                }
                                
                                relatedItems.add(relatedItem);
                            }
                        }
                    } 
                    // Otherwise, find all target items where target[foreignKey] = source[primaryKey]
                    else {
                        for (Map<String, Object> target : targetData) {
                            if (target.containsKey(relInfo.targetField) && 
                                Objects.equals(target.get(relInfo.targetField), sourceValue)) {
                                
                                Map<String, Object> relatedItem = new HashMap<>(target);
                                
                                // Apply field selection
                                if (includeFields != null && !includeFields.isEmpty()) {
                                    relatedItem = selectFields(relatedItem, includeFields);
                                }
                                
                                // Process nested includes if any
                                if (nestedIncludes != null && !nestedIncludes.isEmpty()) {
                                    List<Map<String, Object>> nestedSource = new ArrayList<>();
                                    nestedSource.add(relatedItem);
                                    nestedSource = resolveRelationships(relInfo.targetType, nestedSource, nestedIncludes);
                                    relatedItem = nestedSource.get(0);
                                }
                                
                                relatedItems.add(relatedItem);
                            }
                        }
                    }
                    
                    source.put(relInfo.fieldName, relatedItems);
                } 
                else {
                    // Handle one-to-one relationships
                    if (targetIndex.containsKey(sourceValue)) {
                        Map<String, Object> relatedItem = new HashMap<>(targetIndex.get(sourceValue));
                        
                        // Apply field selection
                        if (includeFields != null && !includeFields.isEmpty()) {
                            relatedItem = selectFields(relatedItem, includeFields);
                        }
                        
                        // Process nested includes if any
                        if (nestedIncludes != null && !nestedIncludes.isEmpty()) {
                            List<Map<String, Object>> nestedSource = new ArrayList<>();
                            nestedSource.add(relatedItem);
                            nestedSource = resolveRelationships(relInfo.targetType, nestedSource, nestedIncludes);
                            relatedItem = nestedSource.get(0);
                        }
                        
                        source.put(relInfo.fieldName, relatedItem);
                    }
                }
            }
        }
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
        debug.put("relationships", relationships);
        return debug;
    }
    
    @GetMapping("/debug/relationships")
    public Map<String, RelationshipInfo> getRelationships() {
        return relationships;
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

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS();
    }

    @PermissionCheck(entity = "QuerySocket")
    @MessageMapping("/querySocket")
    @SendTo("/topic/responses")
    public Map<String, Object> handleSocketQuery(Map<String, Object> body) {
        return handleQuery(body);
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
    
    // New class to track relationship information
    private static class RelationshipInfo {
        private String sourceType;
        private String targetType;
        private String fieldName;
        private String sourceField;
        private String targetField;
        private boolean isList;
        
        @Override
        public String toString() {
            return "RelationshipInfo{" +
                    "sourceType='" + sourceType + '\'' +
                    ", targetType='" + targetType + '\'' +
                    ", fieldName='" + fieldName + '\'' +
                    ", sourceField='" + sourceField + '\'' +
                    ", targetField='" + targetField + '\'' +
                    ", isList=" + isList +
                    '}';
        }
    }
}

