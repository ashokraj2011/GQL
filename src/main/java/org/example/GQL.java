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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.ai.AIQueryException;
import org.example.ai.AIQueryGenerator;
import org.example.data.DataLoader;
import org.example.query.QueryProcessor;
import org.example.schema.SchemaField;
import org.example.schema.SchemaParser;
import org.example.schema.SchemaType;
import org.example.timetravel.TimeTravel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
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
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
@RestController
@RequestMapping("/api")
@EnableWebSocketMessageBroker
public class GQL implements WebSocketMessageBrokerConfigurer {
    @Value("${schema.path:schema.graphql}")
    private String schemaPath;
    
    private SchemaParser schemaParser;
    
    @Autowired
    private DataLoader dataLoader;
    
    private QueryProcessor queryProcessor;
    private ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Set<String>> namespaceTypesMap = new HashMap<>();
    private List<RelationshipInfo> relationships = new ArrayList<>();

    @Autowired(required = false)
    private AIQueryGenerator aiQueryGenerator;
    
    @Autowired
    private TimeTravel timeTravel;

    public static void main(String[] args) {
        SpringApplication.run(GQL.class, args);
    }
    
    @EventListener(ContextRefreshedEvent.class)
    public void initialize() throws IOException {
        System.out.println("Initializing GQL service...");
        
        // Initialize schema parser
        schemaParser = new SchemaParser();
        
        // Load schema
        Map<String, SchemaType> schema = loadSchema();
        
        // Load data
        dataLoader.loadData(schema);
        
        // Build relationships
        extractRelationships(schema);
        
        // Initialize query processor
        queryProcessor = new QueryProcessor(schema, dataLoader, relationships);
        
        System.out.println("GQL service initialized successfully!");
        
        // Build namespace types map
        buildNamespaceTypesMap(schema);
    }
    
    /**
     * Configure WebSocket message broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
    
    /**
     * Register STOMP endpoints
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/gql-websocket")
               .setAllowedOrigins("*")
               .withSockJS();
    }
    
    /**
     * Load GraphQL schema from file
     */
    private Map<String, SchemaType> loadSchema() throws IOException {
        System.out.println("Loading schema from: " + schemaPath);
        
        Path path = Paths.get(schemaPath);
        
        // If path doesn't exist directly, try finding it
        if (!Files.exists(path)) {
            // Try multiple resolutions
            List<Path> possiblePaths = new ArrayList<>();
            possiblePaths.add(path);
            possiblePaths.add(Paths.get("src/main/resources", schemaPath));
            possiblePaths.add(Paths.get("src", schemaPath));
            possiblePaths.add(Paths.get("gql", schemaPath));
            
            for (Path candidate : possiblePaths) {
                if (Files.exists(candidate)) {
                    path = candidate;
                    break;
                }
            }
        }
        
        // If still not found, look for it in resources
        String schemaContent;
        if (!Files.exists(path)) {
            System.out.println("Schema file not found on disk, looking in classpath: " + schemaPath);
            // Try loading from classpath
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(schemaPath)) {
                if (inputStream == null) {
                    throw new IOException("Schema file not found: " + schemaPath);
                }
                schemaContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } else {
            System.out.println("Loading schema from file: " + path.toAbsolutePath());
            schemaContent = Files.readString(path);
        }
        
        return schemaParser.parseSchema(schemaContent);
    }
    
    /**
     * Extract relationships from schema
     */
    private void extractRelationships(Map<String, SchemaType> schema) {
        System.out.println("Extracting relationships from schema...");
        
        // Track relationships to avoid duplicates
        Set<String> processedRelationships = new HashSet<>();
        
        for (Map.Entry<String, SchemaType> entry : schema.entrySet()) {
            SchemaType sourceType = entry.getValue();
            String sourceTypeName = entry.getKey();
            
            // Process each field for potential relationships
            for (SchemaField field : sourceType.getFields()) {
                String fieldName = field.getName();
                
                // Skip scalar fields
                if (field.isScalar()) {
                    continue;
                }
                
                // Get clean type name (remove [] and !)
                String targetTypeName = field.getType()
                        .replace("[", "")
                        .replace("]", "")
                        .replace("!", "");
                
                // Check if target type exists in schema
                if (schema.containsKey(targetTypeName)) {
                    String relationshipKey = sourceTypeName + ":" + fieldName + ":" + targetTypeName;
                    if (!processedRelationships.contains(relationshipKey)) {
                        RelationshipInfo relationship = new RelationshipInfo(
                                sourceTypeName,
                                fieldName,
                                targetTypeName,
                                field.isList()
                        );
                        
                        relationships.add(relationship);
                        processedRelationships.add(relationshipKey);
                    }
                }
            }
        }
        
        System.out.println("Extracted " + relationships.size() + " relationships");
    }
    
    /**
     * Build map of namespaces to types
     */
    private void buildNamespaceTypesMap(Map<String, SchemaType> schema) {
        for (Map.Entry<String, SchemaType> entry : schema.entrySet()) {
            SchemaType type = entry.getValue();
            String namespace = type.getNamespace();
            
            if (namespace != null && !namespace.isEmpty()) {
                Set<String> types = namespaceTypesMap.computeIfAbsent(namespace, k -> new HashSet<>());
                types.add(entry.getKey());
            }
        }
    }
    
    /**
     * Process a query through REST API
     */
    @PostMapping("/query")
    public ResponseEntity<Object> processQuery(@RequestBody JsonNode queryNode) {
        try {
            JsonNode result = queryProcessor.processQuery(queryNode);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error processing query: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Process a query for a specific point in time
     */
    @PostMapping("/query/at")
    public ResponseEntity<Object> processQueryAt(
            @RequestBody JsonNode queryNode,
            @RequestParam("timestamp") String timestamp) {
        try {
            Instant pointInTime = Instant.parse(timestamp);
            JsonNode result = timeTravel.processQueryAtTime(queryNode, pointInTime, queryProcessor);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error processing time travel query: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Process a natural language query using AI
     */
    @PostMapping("/nl-query")
    public ResponseEntity<Object> processNaturalLanguageQuery(@RequestBody Map<String, String> request) {
        String nlQuery = request.get("query");
        
        if (aiQueryGenerator == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "AI query generator not configured");
            return ResponseEntity.badRequest().body(error);
        }
        
        try {
            // Use schema info as additional context for the AI
            JsonNode structuredQuery = aiQueryGenerator.generateQuery(nlQuery, schemaParser.getSchemaForAIPrompt());
            JsonNode result = queryProcessor.processQuery(structuredQuery);
            
            // Create a response that includes both the generated query and results
            ObjectNode response = objectMapper.createObjectNode();
            response.set("generatedQuery", structuredQuery);
            response.set("results", result);
            
            return ResponseEntity.ok(response);
        } catch (AIQueryException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate query: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error processing query: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Get schema information
     */
    @GetMapping("/schema")
    public ResponseEntity<Object> getSchema() {
        try {
            return ResponseEntity.ok(schemaParser.getDebugInfo());
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Get data information
     */
    @GetMapping("/data-info")
    public ResponseEntity<Object> getDataInfo() {
        try {
            return ResponseEntity.ok(dataLoader.getDebugInfo());
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Get namespaces information
     */
    @GetMapping("/namespaces")
    public ResponseEntity<Object> getNamespaces() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("namespaces", namespaceTypesMap);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Get relationships information
     */
    @GetMapping("/relationships")
    public ResponseEntity<Object> getRelationships() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("relationships", relationships);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Get record history
     */
    @GetMapping("/history/{typeName}/{id}")
    public ResponseEntity<Object> getRecordHistory(@PathVariable String typeName, @PathVariable String id) {
        try {
            List<Map<String, Object>> history = timeTravel.getRecordHistory(typeName, id);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * WebSocket endpoint for query processing
     */
    @MessageMapping("/query")
    @SendTo("/topic/results")
    public JsonNode processWebSocketQuery(JsonNode queryNode) throws Exception {
        return queryProcessor.processQuery(queryNode);
    }
    
    /**
     * Get health status
     */
    @GetMapping("/health")
    public ResponseEntity<Object> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", Instant.now().toString());
        
        Map<String, Object> components = new HashMap<>();
        components.put("schema", Map.of("status", "UP", "types", schemaParser.getSchema().size()));
        components.put("data", Map.of("status", "UP", "sources", dataLoader.getDataTypes().size()));
        
        status.put("components", components);
        
        return ResponseEntity.ok(status);
    }
}

