package org.example.ai;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for AI-assisted query generation.
 * Implementations should connect to AI providers to generate GQL queries
 * from natural language descriptions.
 */
public interface AIQueryGenerator {
    
    /**
     * Generate a GQL query from a natural language description
     * 
     * @param description The natural language description of what data is needed
     * @return A JsonNode containing the generated query
     * @throws AIQueryException If there was an error generating the query
     */
    JsonNode generateQuery(String description) throws AIQueryException;
    
    /**
     * Generate a GQL query with schema context
     * 
     * @param description The natural language description of what data is needed
     * @param schemaContext Schema information to help guide the generation
     * @return A JsonNode containing the generated query
     * @throws AIQueryException If there was an error generating the query
     */
    JsonNode generateQuery(String description, String schemaContext) throws AIQueryException;
    
    /**
     * Generate a GQL query with additional context/examples
     * 
     * @param description The natural language description of what data is needed
     * @param examples Additional examples to help guide the generation
     * @return A JsonNode containing the generated query
     * @throws AIQueryException If there was an error generating the query
     */
    JsonNode generateQueryWithExamples(String description, JsonNode examples) throws AIQueryException;
    
    /**
     * Generate a GQL query from natural language with schema context
     * 
     * @param nlQuery The natural language query describing what data is needed
     * @param schemaContext Schema information to help guide the generation
     * @return A JsonNode containing the generated query
     * @throws AIQueryException If there was an error generating the query
     */
    JsonNode generateQueryFromNL(String nlQuery, String schemaContext) throws AIQueryException;
    
    /**
     * Get the name of the AI provider being used
     * 
     * @return The provider name (e.g., "OpenAI", "Claude", etc.)
     */
    String getProviderName();
}
