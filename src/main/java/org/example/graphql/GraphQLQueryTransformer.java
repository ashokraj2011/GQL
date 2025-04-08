package org.example.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.language.*;
import graphql.parser.Parser;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Transforms GraphQL syntax queries into the internal JSON query format
 */
public class GraphQLQueryTransformer {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Transform a GraphQL syntax query to internal JSON query format
     *
     * @param graphqlQuery The GraphQL query string
     * @return JsonNode representing the internal query format
     */
    public JsonNode transformGraphQLQuery(String graphqlQuery) {
        try {
            // Parse the GraphQL query
            Document document = Parser.parse(graphqlQuery);
            
            // Find the first operation definition (query)
            OperationDefinition operation = document.getDefinitions().stream()
                .filter(def -> def instanceof OperationDefinition)
                .map(def -> (OperationDefinition) def)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No query operation found"));
            
            // Create the root query object
            ObjectNode rootQueryNode = objectMapper.createObjectNode();
            ObjectNode queryNode = rootQueryNode.putObject("query");
            
            // Process all selections in the query
            List<Selection> selections = operation.getSelectionSet().getSelections();
            for (Selection selection : selections) {
                if (selection instanceof Field) {
                    Field field = (Field) selection;
                    processQueryField(field, queryNode);
                }
            }
            
            return rootQueryNode;
        } catch (Exception e) {
            throw new RuntimeException("Error transforming GraphQL query: " + e.getMessage(), e);
        }
    }
    
    /**
     * Process a field in the GraphQL query
     */
    private void processQueryField(Field field, ObjectNode parentNode) {
        String fieldName = field.getName();
        SelectionSet selectionSet = field.getSelectionSet();
        
        // Handle namespace or entity queries
        if (selectionSet != null && !selectionSet.getSelections().isEmpty()) {
            // This is either a namespace or entity with subfields
            ObjectNode fieldNode = parentNode.putObject(fieldName);
            
            // Process arguments if any
            if (field.getArguments() != null && !field.getArguments().isEmpty()) {
                processArguments(field.getArguments(), fieldNode);
            }
            
            // Process subfields
            for (Selection subSelection : selectionSet.getSelections()) {
                if (subSelection instanceof Field) {
                    Field subField = (Field) subSelection;
                    processQueryField(subField, fieldNode);
                }
            }
        } else {
            // This is a simple field, add to fields array
            ensureFieldsArray(parentNode).add(fieldName);
        }
    }
    
    /**
     * Process arguments in a GraphQL query (where conditions, etc.)
     */
    private void processArguments(List<Argument> arguments, ObjectNode parentNode) {
        for (Argument arg : arguments) {
            String argName = arg.getName();
            Value<?> value = arg.getValue();
            
            // Handle different types of arguments
            switch (argName) {
                case "where":
                    // Where conditions
                    if (value instanceof ObjectValue) {
                        ObjectNode whereNode = parentNode.putObject("where");
                        processWhereConditions((ObjectValue) value, whereNode);
                    }
                    break;
                case "limit":
                    // Pagination limit
                    if (value instanceof IntValue) {
                        int limit = ((IntValue) value).getValue().intValue();
                        ensurePaginationNode(parentNode).put("limit", limit);
                    }
                    break;
                case "offset":
                    // Pagination offset
                    if (value instanceof IntValue) {
                        int offset = ((IntValue) value).getValue().intValue();
                        ensurePaginationNode(parentNode).put("offset", offset);
                    }
                    break;
                default:
                    // Handle other arguments
                    if (value instanceof StringValue) {
                        parentNode.put(argName, ((StringValue) value).getValue());
                    } else if (value instanceof IntValue) {
                        parentNode.put(argName, ((IntValue) value).getValue().intValue());
                    } else if (value instanceof BooleanValue) {
                        parentNode.put(argName, ((BooleanValue) value).isValue());
                    }
                    break;
            }
        }
    }
    
    /**
     * Process where conditions
     */
    private void processWhereConditions(ObjectValue objectValue, ObjectNode whereNode) {
        for (ObjectField field : objectValue.getObjectFields()) {
            String fieldName = field.getName();
            Value<?> value = field.getValue();
            
            if (value instanceof StringValue) {
                whereNode.put(fieldName, ((StringValue) value).getValue());
            } else if (value instanceof IntValue) {
                whereNode.put(fieldName, ((IntValue) value).getValue().intValue());
            } else if (value instanceof BooleanValue) {
                whereNode.put(fieldName, ((BooleanValue) value).isValue());
            } else if (value instanceof NullValue) {
                whereNode.putNull(fieldName);
            } else if (value instanceof ObjectValue) {
                // Handle operators like $gt, $lt
                ObjectNode operatorNode = whereNode.putObject(fieldName);
                for (ObjectField opField : ((ObjectValue) value).getObjectFields()) {
                    String operator = opField.getName();
                    Value<?> opValue = opField.getValue();
                    
                    // Convert operator to internal format ($gt -> $gt)
                    if (value instanceof IntValue) {
                        operatorNode.put(operator, ((IntValue) opValue).getValue().intValue());
                    } else if (value instanceof FloatValue) {
                        operatorNode.put(operator, ((FloatValue) opValue).getValue().doubleValue());
                    } else if (value instanceof StringValue) {
                        operatorNode.put(operator, ((StringValue) opValue).getValue());
                    }
                }
            }
        }
    }
    
    /**
     * Ensure a fields array exists in the node
     */
    private ArrayNode ensureFieldsArray(ObjectNode node) {
        if (!node.has("fields")) {
            return node.putArray("fields");
        }
        return (ArrayNode) node.get("fields");
    }
    
    /**
     * Ensure a pagination node exists
     */
    private ObjectNode ensurePaginationNode(ObjectNode node) {
        if (!node.has("pagination")) {
            return node.putObject("pagination");
        }
        return (ObjectNode) node.get("pagination");
    }
}
