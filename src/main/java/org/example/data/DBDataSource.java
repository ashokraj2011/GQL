package org.example.data;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for database-sourced data
 */
public interface DBDataSource {
    /**
     * Gets the entity class name this data source provides
     */
    String getEntityType();
    
    /**
     * Gets all entities as maps
     */
    List<Map<String, Object>> getAllEntities();
    
    /**
     * Gets entity by ID
     */
    Optional<Map<String, Object>> getEntityById(String id);
    
    /**
     * Gets entities by field value
     */
    List<Map<String, Object>> getEntitiesByField(String fieldName, Object value);
}
