package org.example.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.data.DataLoader;
import org.example.schema.SchemaType;
import org.example.schema.SchemaField;
import org.example.RelationshipInfo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Processes GraphQL-style queries against the loaded data
 */
public class QueryProcessor {
    private final Map<String, SchemaType> schema;
    private final DataLoader dataLoader;
    private final ObjectMapper objectMapper = new ObjectMapper();
    // Map to cache namespace prefixes to type names for quicker lookup
    private final Map<String, Map<String, String>> namespaceTypeMap = new HashMap<>();
    // List of relationships
    private final List<RelationshipInfo> relationships;
    
    public QueryProcessor(Map<String, SchemaType> schema, DataLoader dataLoader, List<RelationshipInfo> relationships) {
        this.schema = schema;
        this.dataLoader = dataLoader;
        this.relationships = relationships;
        buildNamespaceTypeMap();
    }
    
    /**
     * Builds a map of namespace prefixes to their type names for quicker lookup
     */
    private void buildNamespaceTypeMap() {
        // Create initial namespace maps
        Set<String> knownNamespaces = new HashSet<>();
        
        // Add standard namespaces we always want to support
        knownNamespaces.add("marketing");
        knownNamespaces.add("finance");
        knownNamespaces.add("external");
        
        // First pass: extract namespaces from schema types
        for (Map.Entry<String, SchemaType> entry : schema.entrySet()) {
            String typeName = entry.getKey();
            SchemaType type = entry.getValue();
            String namespace = type.getNamespace();
            
            if (namespace != null && !namespace.isEmpty()) {
                String normalizedNamespace = namespace.toLowerCase();
                knownNamespaces.add(normalizedNamespace);
            } else {
                // Try to extract namespace from type name for types without explicit namespace
                for (String prefix : Arrays.asList("Marketing", "Finance", "External")) {
                    if (typeName.startsWith(prefix)) {
                        knownNamespaces.add(prefix.toLowerCase());
                        break;
                    }
                }
            }
        }
        
        // Initialize namespace type maps for all known namespaces
        for (String namespace : knownNamespaces) {
            namespaceTypeMap.put(namespace, new HashMap<>());
        }
        
        // Second pass: populate namespace type maps
        for (Map.Entry<String, SchemaType> entry : schema.entrySet()) {
            String typeName = entry.getKey();
            SchemaType type = entry.getValue();
            String namespace = type.getNamespace();
            
            if (namespace != null && !namespace.isEmpty()) {
                // Use explicit namespace
                String normalizedNamespace = namespace.toLowerCase();
                addTypeToNamespace(normalizedNamespace, typeName);
            } else {
                // Try to extract namespace from type name
                boolean namespaceFound = false;
                for (String prefix : Arrays.asList("Marketing", "Finance", "External")) {
                    if (typeName.startsWith(prefix)) {
                        String extractedNamespace = prefix.toLowerCase();
                        addTypeToNamespace(extractedNamespace, typeName);
                        namespaceFound = true;
                        break;
                    }
                }
                
                // If no namespace was found, add to all namespaces as a fallback
                if (!namespaceFound) {
                    for (String namespace2 : knownNamespaces) {
                        addTypeToNamespace(namespace2, typeName);
                    }
                }
            }
        }
        
        // For each known entity type (Customer, Order, etc.), make sure it's mapped in each namespace
        for (String typeName : schema.keySet()) {
            // Extract the entity part without the namespace prefix
            String entityName = typeName;
            for (String prefix : Arrays.asList("Marketing", "Finance", "External")) {
                if (typeName.startsWith(prefix)) {
                    entityName = typeName.substring(prefix.length());
                    break;
                }
            }
            
            // For common entity types, add mappings to all namespaces
            for (String entityType : Arrays.asList("Customer", "Order", "Campaign", "Lead", "Event")) {
                if (typeName.endsWith(entityType)) {
                    for (String namespace : knownNamespaces) {
                        Map<String, String> typeMap = namespaceTypeMap.get(namespace);
                        // Add common variations of the entity name
                        typeMap.putIfAbsent(entityType.toLowerCase(), typeName);
                        // First letter lowercase
                        String lcFirst = entityType.substring(0, 1).toLowerCase() + entityType.substring(1);
                        typeMap.putIfAbsent(lcFirst, typeName);
                    }
                }
            }
        }
        
        // Add direct mappings for clear resolution with capitalized type names
        addSpecialCaseMappings();
        
        System.out.println("Built namespace type map: ");
        for (Map.Entry<String, Map<String, String>> entry : namespaceTypeMap.entrySet()) {
            System.out.println("  Namespace: " + entry.getKey());
            for (Map.Entry<String, String> typeEntry : entry.getValue().entrySet()) {
                System.out.println("    " + typeEntry.getKey() + " -> " + typeEntry.getValue());
            }
        }
    }
    
    /**
     * Add special case mappings for better type resolution
     */
    private void addSpecialCaseMappings() {
        // Add direct mappings for common entities in each namespace
        Map<String, String> marketingMap = namespaceTypeMap.get("marketing");
        if (marketingMap != null) {
            // Add explicit mappings with proper capitalization
            marketingMap.put("Customer", "MarketingCustomer");
            marketingMap.put("Order", "MarketingOrder");
            marketingMap.put("Campaign", "MarketingCampaign");
            marketingMap.put("Lead", "MarketingLead");
            marketingMap.put("Event", "MarketingEvent");
        }
        
        Map<String, String> financeMap = namespaceTypeMap.get("finance");
        if (financeMap != null) {
            financeMap.put("Customer", "FinanceCustomer");
        }
        
        Map<String, String> externalMap = namespaceTypeMap.get("external");
        if (externalMap != null) {
            externalMap.put("Customer", "ExternalCustomer");
        }
    }
    
    /**
     * Add a type to a namespace map with common variations of its name
     */
    private void addTypeToNamespace(String namespace, String typeName) {
        Map<String, String> typeMap = namespaceTypeMap.get(namespace);
        if (typeMap == null) {
            typeMap = new HashMap<>();
            namespaceTypeMap.put(namespace, typeMap);
        }
        
        // Add the full type name
        typeMap.put(typeName.toLowerCase(), typeName);
        
        // Extract the entity part (e.g., "Customer" from "MarketingCustomer")
        String entityName = typeName;
        for (String prefix : Arrays.asList("Marketing", "Finance", "External")) {
            if (typeName.startsWith(prefix)) {
                entityName = typeName.substring(prefix.length());
                break;
            }
        }
        
        // Add the entity name in various forms
        if (!entityName.equals(typeName)) {
            typeMap.put(entityName.toLowerCase(), typeName);
            
            // Add first letter lowercase version
            if (entityName.length() > 0) {
                String lcFirst = entityName.substring(0, 1).toLowerCase() + 
                               (entityName.length() > 1 ? entityName.substring(1) : "");
                typeMap.put(lcFirst, typeName);
            }
            
            // Add the capitalized entity name for better match
            typeMap.put(entityName, typeName);
        }
    }
    
    /**
     * Process a query and return results
     */
    public JsonNode processQuery(JsonNode queryNode) {
        System.out.println("Processing query: " + queryNode);
        ObjectNode result = objectMapper.createObjectNode();
        
        // Handle main query structure
        if (queryNode.has("query")) {
            JsonNode queryObj = queryNode.get("query");
            
            // Process each type query
            Iterator<Map.Entry<String, JsonNode>> fields = queryObj.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String namespaceName = entry.getKey();
                JsonNode namespaceNode = entry.getValue();
                
                // Special handling for metadata query
                if (namespaceName.equals("metadata")) {
                    result.set(namespaceName, processMetadataQuery(namespaceNode));
                    continue;
                }
                
                // Check if this is a direct type or a namespace containing types
                if (isDirectTypeQuery(namespaceNode)) {
                    // Direct type query (legacy format)
                    String typeName = namespaceName;
                    JsonNode typeQuery = namespaceNode;
                    result.set(typeName, processTypeQuery(typeName, typeQuery));
                } else {
                    // Process as namespace query
                    ObjectNode namespaceResult = result.putObject(namespaceName);
                    processNamespaceQuery(namespaceName, namespaceNode, namespaceResult);
                }
            }
        } else if (queryNode.isTextual()) {
            // Simple namespace reference like "Marketing"
            String namespaceName = queryNode.asText();
            result.set(namespaceName, objectMapper.createObjectNode());
        }
        
        return result;
    }
    
    /**
     * Process a metadata query
     */
    private JsonNode processMetadataQuery(JsonNode metadataNode) {
        System.out.println("Processing metadata query: " + metadataNode);
        ObjectNode metadataResult = objectMapper.createObjectNode();
        
        // Extract fields to return
        List<String> fields = extractFields(metadataNode);
        
        // Return types information if requested
        if (fields.contains("*") || fields.contains("types")) {
            ArrayNode typesArray = metadataResult.putArray("types");
            for (Map.Entry<String, SchemaType> entry : schema.entrySet()) {
                SchemaType type = entry.getValue();
                ObjectNode typeNode = objectMapper.createObjectNode();
                typeNode.put("name", type.getName());
                
                if (type.getNamespace() != null) {
                    typeNode.put("namespace", type.getNamespace());
                }
                
                ArrayNode fieldsArray = objectMapper.createArrayNode();
                for (SchemaField field : type.getFields()) {
                    ObjectNode fieldNode = objectMapper.createObjectNode();
                    fieldNode.put("name", field.getName());
                    fieldNode.put("type", field.getType());
                    fieldNode.put("required", field.isRequired());
                    fieldNode.put("isList", field.isList());
                    fieldNode.put("isScalar", field.isScalar());
                    fieldsArray.add(fieldNode);
                }
                typeNode.set("fields", fieldsArray);
                
                ObjectNode sourceNode = objectMapper.createObjectNode();
                if (type.getSourceFile() != null) {
                    sourceNode.put("type", "file");
                    sourceNode.put("path", type.getSourceFile());
                    typeNode.set("source", sourceNode);
                } else if (type.getApiUrl() != null) {
                    sourceNode.put("type", "api");
                    sourceNode.put("url", type.getApiUrl());
                    typeNode.set("source", sourceNode);
                }
                
                typesArray.add(typeNode);
            }
        }
        
        // Return namespaces information if requested
        if (fields.contains("*") || fields.contains("namespaces")) {
            ArrayNode namespacesArray = metadataResult.putArray("namespaces");
            
            // Extract namespaces from types
            Map<String, Set<String>> namespaceMap = new HashMap<>();
            for (Map.Entry<String, SchemaType> entry : schema.entrySet()) {
                SchemaType type = entry.getValue();
                String namespace = type.getNamespace();
                
                if (namespace != null && !namespace.isEmpty()) {
                    namespaceMap.computeIfAbsent(namespace, k -> new HashSet<>()).add(type.getName());
                } else {
                    // Try to extract namespace from type name
                    String typeName = type.getName();
                    for (String prefix : Arrays.asList("Marketing", "Finance", "External")) {
                        if (typeName.startsWith(prefix)) {
                            String ns = prefix.toLowerCase();
                            namespaceMap.computeIfAbsent(ns, k -> new HashSet<>()).add(typeName);
                            break;
                        }
                    }
                }
            }
            
            // Add standard namespaces if not present
            for (String ns : Arrays.asList("marketing", "finance", "external")) {
                namespaceMap.computeIfAbsent(ns, k -> new HashSet<>());
            }
            
            // Add namespaces to result
            for (Map.Entry<String, Set<String>> nsEntry : namespaceMap.entrySet()) {
                ObjectNode nsNode = objectMapper.createObjectNode();
                nsNode.put("name", nsEntry.getKey());
                
                ArrayNode typesArray = objectMapper.createArrayNode();
                for (String typeName : nsEntry.getValue()) {
                    typesArray.add(typeName);
                }
                nsNode.set("types", typesArray);
                
                namespacesArray.add(nsNode);
            }
        }
        
        // Return relationships information if requested
        if (fields.contains("*") || fields.contains("relationships")) {
            ArrayNode relationshipsArray = metadataResult.putArray("relationships");
            
            for (RelationshipInfo relationship : this.relationships) {
                ObjectNode relNode = objectMapper.createObjectNode();
                relNode.put("sourceType", relationship.getSourceType());
                relNode.put("fieldName", relationship.getFieldName());
                relNode.put("targetType", relationship.getTargetType());
                relNode.put("isList", relationship.isList());
                relationshipsArray.add(relNode);
            }
        }
        
        // Return directives information if requested
        if (fields.contains("*") || fields.contains("directives")) {
            ArrayNode directivesArray = metadataResult.putArray("directives");
            
            // Collect directives from schema types
            for (Map.Entry<String, SchemaType> entry : schema.entrySet()) {
                String typeName = entry.getKey();
                SchemaType type = entry.getValue();
                
                if (type.getSourceFile() != null) {
                    ObjectNode directive = objectMapper.createObjectNode();
                    directive.put("typeName", typeName);
                    directive.put("directive", "source");
                    directive.put("value", type.getSourceFile());
                    directivesArray.add(directive);
                }
                
                if (type.getApiUrl() != null) {
                    ObjectNode directive = objectMapper.createObjectNode();
                    directive.put("typeName", typeName);
                    directive.put("directive", "api");
                    directive.put("value", type.getApiUrl());
                    directivesArray.add(directive);
                }
                
                if (type.getNamespace() != null) {
                    ObjectNode directive = objectMapper.createObjectNode();
                    directive.put("typeName", typeName);
                    directive.put("directive", "params");
                    directive.put("value", "fields: [\"id\"]");
                    directivesArray.add(directive);
                }
            }
        }
        
        return metadataResult;
    }
    
    /**
     * Determine if this is a direct type query or a namespace containing types
     */
    private boolean isDirectTypeQuery(JsonNode node) {
        // A direct type query typically has fields like "fields", "where", etc.
        return node.has("fields") || node.has("where");
    }
    
    /**
     * Process queries for a specific namespace
     */
    private void processNamespaceQuery(String namespaceName, JsonNode namespaceNode, ObjectNode result) {
        System.out.println("Processing namespace query for " + namespaceName + ": " + namespaceNode);
        
        // Get the map of entity names to type names for this namespace
        Map<String, String> namespaceTypes = namespaceTypeMap.get(namespaceName.toLowerCase());
        
        if (namespaceTypes == null) {
            // Try case-insensitive lookup
            for (String ns : namespaceTypeMap.keySet()) {
                if (ns.equalsIgnoreCase(namespaceName)) {
                    namespaceTypes = namespaceTypeMap.get(ns);
                    System.out.println("Found case-insensitive namespace match: " + ns);
                    break;
                }
            }
            
            if (namespaceTypes == null) {
                System.out.println("No types found for namespace: " + namespaceName);
                return;
            }
        }
        
        // Process each entity in the namespace
        Iterator<Map.Entry<String, JsonNode>> entityFields = namespaceNode.fields();
        while (entityFields.hasNext()) {
            Map.Entry<String, JsonNode> entityEntry = entityFields.next();
            String entityName = entityEntry.getKey();
            JsonNode entityQuery = entityEntry.getValue();
            
            // Find the full type name
            String fullTypeName = resolveTypeName(namespaceName.toLowerCase(), entityName);
            
            if (fullTypeName != null) {
                System.out.println("Mapped " + namespaceName + "." + entityName + " to type: " + fullTypeName);
                result.set(entityName, processTypeQuery(fullTypeName, entityQuery));
            } else {
                System.out.println("Could not resolve type for: " + namespaceName + "." + entityName);
                result.set(entityName, objectMapper.createArrayNode());
            }
        }
    }
    
    /**
     * Resolve a namespace and entity name to a full type name
     */
    private String resolveTypeName(String namespaceName, String entityName) {
        // First, try direct lookup
        Map<String, String> namespaceTypes = namespaceTypeMap.get(namespaceName.toLowerCase());
        
        if (namespaceTypes == null) {
            System.out.println("No types found for namespace: " + namespaceName.toLowerCase());
            return null;
        }
        
        // Try direct match - including with proper capitalization
        if (namespaceTypes.containsKey(entityName)) {
            return namespaceTypes.get(entityName);
        }
        
        String normalizedEntityName = entityName.toLowerCase();
        
        // Try case-insensitive lookup
        if (namespaceTypes.containsKey(normalizedEntityName)) {
            return namespaceTypes.get(normalizedEntityName);
        }
        
        // Try with capital first letter
        String capitalizedEntity = entityName.substring(0, 1).toUpperCase() + 
                                 (entityName.length() > 1 ? entityName.substring(1) : "");
        if (namespaceTypes.containsKey(capitalizedEntity)) {
            return namespaceTypes.get(capitalizedEntity);
        }
        
        // Try with namespace prefix
        String expectedTypeName = namespaceName.substring(0, 1).toUpperCase() + 
                                 namespaceName.substring(1) + capitalizedEntity;
        if (schema.containsKey(expectedTypeName)) {
            return expectedTypeName;
        }
        
        // Try with namespace prefix in lowercase
        String fullTypePrefix = namespaceName + entityName;
        String fullTypePrefixLower = fullTypePrefix.toLowerCase();
        
        for (String typeName : namespaceTypes.keySet()) {
            if (typeName.toLowerCase().startsWith(fullTypePrefixLower)) {
                return namespaceTypes.get(typeName);
            }
        }
        
        // Try common entity types with pluralization
        if (normalizedEntityName.endsWith("s")) {
            String singular = normalizedEntityName.substring(0, normalizedEntityName.length() - 1);
            if (namespaceTypes.containsKey(singular)) {
                return namespaceTypes.get(singular);
            }
            
            // Try with capital first letter for singular form
            String capitalizedSingular = singular.substring(0, 1).toUpperCase() + 
                                       (singular.length() > 1 ? singular.substring(1) : "");
            if (namespaceTypes.containsKey(capitalizedSingular)) {
                return namespaceTypes.get(capitalizedSingular);
            }
        }
        
        // Last resort: Try to directly construct the type name from namespace and entity
        String constructedTypeName = namespaceName.substring(0, 1).toUpperCase() + 
                                    namespaceName.substring(1) + 
                                    capitalizedEntity;
        if (schema.containsKey(constructedTypeName)) {
            return constructedTypeName;
        }
        
        return null;
    }
    
    /**
     * Find a type by prefix (case-insensitive)
     */
    private String findTypeByPrefix(String prefix) {
        String normalizedPrefix = prefix.toLowerCase();
        for (String typeName : schema.keySet()) {
            if (typeName.toLowerCase().equals(normalizedPrefix)) {
                return typeName;
            }
        }
        return null;
    }
    
    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
    
    /**
     * Process a query for a specific type
     */
    private JsonNode processTypeQuery(String typeName, JsonNode typeQuery) {
        System.out.println("Processing type query for " + typeName + ": " + typeQuery);
        
        // Try case-insensitive match if exact match fails
        List<Map<String, Object>> data = dataLoader.getData(typeName);
        String actualTypeName = typeName;
        
        if (data == null || data.isEmpty()) {
            // Try case-insensitive match
            for (String schemaTypeName : schema.keySet()) {
                if (schemaTypeName.equalsIgnoreCase(typeName)) {
                    System.out.println("Using case-insensitive match: " + schemaTypeName + " for " + typeName);
                    data = dataLoader.getData(schemaTypeName);
                    actualTypeName = schemaTypeName; // Update to matched case
                    break;
                }
            }
        }
        
        if (data == null || data.isEmpty()) {
            System.out.println("No data found for type: " + actualTypeName);
            System.out.println("Available types with data: " + String.join(", ", dataLoader.getDataTypes()));
            return objectMapper.createArrayNode();
        }
        
        // Extract query parameters
        List<String> fields = extractFields(typeQuery);
        Map<String, Object> where = extractWhere(typeQuery);
        
        // Apply filters
        List<Map<String, Object>> filteredData = applyFilters(data, where, fields);
        
        if (filteredData.isEmpty()) {
            System.out.println("No data found after applying filters. Where conditions: " + where);
        } else {
            System.out.println("Found " + filteredData.size() + " records after applying filters.");
        }
        
        // Convert to JSON
        return objectMapper.valueToTree(filteredData);
    }
    
    /**
     * Extract field list from query
     */
    private List<String> extractFields(JsonNode typeQuery) {
        List<String> fields = new ArrayList<>();
        
        if (typeQuery.has("fields")) {
            JsonNode fieldsNode = typeQuery.get("fields");
            if (fieldsNode.isArray()) {
                for (JsonNode fieldNode : fieldsNode) {
                    fields.add(fieldNode.asText());
                }
            } else if (fieldsNode.isTextual()) {
                fields.add(fieldsNode.asText());
            }
        } else if (typeQuery.isArray()) {
            // Array format like ["field1", "field2"]
            for (JsonNode fieldNode : typeQuery) {
                fields.add(fieldNode.asText());
            }
        }
        
        // Default to all fields if none specified
        if (fields.isEmpty()) {
            fields.add("*");
        }
        
        return fields;
    }
    
    /**
     * Extract where conditions from query
     */
    private Map<String, Object> extractWhere(JsonNode typeQuery) {
        Map<String, Object> where = new HashMap<>();
        
        if (typeQuery.has("where")) {
            JsonNode whereNode = typeQuery.get("where");
            Iterator<Map.Entry<String, JsonNode>> fields = whereNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String field = entry.getKey();
                JsonNode value = entry.getValue();
                
                if (value.isTextual()) {
                    where.put(field, value.asText());
                } else if (value.isNumber()) {
                    where.put(field, value.asDouble());
                } else if (value.isBoolean()) {
                    where.put(field, value.asBoolean());
                } else if (value.isNull()) {
                    where.put(field, null);
                }
            }
        }
        
        return where;
    }
    
    /**
     * Apply filters to data
     */
    private List<Map<String, Object>> applyFilters(List<Map<String, Object>> data, Map<String, Object> where, List<String> fields) {
        // Apply WHERE filters
        List<Map<String, Object>> filtered = data;
        
        if (!where.isEmpty()) {
            filtered = data.stream()
                .filter(record -> matchesConditions(record, where))
                .collect(Collectors.toList());
        }
        
        // Apply field selection (projection)
        if (!fields.contains("*")) {
            filtered = filtered.stream()
                .map(record -> selectFields(record, fields))
                .collect(Collectors.toList());
        }
        
        return filtered;
    }
    
    /**
     * Check if a record matches all conditions
     */
    private boolean matchesConditions(Map<String, Object> record, Map<String, Object> conditions) {
        for (Map.Entry<String, Object> condition : conditions.entrySet()) {
            String field = condition.getKey();
            Object value = condition.getValue();
            
            if (!record.containsKey(field) || !Objects.equals(record.get(field), value)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Select only specific fields from a record
     */
    private Map<String, Object> selectFields(Map<String, Object> record, List<String> fields) {
        Map<String, Object> result = new HashMap<>();
        
        for (String field : fields) {
            if (record.containsKey(field)) {
                result.put(field, record.get(field));
            }
        }
        
        return result;
    }
}

