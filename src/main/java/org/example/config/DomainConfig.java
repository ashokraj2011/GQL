package org.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuration for domain-specific settings
 */
@Configuration
@ConfigurationProperties(prefix = "domain")
public class DomainConfig {
    
    // List of known namespaces (can be configured in application.properties)
    private List<String> namespaces = new ArrayList<>();
    
    // Common entity type suffixes (can be configured in application.properties)
    private List<String> entityTypeSuffixes = new ArrayList<>();
    
    // Cached capitalized namespace prefixes
    private List<String> capitalizedPrefixes;
    
    /**
     * Default constructor that initializes with common defaults
     */
    public DomainConfig() {
        // Initialize with common defaults if not specified in config
        if (namespaces.isEmpty()) {
            namespaces.add("marketing");
            namespaces.add("finance");
            namespaces.add("external");
            namespaces.add("analytics");
            namespaces.add("product");
        }
        
        if (entityTypeSuffixes.isEmpty()) {
            entityTypeSuffixes.add("Customer");
            entityTypeSuffixes.add("Order");
            entityTypeSuffixes.add("Campaign");
            entityTypeSuffixes.add("Lead");
            entityTypeSuffixes.add("Event");
            entityTypeSuffixes.add("Invoice");
            entityTypeSuffixes.add("Transaction");
            entityTypeSuffixes.add("User");
            entityTypeSuffixes.add("Product");
        }
    }

    public List<String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(List<String> namespaces) {
        this.namespaces = namespaces;
        // Reset cached capitalized prefixes
        this.capitalizedPrefixes = null;
    }

    public List<String> getEntityTypeSuffixes() {
        return entityTypeSuffixes;
    }

    public void setEntityTypeSuffixes(List<String> entityTypeSuffixes) {
        this.entityTypeSuffixes = entityTypeSuffixes;
    }
    
    /**
     * Get namespace prefixes in capitalized form (e.g., "Marketing" from "marketing")
     * Used for type name matching
     * 
     * @return List of capitalized namespace prefixes
     */
    public List<String> getCapitalizedNamespacePrefixes() {
        if (capitalizedPrefixes == null) {
            capitalizedPrefixes = namespaces.stream()
                .map(ns -> ns.substring(0, 1).toUpperCase() + ns.substring(1))
                .collect(Collectors.toList());
        }
        return capitalizedPrefixes;
    }
    
    /**
     * Extracts namespace prefix from a type name if present
     * 
     * @param typeName The type name to check
     * @return The namespace, or null if no namespace prefix found
     */
    public String extractNamespaceFromTypeName(String typeName) {
        for (String prefix : getCapitalizedNamespacePrefixes()) {
            if (typeName.startsWith(prefix)) {
                return prefix.toLowerCase();
            }
        }
        return null;
    }
    
    /**
     * Checks if a type name belongs to a specific namespace
     * 
     * @param typeName Type name to check
     * @param namespace Namespace to check against
     * @return true if the type belongs to the namespace
     */
    public boolean typeNameBelongsToNamespace(String typeName, String namespace) {
        String capitalized = namespace.substring(0, 1).toUpperCase() + namespace.substring(1);
        return typeName.startsWith(capitalized);
    }
    
    /**
     * Derives standard type name by combining namespace and entity type
     * 
     * @param namespace Namespace
     * @param entityType Entity type
     * @return Combined type name (e.g., "MarketingCustomer")
     */
    public String deriveTypeName(String namespace, String entityType) {
        String capitalized = namespace.substring(0, 1).toUpperCase() + namespace.substring(1);
        return capitalized + entityType;
    }
    
    /**
     * Extract namespace prefix from a type name based on configured namespace prefixes
     * 
     * @param typeName The type name to analyze
     * @return The lowercase namespace or null if no match found
     */
    public String extractNamespacePrefix(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return null;
        }
        
        // Try to match with configured namespace prefixes
        for (String prefix : getCapitalizedNamespacePrefixes()) {
            if (typeName.startsWith(prefix)) {
                return prefix.toLowerCase();
            }
        }
        
        // Try to extract namespace from camelCase format (e.g., marketingCustomer -> marketing)
        if (typeName.length() > 1) {
            int firstUpperCaseIndex = -1;
            for (int i = 1; i < typeName.length(); i++) {
                if (Character.isUpperCase(typeName.charAt(i))) {
                    firstUpperCaseIndex = i;
                    break;
                }
            }
            
            if (firstUpperCaseIndex > 0) {
                String potentialNamespace = typeName.substring(0, firstUpperCaseIndex).toLowerCase();
                if (namespaces.contains(potentialNamespace)) {
                    return potentialNamespace;
                }
            }
        }
        
        return null;
    }
}
