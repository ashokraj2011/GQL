package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.data.DataLoader;
import org.example.query.QueryProcessor;
import org.example.schema.SchemaField;
import org.example.schema.SchemaParser;
import org.example.schema.SchemaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@SpringBootApplication
@RestController
@RequestMapping("/api")
@EnableWebSocketMessageBroker
public class GQL implements WebSocketMessageBrokerConfigurer {
    @Value("${schema.path:schema.graphql}")
    private String schemaPath;
    
    @Value("${data.directory:data}")
    private String dataDirectory;
    
    private SchemaParser schemaParser;
    private DataLoader dataLoader;
    private QueryProcessor queryProcessor;
    private ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Set<String>> namespaceTypesMap = new HashMap<>();
    private List<RelationshipInfo> relationships = new ArrayList<>();

    public static void main(String[] args) {
        SpringApplication.run(GQL.class, args);
    }
    
    @EventListener(ContextRefreshedEvent.class)
    public void initialize() throws IOException {
        // Initialize schema parser
        System.out.println("Initializing schema from path: " + schemaPath);
        schemaParser = new SchemaParser();
        
        // Load schema content
        String schemaContent = loadSchemaContent(schemaPath);
        Map<String, SchemaType> schema = schemaParser.parseSchema(schemaContent);
        
        // Extract relationships from schema
        extractRelationships(schema);
        
        // Find and set the proper data directory path
        String effectiveDataDirectory = findEffectiveDataDirectory(dataDirectory);
        System.out.println("Using effective data directory: " + effectiveDataDirectory);
        
        // Initialize data loader
        System.out.println("Initializing data loader with directory: " + effectiveDataDirectory);
        dataLoader = new DataLoader(effectiveDataDirectory);
        dataLoader.loadData(schema);
        
        // Initialize query processor
        queryProcessor = new QueryProcessor(schema, dataLoader, relationships);
        
        // Initialize namespace map
        for (Map.Entry<String, SchemaType> entry : schema.entrySet()) {
            String typeName = entry.getKey();
            String namespace = entry.getValue().getNamespace();
            if (namespace != null && !namespace.isEmpty()) {
                namespaceTypesMap.computeIfAbsent(namespace.toLowerCase(), k -> new HashSet<>()).add(typeName);
            }
        }
        
        // Extract namespace from type names where not explicitly set
        for (Map.Entry<String, SchemaType> entry : schema.entrySet()) {
            String typeName = entry.getKey();
            SchemaType type = entry.getValue();
            
            // If namespace isn't set yet, try to extract it from type name
            if (type.getNamespace() == null || type.getNamespace().isEmpty()) {
                // Common namespace prefixes in type names 
                for (String prefix : Arrays.asList("Marketing", "Finance", "External")) {
                    if (typeName.startsWith(prefix)) {
                        String extractedNamespace = prefix.toLowerCase();
                        type.setNamespace(extractedNamespace);
                        namespaceTypesMap.computeIfAbsent(extractedNamespace, k -> new HashSet<>()).add(typeName);
                        System.out.println("Added extracted namespace " + extractedNamespace + " for type " + typeName);
                        break;
                    }
                }
            }
        }
        
        // Add explicit namespace entries for root-level namespace types
        // This ensures queries can reference them directly
        if (!namespaceTypesMap.containsKey("marketing")) {
            namespaceTypesMap.put("marketing", new HashSet<>());
        }
        if (!namespaceTypesMap.containsKey("finance")) {
            namespaceTypesMap.put("finance", new HashSet<>());
        }
        
        // Print namespace mapping for debugging
        for (Map.Entry<String, Set<String>> entry : namespaceTypesMap.entrySet()) {
            System.out.println("Namespace: " + entry.getKey() + " -> Types: " + entry.getValue());
        }
        
        System.out.println("GQL initialization complete. Ready to process queries.");
    }

    /**
     * Load schema content from file or classpath
     */
    private String loadSchemaContent(String schemaPath) throws IOException {
        // First, try to load from file system
        Path path = Paths.get(schemaPath);
        if (Files.exists(path)) {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        }
        
        // Next, try to load from classpath
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(schemaPath)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        
        // Finally, try with different path prefixes
        for (String prefix : Arrays.asList("gql/", "src/main/resources/")) {
            path = Paths.get(prefix + schemaPath);
            if (Files.exists(path)) {
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            }
        }
        
        throw new IOException("Schema file not found: " + schemaPath);
    }

    /**
     * Extract relationships between types from the schema
     * 
     * @param schema The parsed schema
     */
    private void extractRelationships(Map<String, SchemaType> schema) {
        for (Map.Entry<String, SchemaType> entry : schema.entrySet()) {
            String sourceType = entry.getKey();
            SchemaType type = entry.getValue();
            
            for (SchemaField field : type.getFields()) {
                // Skip scalar fields
                if (field.isScalar()) {
                    continue;
                }
                
                // Check if this field references another type
                String targetType = field.getType();
                if (schema.containsKey(targetType)) {
                    // Add relationship
                    RelationshipInfo relationship = new RelationshipInfo(
                        sourceType,
                        field.getName(),
                        targetType,
                        field.isList()
                    );
                    relationships.add(relationship);
                    System.out.println("Found relationship: " + relationship);
                }
            }
        }
        
        System.out.println("Extracted " + relationships.size() + " relationships");
    }
    
    /**
     * Handles GraphQL queries via REST endpoint
     */
    @PostMapping("/query")
    @PermissionCheck
    public ResponseEntity<JsonNode> handleQuery(@RequestBody JsonNode requestBody) {
        try {
            JsonNode result;
            
            // Check if the request uses the simplified format
            if (isSimplifiedQueryFormat(requestBody)) {
                // Convert from simplified format to standard format
                ObjectNode standardFormat = objectMapper.createObjectNode();
                ObjectNode queryObj = standardFormat.putObject("query");
                
                Iterator<Map.Entry<String, JsonNode>> fields = requestBody.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    queryObj.set(entry.getKey(), entry.getValue());
                }
                
                result = queryProcessor.processQuery(standardFormat);
            } else {
                // Process the query normally
                result = queryProcessor.processQuery(requestBody);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", "Error processing query: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorNode);
        }
    }
    
    /**
     * Handles GraphQL queries via WebSocket
     */
    @MessageMapping("/ws-query")
    @SendTo("/topic/results")
    public JsonNode handleWebSocketQuery(JsonNode requestBody) throws JsonProcessingException {
        try {
            // Use the same processing logic as the REST endpoint
            if (isSimplifiedQueryFormat(requestBody)) {
                ObjectNode standardFormat = objectMapper.createObjectNode();
                ObjectNode queryObj = standardFormat.putObject("query");
                
                Iterator<Map.Entry<String, JsonNode>> fields = requestBody.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    queryObj.set(entry.getKey(), entry.getValue());
                }
                
                return queryProcessor.processQuery(standardFormat);
            } else {
                return queryProcessor.processQuery(requestBody);
            }
        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", "Error processing WebSocket query: " + e.getMessage());
            return errorNode;
        }
    }
    
    /**
     * Provides system metadata
     */
    @GetMapping("/metadata")
    public ResponseEntity<Map<String, Object>> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        
        // Add relationships
        metadata.put("relationships", relationships);
        
        // Add namespaces
        metadata.put("namespaces", namespaceTypesMap);
        
        // Add data loader debug info
        metadata.put("dataLoader", dataLoader.getDebugInfo());
        
        return ResponseEntity.ok(metadata);
    }
    
    /**
     * Determines if the request uses the simplified query format
     * Simplified format: { "TypeName": [], "AnotherType": [] }
     * 
     * @param requestBody The incoming request
     * @return true if it's a simplified format, false otherwise
     */
    private boolean isSimplifiedQueryFormat(JsonNode requestBody) {
        // If it has a "query" field with nested objects that aren't simple types, it's standard format
        if (requestBody.has("query")) {
            JsonNode query = requestBody.get("query");
            Iterator<Map.Entry<String, JsonNode>> fields = query.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode value = field.getValue();
                // If we find nested objects that have their own structure, it's likely standard format
                if (value.isObject() && !value.has("fields") && !value.has("where")) {
                    return false;
                }
            }
        }
        
        // If it has a "query" field with direct field->value mappings, it's standard format
        if (requestBody.has("query") && requestBody.get("query").has("fields")) {
            return false;
        }
        
        // Check for simplified format structure
        Iterator<String> fieldNames = requestBody.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            // If any field has an array value, it might be the simplified format
            if (requestBody.get(fieldName).isArray()) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/gql-ws").withSockJS();
    }
    
    /**
     * Determines the effective data directory by checking multiple possible paths
     */
    private String findEffectiveDataDirectory(String configuredDirectory) {
        List<String> candidatePaths = new ArrayList<>();
        
        // Try the configured directory first
        candidatePaths.add(configuredDirectory);
        
        // Try some common variations
        candidatePaths.add("data");
        candidatePaths.add("gql/data");
        candidatePaths.add("/Users/ashokraj/Downloads/learn/dataContext/gql/data");
        
        for (String path : candidatePaths) {
            Path dirPath = Paths.get(path);
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                System.out.println("Found data directory at: " + dirPath.toAbsolutePath());
                return dirPath.toString();
            }
        }
        
        // If we can't find it, return the configured directory and DataLoader
        // will create it if needed
        System.out.println("Could not find existing data directory, will use: " + configuredDirectory);
        return configuredDirectory;
    }
}
