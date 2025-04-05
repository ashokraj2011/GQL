package org.example.ai;

/**
 * Exception thrown when there's an error generating a query via AI
 */
public class AIQueryException extends Exception {
    
    public AIQueryException(String message) {
        super(message);
    }
    
    public AIQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
