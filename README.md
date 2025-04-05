# GraphQL-inspired JSON API

A flexible, schema-driven API that provides GraphQL-like querying capabilities over JSON data sources with namespace support.

## Overview

This project implements a lightweight GraphQL-inspired API that reads data from various sources (JSON files or external APIs) based on a schema definition. It supports:

- Namespace-based data organization
- Field selection (like GraphQL)
- Advanced filtering with operators
- Pagination and sorting
- Schema-driven data retrieval
- Metadata introspection

## Features

- **Schema-Driven**: Define your data models in a GraphQL-like schema file
- **Multiple Data Sources**: Load data from JSON files or external APIs
- **Domain/Namespace Support**: Organize data into logical domains (Marketing, Finance, etc.)
- **Selective Query Results**: Request only the fields you need
- **Advanced Filtering**: Support for comparison operators, string operations, and list operations
- **Pagination**: Control result size with offset and limit parameters
- **Sorting**: Order results by any field in ascending or descending order
- **Schema Introspection**: Explore available types, fields and relationships through metadata endpoints

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+

### Installation

1. Clone the repository
2. Navigate to the project directory
3. Build the project:

```bash
mvn clean install
```

### Running the Application

```bash
mvn spring-boot:run
```

The server will start on port 8080 by default.

## Usage

### Schema Definition

The application uses a GraphQL-like schema file (`schema.graphql`) to define data models and their sources:

```graphql
type MarketingCustomer @source(file: "data/marketing.customer.json") {
  id: ID!
  name: String
  email: String
  orders: [MarketingOrder]
}
```

Special directives:
- `@source(file: "path/to/file.json")`: Specifies a JSON file data source
- `@api(url: "https://example.com/api")`: Specifies an external API data source
- `@params(fields: ["id"])`: Defines parameters for a type

### API Endpoints

#### Query Data: POST `/api/query`

Send queries to retrieve data:

```json
{
  "query": {
    "Marketing": {
      "customers": {
        "fields": ["id", "name", "email"],
        "where": { "id": "100" }
      }
    }
  }
}
```

#### Get Metadata: GET `/api/metadata`

Retrieve schema information including types, relationships, and namespaces.

#### Debug Endpoints:
- GET `/debug/datastore`: View loaded data sources
- GET `/debug/schema`: View schema structure

### Query Examples

#### Simple Collection Query

```json
{
  "query": {
    "collection": "MarketingCustomer",
    "fields": ["id", "name", "email"]
  }
}
```

#### Query with Pagination and Sorting

```json
{
  "query": {
    "collection": "MarketingOrder",
    "fields": ["id", "total", "date", "customerId"],
    "limit": 10,
    "offset": 0,
    "sortBy": "total",
    "sortOrder": "desc"
  }
}
```

#### Advanced Filtering Examples

```json
{
  "query": {
    "collection": "MarketingCustomer",
    "fields": ["id", "name", "email"],
    "where": {
      "name": { "$contains": "Alice" }
    }
  }
}
```

```json
{
  "query": {
    "collection": "MarketingOrder",
    "fields": ["id", "total", "date"],
    "where": {
      "total": { "$gt": "100" }
    }
  }
}
```

```json
{
  "query": {
    "collection": "MarketingCustomer",
    "fields": ["id", "name", "email"],
    "where": {
      "id": { "$in": ["100", "101"] }
    }
  }
}
```

#### Namespace Query with Field Selection and Filter

```json
{
  "query": {
    "Marketing": {
      "customers": {
        "fields": ["id", "name", "email"],
        "where": { "id": "100" }
      },
      "orders": {
        "fields": ["id", "total", "date"]
      }
    }
  }
}
```

#### Metadata Query

```json
{
  "query": {
    "metadata": {
      "fields": ["types", "namespaces"]
    }
  }
}
```

## Filter Operators

The API supports the following filter operators:

- `$eq`: Equal to (default when no operator is specified)
- `$ne`: Not equal to
- `$gt`: Greater than
- `$lt`: Less than
- `$gte`: Greater than or equal to
- `$lte`: Less than or equal to
- `$contains`: String contains
- `$startsWith`: String starts with
- `$endsWith`: String ends with 
- `$in`: Value exists in an array
- `$nin`: Value does not exist in an array

## Pagination

Control the number of results with:

- `limit`: Maximum number of results to return
- `offset`: Number of results to skip before returning

## Sorting

Order results with:

- `sortBy`: Field name to sort by
- `sortOrder`: `asc` for ascending (default) or `desc` for descending

## Project Structure

- `src/main/java/org/example/GQL.java`: Main application class
- `schema.graphql`: Schema definition file
- `data/`: Directory containing JSON data files
  - `marketing.customer.json`: Marketing customer data
  - `marketing.order.json`: Marketing order data
  - `finance.customer.json`: Finance customer data

## Customization

### Adding New Data Sources

1. Create a new JSON file in the `data` directory
2. Update the `schema.graphql` file to add a new type with appropriate directives
3. Restart the application

### Adding External API Sources

1. Add a new type to `schema.graphql` with the `@api` directive
2. Configure the API URL in the directive
3. Restart the application

### Adding New Namespaces

1. Define a new namespace type in `schema.graphql`
2. Add child types with appropriate source directives
3. Restart the application

## Data Relationships

The system automatically detects potential relationships between types based on field names. For example, a field named `customerId` in `MarketingOrder` will be recognized as a potential foreign key to a `MarketingCustomer` entity.

## License

This project is open source and available under the MIT License.
