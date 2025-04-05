/**
 * GraphQL-like JSON API
 * 
 * Copyright (c) 2023-2024 Ashok Raj (ashok.nair.raj@gmail.com)
 * Licensed under MIT License
 */
package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.ai.AIQueryException;
import org.example.ai.AIQueryGenerator;
import org.example.data.DataLoader;
import org.example.query.QueryProcessor;
import org.example.schema.SchemaField;
import org.example.schema.SchemaParser;
import org.example.schema.SchemaType;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = false)
    private AIQueryGenerator aiQueryGenerator;

    public static void main(String[] args) {
        SpringApplication.run(GQL.class, args);
    }
    
    @EventListener(ContextRefreshedEvent.class)
    public void initialize() {
        try {
            // Initialize schema parser
            schemaParser = new SchemaParser();
            
            // Load schema from file or resource
            Map<String, SchemaType> schema = loadSchema();
            System.out.println("Loaded schema with " + schema.size() + " types");
            
            // Build relationships
            extractRelationships(schema);
            System.out.println("Extracted " + relationships.size() + " relationships");
            
            // Initialize data loader
            dataLoader = new DataLoader(dataDirectory);
            dataLoader.loadData(schema);
            
            // Initialize query processor
            queryProcessor = new QueryProcessor(schema, dataLoader, relationships);
            
            // Build namespace to types mapping
            buildNamespaceTypesMap(schema);
            
            System.out.println("Initialization complete, GQL ready to process queries");
        } catch (Exception e) {
            System.err.println("Error initializing GQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load the GraphQL schema from a file or resource
     */
    private Map<String, SchemaType> loadSchema() throws IOException {
        Path schemaFilePath = Paths.get(schemaPath);
        String schemaContent = null; // Initialize with null to ensure compiler knows we're handling it
        
        if (Files.exists(schemaFilePath)) {
            // Load from file
            schemaContent = Files.readString(schemaFilePath);
            System.out.println("Loaded schema from file: " + schemaFilePath.toAbsolutePath());
        } else {
            // Try to load from classpath/resources
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(schemaPath)) {
                if (inputStream != null) {
                    schemaContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    System.out.println("Loaded schema from classpath resource: " + schemaPath);
                } else {
                    // Try alternative locations
                    List<String> alternativePaths = Arrays.asList(
                        "schema.graphql",
                        "gql/schema.graphql",
                        "../schema.graphql"
                    );
                    
                    boolean found = false;
                    for (String altPath : alternativePaths) {
                        Path path = Paths.get(altPath);
                        if (Files.exists(path)) {
                            schemaContent = Files.readString(path);
                            System.out.println("Loaded schema from alternative path: " + path.toAbsolutePath());
                            found = true;
                            break;
                        }
                    }
                    
                    if (!found) {
                        throw new IOException("Schema file not found at " + schemaPath + " or in resources");
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading schema: " + e.getMessage());
                throw e;
            }
        }
        
        // Additional safety check to satisfy compiler
        if (schemaContent == null) {
            throw new IOException("Failed to load schema content from any source");
        }
        
        return schemaParser.parseSchema(schemaContent);
    }

    /**
     * Extract relationships between types based on field references
     */
    private void extractRelationships(Map<String, SchemaType> schema) {
        for (Map.Entry<String, SchemaType> entry : schema.entrySet()) {
            String sourceTypeName = entry.getKey();
            SchemaType sourceType = entry.getValue();
            
            for (SchemaField field : sourceType.getFields()) {
                String fieldName = field.getName();
                String fieldTypeName = field.getType()
                    .replace("[", "")
                    .replace("]", "")
                    .replace("!", "");
                
                // Skip scalar types
                if (fieldTypeName.equals("ID") || fieldTypeName.equals("String") || 
                    fieldTypeName.equals("Int") || fieldTypeName.equals("Float") || 
                    fieldTypeName.equals("Boolean")) {
                    continue;
                }
                
                // Check if the field type exists in the schema
                if (schema.containsKey(fieldTypeName)) {
                    // Add relationship
                    RelationshipInfo relationship = new RelationshipInfo(
                        sourceTypeName,
                        fieldName,
                        fieldTypeName,
                        field.isList()
                    );
                    relationships.add(relationship);
                }
            }
        }
    }

    /**
     * Build a map of namespaces to their types
     */
    private void buildNamespaceTypesMap(Map<String, SchemaType> schema) {
        namespaceTypesMap.clear();
        
        for (Map.Entry<String, SchemaType> entry : schema.entrySet()) {
            String typeName = entry.getKey();
            SchemaType type = entry.getValue();
            String namespace = type.getNamespace();
            
            if (namespace != null && !namespace.isEmpty()) {
                namespaceTypesMap.computeIfAbsent(namespace, k -> new HashSet<>()).add(typeName);
            } else {
                // Try to infer namespace from type name
                for (String prefix : Arrays.asList("Marketing", "Finance", "External")) {
                    if (typeName.startsWith(prefix)) {
                        String ns = prefix.toLowerCase();
                        namespaceTypesMap.computeIfAbsent(ns, k -> new HashSet<>()).add(typeName);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Process a JSON query
     */
    @PostMapping("/query")
    public ResponseEntity<JsonNode> processQuery(@RequestBody JsonNode queryNode) {
        try {
            JsonNode result = queryProcessor.processQuery(queryNode);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorNode);
        }
    }

    /**
     * Process a natural language query using AI
     */
    @PostMapping("/nl-query")
    public ResponseEntity<JsonNode> processNaturalLanguageQuery(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", "Query cannot be empty");
            return ResponseEntity.badRequest().body(errorNode);
        }
        
        try {
            if (aiQueryGenerator == null) {
                throw new AIQueryException("AI query generator is not configured");
            }
            
            // Generate JSON query from natural language using AI
            JsonNode jsonQuery = aiQueryGenerator.generateQuery(query);
            System.out.println("Generated query: " + jsonQuery);
            
            // Process the generated query
            JsonNode result = queryProcessor.processQuery(jsonQuery);
            
            // Return both the generated query and the result
            ObjectNode response = objectMapper.createObjectNode();
            response.set("query", jsonQuery);
            response.set("result", result);
            return ResponseEntity.ok(response);
        } catch (AIQueryException e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", "AI query error: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorNode);
        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorNode);
        }
    }

    /**
     * Get schema information
     */
    @GetMapping("/schema")
    public ResponseEntity<JsonNode> getSchema() {
        try {
            return ResponseEntity.ok(schemaParser.getDebugInfo());
        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorNode);
        }
    }

    /**
     * Get data info and statistics
     */
    @GetMapping("/data-info")
    public ResponseEntity<JsonNode> getDataInfo() {
        try {
            return ResponseEntity.ok(objectMapper.valueToTree(dataLoader.getDebugInfo()));
        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorNode);
        }
    }

    /**
     * Get all namespaces and their types
     */
    @GetMapping("/namespaces")
    public ResponseEntity<Map<String, Set<String>>> getNamespaces() {
        return ResponseEntity.ok(namespaceTypesMap);
    }

    /**
     * Get all relationships between types
     */
    @GetMapping("/relationships")
    public ResponseEntity<List<RelationshipInfo>> getRelationships() {
        return ResponseEntity.ok(relationships);
    }

    /**
     * WebSocket configuration for real-time messaging
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/gql-websocket").withSockJS();
    }

    /**
     * WebSocket endpoint for queries
     */
    @MessageMapping("/query")
    @SendTo("/topic/results")
    public JsonNode handleWebSocketQuery(String queryString) throws JsonProcessingException {
        try {
            JsonNode queryNode = objectMapper.readTree(queryString);
            return queryProcessor.processQuery(queryNode);
        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", e.getMessage());
            return errorNode;
        }
    }
}
