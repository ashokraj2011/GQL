package org.example.timetravel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.data.DataLoader;
import org.example.query.QueryProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service for executing time travel queries against historical data
 */
@Service
public class TimeTravel {

    @Autowired
    private DataLoader dataLoader;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public TimeTravel() {
        // Default constructor for Spring
    }

    /**
     * Execute a query against data as it existed at a specific point in time
     *
     * @param query The query to execute
     * @param timestamp The timestamp at which to view the data
     * @param queryProcessor The query processor to use
     * @return The query results based on historical data
     */
    public JsonNode executeQueryAt(JsonNode query, Instant timestamp, QueryProcessor queryProcessor) {
        // Set the point-in-time context on the data loader
        dataLoader.setTimestamp(timestamp);
        
        try {
            // Execute the query with the historical context
            return queryProcessor.processQuery(query);
        } finally {
            // Always reset the timestamp to avoid affecting subsequent queries
            dataLoader.resetTimestamp();
        }
    }

    /**
     * Process a query for data at a specific point in time
     * 
     * @param query The query to execute
     * @param timestamp The timestamp at which to view the data
     * @param queryProcessor The query processor to use
     * @return The query results based on historical data
     */
    public JsonNode processQueryAtTime(JsonNode query, Instant timestamp, QueryProcessor queryProcessor) {
        // Set the point-in-time context on the data loader
        dataLoader.setTimestamp(timestamp);
        
        try {
            // Execute the query with the historical context
            return queryProcessor.processQuery(query);
        } finally {
            // Always reset the timestamp to avoid affecting subsequent queries
            dataLoader.resetTimestamp();
        }
    }

    /**
     * Get historical versions of a specific record
     *
     * @param typeName The type name
     * @param id The record ID
     * @return A list of all historical versions of the record
     */
    public JsonNode getHistoricalVersions(String typeName, String id) {
        return objectMapper.valueToTree(dataLoader.getHistoricalVersions(typeName, id));
    }
    
    /**
     * Get record history for a specific record
     *
     * @param typeName The type name
     * @param id The record ID
     * @return A list of all historical versions of the record
     */
    public List<Map<String, Object>> getRecordHistory(String typeName, String id) {
        return dataLoader.getHistoricalVersions(typeName, id);
    }
}
