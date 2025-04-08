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
import org.example.graphql.GraphQLQueryTransformer;
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
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
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
    
    private GraphQLQueryTransformer graphQLQueryTransformer;
    
    // Request metrics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final List<Long> recentRequestTimestamps = Collections.synchronizedList(new ArrayList<>());
    private final int RECENT_WINDOW_MS = 60000; // 1 minute window for req/sec calculation

    public static void main(String[] args) {
        SpringApplication.run(GQL.class, args);
    }
    
    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        System.out.println("Initializing GQL API");
        
        try {
            // Initialize schema parser
            schemaParser = new SchemaParser();
            
            // Try to load schema from file
            Path schemaFilePath = Paths.get(schemaPath);
            Map<String, SchemaType> schema;
            
            if (Files.exists(schemaFilePath)) {
                System.out.println("Loading schema from file: " + schemaFilePath.toAbsolutePath());
                schema = schemaParser.parseSchema(schemaFilePath);
            } else {
                // Try to load from resources if file doesn't exist
                try (InputStream schemaStream = getClass().getClassLoader().getResourceAsStream(schemaPath)) {
                    if (schemaStream != null) {
                        System.out.println("Loading schema from resources: " + schemaPath);
                        String schemaContent = new String(schemaStream.readAllBytes(), StandardCharsets.UTF_8);
                        schema = schemaParser.parseSchema(schemaContent);
                    } else {
                        throw new IOException("Schema file not found in resources: " + schemaPath);
                    }
                }
            }
            
            // Load data based on schema
            dataLoader.loadData(schema);
            
            // Build namespace to types mapping
            buildNamespaceTypeMap(schema);
            
            // Build relationship map
            detectRelationships(schema);
            
            // Initialize query processor with schema, dataLoader, relationships, and timeTravel
            queryProcessor = new QueryProcessor(schema, dataLoader,  relationships, timeTravel);
            
            // Initialize GraphQL query transformer
            graphQLQueryTransformer = new GraphQLQueryTransformer();
            
            System.out.println("GQL API initialized successfully with " + 
                               schema.size() + " types in " + 
                               namespaceTypesMap.size() + " namespaces");
        } catch (Exception e) {
            System.err.println("Failed to initialize GQL API: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Build a map of namespace to types
     */
    private void buildNamespaceTypeMap(Map<String, SchemaType> schema) {
        // Initialize namespace map
        namespaceTypesMap.clear();
        
        // Group types by namespace
        for (SchemaType type : schema.values()) {
            String namespace = type.getNamespace();
            if (namespace != null && !namespace.isEmpty()) {
                namespaceTypesMap
                    .computeIfAbsent(namespace, k -> new HashSet<>())
                    .add(type.getName());
            }
        }
        
        System.out.println("Built namespace map with " + namespaceTypesMap.size() + " namespaces");
    }
    
    /**
     * Detect relationships between types
     */
    private void detectRelationships(Map<String, SchemaType> schema) {
        relationships.clear();
        
        for (SchemaType sourceType : schema.values()) {
            for (SchemaField field : sourceType.getFields()) {
                // Skip scalar fields
                if (field.isScalar()) {
                    continue;
                }
                
                // Get the target type name
                String targetTypeName = field.getType()
                    .replace("[", "")
                    .replace("]", "")
                    .replace("!", "");
                
                // Check if target type exists in schema
                if (schema.containsKey(targetTypeName)) {
                    // Add relationship
                    relationships.add(new RelationshipInfo(
                        sourceType.getName(),
                        field.getName(),
                        targetTypeName,
                        field.isList()
                    ));
                }
            }
        }
        
        System.out.println("Detected " + relationships.size() + " relationships between types");
    }

    /**
     * Record an API request for metrics
     */
    private void recordRequest() {
        totalRequests.incrementAndGet();
        long now = System.currentTimeMillis();
        recentRequestTimestamps.add(now);
        
        // Clean up old timestamps
        synchronized(recentRequestTimestamps) {
            recentRequestTimestamps.removeIf(timestamp -> now - timestamp > RECENT_WINDOW_MS);
        }
    }
    
    /**
     * Calculate current requests per second
     */
    private double calculateRequestsPerSecond() {
        long now = System.currentTimeMillis();
        int recentCount;
        
        synchronized(recentRequestTimestamps) {
            // Clean up old timestamps
            recentRequestTimestamps.removeIf(timestamp -> now - timestamp > RECENT_WINDOW_MS);
            recentCount = recentRequestTimestamps.size();
        }
        
        // Calculate over the last minute
        return (double) recentCount / (RECENT_WINDOW_MS / 1000.0);
    }
    
    /**
     * Process a JSON query
     */
    @PostMapping("/query")
    public ResponseEntity<JsonNode> query(@RequestBody JsonNode request) {
        recordRequest();
        
        try {
            // Check if query processor is initialized
            if (queryProcessor == null) {
                return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Query processor not initialized"));
            }
            
            // Process query
            JsonNode resultNode = queryProcessor.processQuery(request);
            return ResponseEntity.ok(resultNode);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error processing query: " + e.getMessage()));
        }
    }
    
    /**
     * Process a GraphQL syntax query
     */
    @PostMapping("/graphql")
    public ResponseEntity<JsonNode> graphqlQuery(@RequestBody Map<String, String> request) {
        recordRequest();
        
        try {
            // Check if query processor is initialized
            if (queryProcessor == null) {
                return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Query processor not initialized"));
            }
            
            // Extract the GraphQL query
            String graphqlQuery = request.get("query");
            if (graphqlQuery == null || graphqlQuery.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("GraphQL query cannot be empty"));
            }
            
            // Transform GraphQL query to internal format
            JsonNode internalQuery = graphQLQueryTransformer.transformGraphQLQuery(graphqlQuery);
            
            // Process the transformed query
            JsonNode resultNode = queryProcessor.processQuery(internalQuery);
            
            // For debugging, include the transformed query in development mode
            if (isDevelopmentMode()) {
                ObjectNode responseWithQuery = objectMapper.createObjectNode();
                responseWithQuery.set("transformedQuery", internalQuery);
                responseWithQuery.set("result", resultNode);
                return ResponseEntity.ok(responseWithQuery);
            }
            
            return ResponseEntity.ok(resultNode);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error processing GraphQL query: " + e.getMessage()));
        }
    }
    
    /**
     * Check if running in development mode
     */
    private boolean isDevelopmentMode() {
        try {
            String activeProfile = System.getProperty("spring.profiles.active", "");
            return activeProfile.contains("dev") || activeProfile.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Process a time-travel query (point-in-time)
     */
    @PostMapping("/query/at")
    public ResponseEntity<JsonNode> queryAt(
            @RequestParam String timestamp, 
            @RequestBody JsonNode request) {
        recordRequest();
        
        try {
            // Parse timestamp
            Instant pointInTime = Instant.parse(timestamp);
            
            // Set time travel context
            timeTravel.setTimestamp(pointInTime);
            
            try {
                // Process query
                JsonNode resultNode = queryProcessor.processQuery(request);
                return ResponseEntity.ok(resultNode);
            } finally {
                // Reset time travel context
                timeTravel.resetTimestamp();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error processing time-travel query: " + e.getMessage()));
        }
    }
    
    /**
     * Natural language query endpoint (AI-powered)
     */
    @PostMapping("/nl-query")
    public ResponseEntity<JsonNode> naturalLanguageQuery(
            @RequestBody Map<String, String> request) {
        recordRequest();
        
        // Extract the natural language query
        String nlQuery = request.get("query");
        if (nlQuery == null || nlQuery.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Natural language query cannot be empty"));
        }
        
        // Check if AI query generator is available
        if (aiQueryGenerator == null) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("AI query generation is not available"));
        }
        
        try {
            // Get schema representation for AI
            String schemaForAI = schemaParser.getSchemaForAIPrompt();
            
            // Generate structured query from natural language
            JsonNode structuredQuery = aiQueryGenerator.generateQueryFromNL(nlQuery, schemaForAI);
            
            // Process the structured query
            JsonNode result = queryProcessor.processQuery(structuredQuery);
            
            // Create response with both the generated query and result
            ObjectNode response = objectMapper.createObjectNode();
            response.set("generatedQuery", structuredQuery);
            response.set("result", result);
            
            return ResponseEntity.ok(response);
        } catch (AIQueryException e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("AI query generation failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error processing natural language query: " + e.getMessage()));
        }
    }
    
    /**
     * Get schema information
     */
    @GetMapping("/schema")
    public ResponseEntity<JsonNode> getSchema() {
        recordRequest();
        
        try {
            Map<String, SchemaType> schema = schemaParser.getSchema();
            JsonNode schemaInfo = schemaParser.getDebugInfo();
            return ResponseEntity.ok(schemaInfo);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error getting schema: " + e.getMessage()));
        }
    }
    
    /**
     * Get data information
     */
    @GetMapping("/data-info")
    public ResponseEntity<JsonNode> getDataInfo() {
        recordRequest();
        
        try {
            Map<String, Object> dataInfo = dataLoader.getDebugInfo();
            JsonNode dataInfoNode = objectMapper.valueToTree(dataInfo);
            return ResponseEntity.ok(dataInfoNode);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error getting data info: " + e.getMessage()));
        }
    }
    
    /**
     * Get namespace information
     */
    @GetMapping("/namespaces")
    public ResponseEntity<JsonNode> getNamespaces() {
        recordRequest();
        
        try {
            ObjectNode result = objectMapper.createObjectNode();
            ArrayNode namespacesArray = objectMapper.createArrayNode();
            
            for (Map.Entry<String, Set<String>> entry : namespaceTypesMap.entrySet()) {
                ObjectNode namespaceNode = objectMapper.createObjectNode();
                namespaceNode.put("name", entry.getKey());
                
                ArrayNode typesArray = objectMapper.createArrayNode();
                for (String typeName : entry.getValue()) {
                    typesArray.add(typeName);
                }
                
                namespaceNode.set("types", typesArray);
                namespacesArray.add(namespaceNode);
            }
            
            result.set("namespaces", namespacesArray);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error getting namespace info: " + e.getMessage()));
        }
    }
    
    /**
     * Get relationship information
     */
    @GetMapping("/relationships")
    public ResponseEntity<JsonNode> getRelationships() {
        recordRequest();
        
        try {
            ObjectNode result = objectMapper.createObjectNode();
            ArrayNode relationshipsArray = objectMapper.createArrayNode();
            
            for (RelationshipInfo relationship : relationships) {
                ObjectNode relationshipNode = objectMapper.createObjectNode();
                relationshipNode.put("sourceType", relationship.getSourceType());
                relationshipNode.put("fieldName", relationship.getFieldName());
                relationshipNode.put("targetType", relationship.getTargetType());
                relationshipNode.put("isList", relationship.isList());
                relationshipsArray.add(relationshipNode);
            }
            
            result.set("relationships", relationshipsArray);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error getting relationship info: " + e.getMessage()));
        }
    }
    
    /**
     * Get history for a record
     */
    @GetMapping("/history/{typeName}/{id}")
    public ResponseEntity<JsonNode> getHistory(
            @PathVariable String typeName,
            @PathVariable String id) {
        recordRequest();
        
        try {
            List<Map<String, Object>> history = dataLoader.getHistoricalVersions(typeName, id);
            JsonNode historyNode = objectMapper.valueToTree(history);
            return ResponseEntity.ok(historyNode);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error getting record history: " + e.getMessage()));
        }
    }
    
    /**
     * Get health information
     */
    @GetMapping("/health")
    public ResponseEntity<JsonNode> getHealth() {
        recordRequest();
        
        try {
            ObjectNode healthInfo = objectMapper.createObjectNode();
            healthInfo.put("status", "UP");
            healthInfo.put("timestamp", Instant.now().toString());
            
            // Add component status
            ObjectNode components = objectMapper.createObjectNode();
            
            // Schema component
            ObjectNode schemaComponent = objectMapper.createObjectNode();
            schemaComponent.put("status", "UP");
            schemaComponent.put("types", schemaParser.getSchema().size());
            components.set("schema", schemaComponent);
            
            // Data component
            ObjectNode dataComponent = objectMapper.createObjectNode();
            dataComponent.put("status", "UP");
            dataComponent.put("sources", dataLoader.getDataTypes().size());
            components.set("data", dataComponent);
            
            healthInfo.set("components", components);
            
            // Add system information
            ObjectNode systemInfo = objectMapper.createObjectNode();
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemLoadAverage();
            if (cpuLoad < 0) cpuLoad = 0; // Fix for some systems that return negative values
            
            systemInfo.put("cpuLoad", cpuLoad);
            systemInfo.put("cpuLoadPercentage", String.format("%.2f%%", cpuLoad * 100));
            systemInfo.put("availableProcessors", osBean.getAvailableProcessors());
            systemInfo.put("osName", osBean.getName());
            systemInfo.put("osVersion", osBean.getVersion());
            systemInfo.put("osArch", osBean.getArch());
            
            healthInfo.set("system", systemInfo);
            
            // Add request metrics
            ObjectNode metrics = objectMapper.createObjectNode();
            metrics.put("totalRequests", totalRequests.get());
            metrics.put("requestsPerSecond", String.format("%.2f", calculateRequestsPerSecond()));
            
            healthInfo.set("metrics", metrics);
            
            return ResponseEntity.ok(healthInfo);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error getting health info: " + e.getMessage()));
        }
    }
    
    /**
     * WebSocket configuration
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/gql-websocket").withSockJS();
    }
    
    /**
     * WebSocket query handler
     */
    @MessageMapping("/query")
    @SendTo("/topic/results")
    public JsonNode handleWebSocketQuery(JsonNode query) throws Exception {
        recordRequest();
        return queryProcessor.processQuery(query);
    }
    
    /**
     * Create an error response
     */
    private JsonNode createErrorResponse(String message) {
        ObjectNode errorNode = objectMapper.createObjectNode();
        errorNode.put("error", message);
        return errorNode;
    }

    /**
     * Bean for SchemaParser
     */
    @Bean
    public SchemaParser schemaParser() {
        return new SchemaParser();
    }
}

