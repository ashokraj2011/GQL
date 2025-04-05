package org.example.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.schema.SchemaType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Responsible for loading data from various sources (JSON files, APIs, etc.)
 * and making it available to the query processor.
 */
public class DataLoader {
    private final String dataDirectory;
    private final ObjectMapper objectMapper;
    
    // In-memory data store: type name -> list of records
    private final Map<String, List<Map<String, Object>>> dataStore = new HashMap<>();
    
    // Indexes for faster lookup: type -> field -> value -> record
    private final Map<String, Map<String, Map<Object, List<Map<String, Object>>>>> indexes = new HashMap<>();

    /**
     * Creates a new DataLoader with the given data directory
     * 
     * @param dataDirectory Directory containing JSON data files
     */
    public DataLoader(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.objectMapper = new ObjectMapper();
        
        // Create data directory if it doesn't exist
        Path path = Paths.get(dataDirectory);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                System.out.println("Created data directory: " + path.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Failed to create data directory: " + e.getMessage());
            }
        }
    }

    /**
     * Loads data for all types specified in the schema
     * 
     * @param schema The schema containing type definitions
     * @throws IOException If an error occurs while loading data
     */
    public void loadData(Map<String, SchemaType> schema) throws IOException {
        System.out.println("Loading data from directory: " + Paths.get(dataDirectory).toAbsolutePath());
        
        for (SchemaType type : schema.values()) {
            String sourceFile = type.getSourceFile();
            String apiUrl = type.getApiUrl();
            
            if (sourceFile != null) {
                // Load from JSON file
                loadFromFile(type.getName(), sourceFile);
                
                // Build indexes for the type
                buildIndexes(type.getName());
            } else if (apiUrl != null) {
                // TODO: Load from API when needed (not implementing yet)
                System.out.println("API data sources not implemented yet: " + apiUrl);
            }
        }
        
        System.out.println("Loaded data: " + dataStore.size() + " types");
        for (Map.Entry<String, List<Map<String, Object>>> entry : dataStore.entrySet()) {
            System.out.println("  - " + entry.getKey() + ": " + entry.getValue().size() + " records");
        }
    }

    /**
     * Loads data from a JSON file
     * 
     * @param typeName The type name for the data
     * @param sourceFile The source file path (relative to data directory)
     * @throws IOException If an error occurs while reading the file
     */
    private void loadFromFile(String typeName, String sourceFile) throws IOException {
        // Handle both absolute paths and relative paths
        Path filePath;
        if (Paths.get(sourceFile).isAbsolute()) {
            filePath = Paths.get(sourceFile);
        } else {
            // Try multiple path resolutions to find the file
            List<Path> candidatePaths = new ArrayList<>();
            
            // 1. Direct path relative to data directory
            candidatePaths.add(Paths.get(dataDirectory, sourceFile));
            
            // 2. Path without 'data/' prefix if it's already in the sourceFile
            if (sourceFile.startsWith("data/")) {
                String pathWithoutDataPrefix = sourceFile.substring(5);
                candidatePaths.add(Paths.get(dataDirectory, pathWithoutDataPrefix));
                // Also try just the path as specified
                candidatePaths.add(Paths.get(sourceFile));
            }
            
            // 3. With additional directory levels
            for (String additionalPath : Arrays.asList("gql/", "")) {
                candidatePaths.add(Paths.get(additionalPath + sourceFile));
                candidatePaths.add(Paths.get(additionalPath + dataDirectory, sourceFile));
            }
            
            // Try each path until we find one that exists
            filePath = null;
            for (Path candidatePath : candidatePaths) {
                if (Files.exists(candidatePath)) {
                    filePath = candidatePath;
                    System.out.println("Found data file at: " + filePath);
                    break;
                }
            }
            
            // If none exists, use the original path and log a warning
            if (filePath == null) {
                System.out.println("Tried paths: " + candidatePaths);
                filePath = Paths.get(dataDirectory, sourceFile);
            }
        }
        
        if (!Files.exists(filePath)) {
            System.out.println("Warning: Data file not found: " + filePath);
            System.out.println("Absolute path: " + filePath.toAbsolutePath());
            dataStore.put(typeName, new ArrayList<>());
            return;
        }
        
        try {
            // Read the file and parse as a list of maps
            List<Map<String, Object>> records = objectMapper.readValue(
                filePath.toFile(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            // Store the data
            dataStore.put(typeName, records);
            
            System.out.println("Loaded " + records.size() + " records for type " + typeName + " from " + filePath);
        } catch (IOException e) {
            System.err.println("Error loading data for " + typeName + " from " + filePath + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Build indexes for a type to speed up queries
     * 
     * @param typeName The type name to index
     */
    private void buildIndexes(String typeName) {
        List<Map<String, Object>> records = dataStore.get(typeName);
        if (records == null || records.isEmpty()) {
            return;
        }
        
        // Create indexes map for this type if it doesn't exist
        Map<String, Map<Object, List<Map<String, Object>>>> typeIndexes = new HashMap<>();
        indexes.put(typeName, typeIndexes);
        
        // Index the ID field by default
        buildIndexForField(typeName, "id");
        
        // Index fields that are likely to be used in joins
        Set<String> potentialJoinFields = records.get(0).keySet().stream()
            .filter(field -> field.toLowerCase().endsWith("id"))
            .collect(Collectors.toSet());
        
        for (String field : potentialJoinFields) {
            buildIndexForField(typeName, field);
        }
    }

    /**
     * Build an index for a specific field of a type
     * 
     * @param typeName The type name
     * @param fieldName The field name to index
     */
    private void buildIndexForField(String typeName, String fieldName) {
        List<Map<String, Object>> records = dataStore.get(typeName);
        if (records == null || records.isEmpty()) {
            return;
        }
        
        // Create field index map
        Map<Object, List<Map<String, Object>>> fieldIndex = new HashMap<>();
        indexes.get(typeName).put(fieldName, fieldIndex);
        
        // Group records by field value
        for (Map<String, Object> record : records) {
            Object value = record.get(fieldName);
            if (value != null) {
                fieldIndex.computeIfAbsent(value, k -> new ArrayList<>()).add(record);
            }
        }
    }

    /**
     * Gets data for a specific type
     * 
     * @param typeName The type name
     * @return List of records for the type, or empty list if not found
     */
    public List<Map<String, Object>> getData(String typeName) {
        return dataStore.getOrDefault(typeName, Collections.emptyList());
    }

    /**
     * Gets a record by ID for a specific type
     * 
     * @param typeName The type name
     * @param id The ID value
     * @return The record, or null if not found
     */
    public Map<String, Object> getRecordById(String typeName, Object id) {
        Map<String, Map<Object, List<Map<String, Object>>>> typeIndexes = indexes.get(typeName);
        if (typeIndexes != null && typeIndexes.containsKey("id")) {
            List<Map<String, Object>> records = typeIndexes.get("id").get(id);
            if (records != null && !records.isEmpty()) {
                return records.get(0);
            }
        }
        
        // Fallback to linear search if no index
        List<Map<String, Object>> allRecords = dataStore.getOrDefault(typeName, Collections.emptyList());
        return allRecords.stream()
            .filter(record -> Objects.equals(record.get("id"), id))
            .findFirst()
            .orElse(null);
    }

    /**
     * Gets records by field value
     * 
     * @param typeName The type name
     * @param fieldName The field name
     * @param value The field value to match
     * @return List of matching records
     */
    public List<Map<String, Object>> getRecordsByField(String typeName, String fieldName, Object value) {
        Map<String, Map<Object, List<Map<String, Object>>>> typeIndexes = indexes.get(typeName);
        if (typeIndexes != null && typeIndexes.containsKey(fieldName)) {
            List<Map<String, Object>> records = typeIndexes.get(fieldName).get(value);
            if (records != null) {
                return records;
            }
        }
        
        // Fallback to filter if no index
        List<Map<String, Object>> allRecords = dataStore.getOrDefault(typeName, Collections.emptyList());
        return allRecords.stream()
            .filter(record -> Objects.equals(record.get(fieldName), value))
            .collect(Collectors.toList());
    }

    /**
     * Gets the names of all data types that have been loaded
     * 
     * @return Set of type names
     */
    public Set<String> getDataTypes() {
        return Collections.unmodifiableSet(dataStore.keySet());
    }

    /**
     * Provides debug information about the data loader
     * 
     * @return Map containing debug information
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> debugInfo = new HashMap<>();
        
        // Add data store sizes
        Map<String, Integer> dataSizes = new HashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : dataStore.entrySet()) {
            dataSizes.put(entry.getKey(), entry.getValue().size());
        }
        debugInfo.put("dataSize", dataSizes);
        
        // Add index information
        Map<String, Map<String, Integer>> indexInfo = new HashMap<>();
        for (Map.Entry<String, Map<String, Map<Object, List<Map<String, Object>>>>> typeEntry : indexes.entrySet()) {
            String typeName = typeEntry.getKey();
            Map<String, Integer> fieldIndexSizes = new HashMap<>();
            
            for (Map.Entry<String, Map<Object, List<Map<String, Object>>>> fieldEntry : typeEntry.getValue().entrySet()) {
                fieldIndexSizes.put(fieldEntry.getKey(), fieldEntry.getValue().size());
            }
            
            indexInfo.put(typeName, fieldIndexSizes);
        }
        debugInfo.put("indexes", indexInfo);
        
        // Add sample data (first record of each type)
        Map<String, Object> samples = new HashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : dataStore.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                samples.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        debugInfo.put("samples", samples);
        
        return debugInfo;
    }
}
