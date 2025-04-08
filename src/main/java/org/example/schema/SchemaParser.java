package org.example.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.config.DomainConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parser for GraphQL schema files.
 * Handles type definitions, fields, and directives.
 */
public class SchemaParser {
    private final Map<String, SchemaType> schemaTypes = new LinkedHashMap<>();
    private final Set<String> scalarTypes = new HashSet<>(
            Arrays.asList("ID", "String", "Int", "Float", "Boolean")
    );
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DomainConfig domainConfig;
    
    /**
     * Default constructor
     */
    public SchemaParser() {
        this.domainConfig = new DomainConfig(); // Use default config if not provided
    }
    
    /**
     * Constructor with domain configuration
     * 
     * @param domainConfig Domain configuration with namespaces and entity types
     */
    public SchemaParser(DomainConfig domainConfig) {
        this.domainConfig = domainConfig;
    }
    
    /**
     * Parse GraphQL schema content into a map of schema types
     */
    public Map<String, SchemaType> parseSchema(String schemaContent) {
        // Clear previous state if reusing the parser
        schemaTypes.clear();
        
        // Remove comments from schema content
        String cleanedContent = removeComments(schemaContent);
        
        // Parse type definitions
        parseTypeDefinitions(cleanedContent);
        
        // Parse directives on types
        parseDirectives(cleanedContent);
        
        System.out.println("Parsed " + schemaTypes.size() + " schema types");
        return Collections.unmodifiableMap(schemaTypes);
    }
    
    /**
     * Parse a schema file into a map of schema types
     */
    public Map<String, SchemaType> parseSchema(Path schemaFile) throws IOException {
        String content = Files.readString(schemaFile);
        return parseSchema(content);
    }
    
    /**
     * Get the current schema
     * 
     * @return Unmodifiable map of the parsed schema types
     */
    public Map<String, SchemaType> getSchema() {
        return Collections.unmodifiableMap(schemaTypes);
    }
    
    /**
     * Get a schema representation suitable for the AI prompt
     */
    public String getSchemaForAIPrompt() {
        StringBuilder builder = new StringBuilder();
        
        // Start with the main namespaces
        List<String> namespaces = domainConfig.getNamespaces().stream()
            .map(this::capitalizeFirstLetter)
            .collect(Collectors.toList());
        
        builder.append("Top-level namespaces: ").append(String.join(", ", namespaces)).append("\n\n");
        
        // Add Query type first as it's the entry point
        if (schemaTypes.containsKey("Query")) {
            SchemaType queryType = schemaTypes.get("Query");
            builder.append("Query: Root query object with these entry points:\n");
            
            for (SchemaField field : queryType.getFields()) {
                builder.append("  - ").append(field.getName())
                       .append(": ").append(field.getType())
                       .append("\n");
            }
            builder.append("\n");
        }
        
        // Add important namespace types
        for (String namespace : namespaces) {
            if (schemaTypes.containsKey(namespace)) {
                SchemaType namespaceType = schemaTypes.get(namespace);
                builder.append(namespace).append(": Container for all ").append(namespace.toLowerCase()).append(" data.\n");
                
                for (SchemaField field : namespaceType.getFields()) {
                    builder.append("  - ").append(field.getName())
                           .append(": ").append(field.getType())
                           .append("\n");
                }
                builder.append("\n");
            }
        }
        
        // Add key entity types - combine namespaces and entity types
        List<String> keyEntityTypes = new ArrayList<>();
        for (String namespace : namespaces) {
            for (String entityType : domainConfig.getEntityTypeSuffixes()) {
                keyEntityTypes.add(namespace + entityType);
            }
        }
        
        // Take a subset of entity types for the prompt to avoid it being too large
        List<String> selectedTypes = keyEntityTypes.stream()
            .filter(schemaTypes::containsKey)
            .limit(10)
            .collect(Collectors.toList());
        
        for (String typeName : selectedTypes) {
            if (schemaTypes.containsKey(typeName)) {
                SchemaType type = schemaTypes.get(typeName);
                builder.append(typeName).append(": Key entity with fields:\n");
                
                for (SchemaField field : type.getFields()) {
                    if (!field.getName().endsWith("Id") || field.getName().equals("id")) {
                        builder.append("  - ").append(field.getName());
                        if (field.getType().contains("!")) {
                            builder.append(" (required)");
                        }
                        builder.append("\n");
                    }
                }
                builder.append("\n");
            }
        }
        
        return builder.toString();
    }
    
    /**
     * Get debug information about the schema structure
     */
    public JsonNode getDebugInfo() {
        ArrayNode typesArray = objectMapper.createArrayNode();
        
        for (SchemaType type : schemaTypes.values()) {
            ObjectNode typeNode = objectMapper.createObjectNode();
            typeNode.put("name", type.getName());
            
            if (type.getNamespace() != null) {
                typeNode.put("namespace", type.getNamespace());
            }
            
            if (type.getSourceFile() != null) {
                typeNode.put("sourceFile", type.getSourceFile());
            }
            
            if (type.getApiUrl() != null) {
                typeNode.put("apiUrl", type.getApiUrl());
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
            typesArray.add(typeNode);
        }
        
        return typesArray;
    }
    
    /**
     * Parse type definitions from schema content
     */
    private void parseTypeDefinitions(String schemaContent) {
        // Regular expression to match type definitions
        // Matches: type TypeName { ... }
        Pattern typePattern = Pattern.compile(
            "type\\s+(\\w+)\\s*(?:@[^{]+)?\\s*\\{([^}]+)\\}",
            Pattern.DOTALL
        );
        
        Matcher typeMatcher = typePattern.matcher(schemaContent);
        
        while (typeMatcher.find()) {
            String typeName = typeMatcher.group(1);
            String typeBody = typeMatcher.group(2);
            
            // Skip if already processed
            if (schemaTypes.containsKey(typeName)) {
                continue;
            }
            
            SchemaType schemaType = new SchemaType(typeName);
            parseFields(typeBody, schemaType);
            schemaTypes.put(typeName, schemaType);
        }
    }
    
    /**
     * Parse fields for a type definition
     */
    private void parseFields(String typeBody, SchemaType schemaType) {
        // Regular expression to match field definitions
        // Matches: fieldName: FieldType or fieldName: [FieldType] or fieldName: FieldType!
        Pattern fieldPattern = Pattern.compile(
            "(\\w+)\\s*:\\s*(\\[?\\w+!?\\]?!?)(?:\\s*@[^\\n]+)?",
            Pattern.DOTALL
        );
        
        Matcher fieldMatcher = fieldPattern.matcher(typeBody);
        
        while (fieldMatcher.find()) {
            String fieldName = fieldMatcher.group(1);
            String fieldType = fieldMatcher.group(2);
            
            boolean isRequired = fieldType.endsWith("!");
            boolean isList = fieldType.startsWith("[") && fieldType.contains("]");
            
            // Clean up the type name
            String cleanType = fieldType
                .replace("[", "")
                .replace("]", "")
                .replace("!", "");
                
            boolean isScalar = scalarTypes.contains(cleanType);
            
            SchemaField field = new SchemaField(
                fieldName, 
                fieldType,
                isRequired,
                isList,
                isScalar
            );
            
            schemaType.addField(field);
        }
    }
    
    /**
     * Parse directives on types
     */
    private void parseDirectives(String schemaContent) {
        // Parse @source directive
        parseSourceDirective(schemaContent);
        
        // Parse @api directive
        parseApiDirective(schemaContent);
        
        // Parse @params directive
        parseParamsDirective(schemaContent);
        
        // Parse @log directive
        parseLogDirective(schemaContent);
        
        // Parse relationship fields to infer namespaces
        inferNamespacesFromRelationships();
    }
    
    /**
     * Parse @source directive that specifies a file data source
     */
    private void parseSourceDirective(String schemaContent) {
        // Two patterns to match different ways the directive might appear:
        // 1. type TypeName @source(file: "path/to/file") {
        // 2. type TypeName { ... } @source(file: "path/to/file")
        
        // Pattern 1
        Pattern sourcePattern1 = Pattern.compile(
            "type\\s+(\\w+)\\s+@source\\s*\\(\\s*file\\s*:\\s*\"([^\"]+)\"\\s*\\)",
            Pattern.DOTALL
        );
        
        Matcher sourceMatcher1 = sourcePattern1.matcher(schemaContent);
        
        while (sourceMatcher1.find()) {
            String typeName = sourceMatcher1.group(1);
            String sourceFile = sourceMatcher1.group(2);
            
            if (schemaTypes.containsKey(typeName)) {
                schemaTypes.get(typeName).setSourceFile(sourceFile);
                System.out.println("Set source file for " + typeName + ": " + sourceFile);
            }
        }
        
        // Pattern 2 - look for type declarations followed by source directive
        Pattern sourcePattern2 = Pattern.compile(
            "type\\s+(\\w+)\\s*\\{[^}]*\\}\\s*@source\\s*\\(\\s*file\\s*:\\s*\"([^\"]+)\"\\s*\\)",
            Pattern.DOTALL
        );
        
        Matcher sourceMatcher2 = sourcePattern2.matcher(schemaContent);
        
        while (sourceMatcher2.find()) {
            String typeName = sourceMatcher2.group(1);
            String sourceFile = sourceMatcher2.group(2);
            
            if (schemaTypes.containsKey(typeName)) {
                schemaTypes.get(typeName).setSourceFile(sourceFile);
                System.out.println("Set source file for " + typeName + ": " + sourceFile);
            }
        }
    }
    
    /**
     * Parse @api directive that specifies an API endpoint
     */
    private void parseApiDirective(String schemaContent) {
        // Matches: type TypeName @api(url: "https://example.com/api")
        Pattern apiPattern = Pattern.compile(
            "type\\s+(\\w+)\\s+@api\\s*\\(\\s*url\\s*:\\s*\"([^\"]+)\"\\s*\\)",
            Pattern.DOTALL
        );
        
        Matcher apiMatcher = apiPattern.matcher(schemaContent);
        
        while (apiMatcher.find()) {
            String typeName = apiMatcher.group(1);
            String apiUrl = apiMatcher.group(2);
            
            if (schemaTypes.containsKey(typeName)) {
                schemaTypes.get(typeName).setApiUrl(apiUrl);
            }
        }
    }
    
    /**
     * Parse @params directive that specifies namespace parameters
     */
    private void parseParamsDirective(String schemaContent) {
        // Matches: type TypeName @params(fields: ["field1", "field2"])
        Pattern paramsPattern = Pattern.compile(
            "type\\s+(\\w+)\\s+@params\\s*\\(\\s*fields\\s*:\\s*\\[([^\\]]+)\\]\\s*\\)",
            Pattern.DOTALL
        );
        
        Matcher paramsMatcher = paramsPattern.matcher(schemaContent);
        
        while (paramsMatcher.find()) {
            String typeName = paramsMatcher.group(1);
            String fieldsStr = paramsMatcher.group(2);
            
            if (schemaTypes.containsKey(typeName)) {
                SchemaType type = schemaTypes.get(typeName);
                // Mark this as a namespace type
                type.setNamespace(typeName.toLowerCase());
                System.out.println("Set namespace " + typeName.toLowerCase() + " for type " + typeName);
                
                // Also assign namespace to related types
                // Any type that starts with this namespace name should be in this namespace
                String namespacePrefix = typeName;
                for (Map.Entry<String, SchemaType> entry : schemaTypes.entrySet()) {
                    String otherTypeName = entry.getKey();
                    if (otherTypeName.startsWith(namespacePrefix) && !otherTypeName.equals(typeName)) {
                        SchemaType otherType = entry.getValue();
                        otherType.setNamespace(typeName.toLowerCase());
                        System.out.println("Also set namespace " + typeName.toLowerCase() + " for related type " + otherTypeName);
                    }
                }
            }
        }
    }
    
    /**
     * Parse @log directive that specifies logging
     */
    private void parseLogDirective(String schemaContent) {
        // Matches: type TypeName { ... } @log
        Pattern logPattern = Pattern.compile(
            "type\\s+(\\w+)\\s*\\{[^}]*\\}\\s*@log",
            Pattern.DOTALL
        );

        Matcher logMatcher = logPattern.matcher(schemaContent);

        while (logMatcher.find()) {
            String typeName = logMatcher.group(1);

            if (schemaTypes.containsKey(typeName)) {
                schemaTypes.get(typeName).setLog(true);
                System.out.println("Set log directive for " + typeName);
            }
        }
    }
    
    /**
     * Infer namespaces from relationship fields
     */
    private void inferNamespacesFromRelationships() {
        // Get the configured namespaces in capitalized form 
        List<String> capitalizedNamespaces = domainConfig.getNamespaces().stream()
            .map(this::capitalizeFirstLetter)
            .collect(Collectors.toList());
            
        // Find field references between types to establish relationships and namespaces
        for (SchemaType type : schemaTypes.values()) {
            // Skip if namespace is already set
            if (type.getNamespace() != null && !type.getNamespace().isEmpty()) {
                continue;
            }
            
            String typeName = type.getName();
            
            // Try to infer namespace from type name
            for (String prefix : capitalizedNamespaces) {
                if (typeName.startsWith(prefix)) {
                    type.setNamespace(prefix.toLowerCase());
                    System.out.println("Inferred namespace " + prefix.toLowerCase() + " for type " + typeName + " from name prefix");
                    break;
                }
            }
        }
        
        // Additional pass to handle implicit namespaces
        for (String typeName : schemaTypes.keySet()) {
            SchemaType type = schemaTypes.get(typeName);
            if (type.getNamespace() == null || type.getNamespace().isEmpty()) {
                // For types like "Customer", add them to all standard namespaces
                for (String entityType : domainConfig.getEntityTypeSuffixes()) {
                    if (typeName.equals(entityType)) {
                        for (String namespace : domainConfig.getNamespaces()) {
                            String namespacedTypeName = capitalizeFirstLetter(namespace) + entityType;
                            if (schemaTypes.containsKey(namespacedTypeName)) {
                                type.setNamespace(namespace);
                                System.out.println("Set namespace " + namespace + " for common type " + typeName);
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        // Second pass to handle types that might not have direct namespace prefixes
        for (SchemaType type : schemaTypes.values()) {
            // Skip if namespace is already set
            if (type.getNamespace() != null && !type.getNamespace().isEmpty()) {
                continue;
            }
            
            // Check if this type has fields that reference types with known namespaces
            Map<String, Integer> namespaceReferences = new HashMap<>();
            
            for (SchemaField field : type.getFields()) {
                String fieldType = field.getType()
                    .replace("[", "")
                    .replace("]", "")
                    .replace("!", "");
                
                if (schemaTypes.containsKey(fieldType)) {
                    SchemaType referencedType = schemaTypes.get(fieldType);
                    if (referencedType.getNamespace() != null && !referencedType.getNamespace().isEmpty()) {
                        String namespace = referencedType.getNamespace();
                        namespaceReferences.put(namespace, namespaceReferences.getOrDefault(namespace, 0) + 1);
                    }
                }
            }
            
            // If we have namespace references, use the most common one
            if (!namespaceReferences.isEmpty()) {
                String mostCommonNamespace = namespaceReferences.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
                
                if (mostCommonNamespace != null) {
                    type.setNamespace(mostCommonNamespace);
                    System.out.println("Inferred namespace " + mostCommonNamespace + " for type " + type.getName() + " based on field references");
                }
            }
        }
    }
    
    /**
     * Capitalize the first letter of a string
     */
    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
    
    /**
     * Remove comments from schema content
     */
    private String removeComments(String content) {
        return content.replaceAll("#[^\\n]*", "");
    }
}
