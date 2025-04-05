# GraphQL-like JSON API

## Overview
This API provides GraphQL-like querying capabilities over JSON files and external services without requiring a full GraphQL implementation. It's built with Spring Boot and designed for quick prototyping and development of data-driven applications.

## Key Features
- **Schema-driven architecture**: Define your data model using GraphQL syntax
- **Flexible data sources**: Load data from JSON files or external APIs
- **Rich querying capabilities**:
  - Field selection (like GraphQL)
  - Filtering with multiple operators (`$eq`, `$gt`, `$lt`, `$in`, etc.)
  - Pagination and sorting
  - Relationship resolution (automatic joins between entities)
- **Namespacing**: Organize entities by domain/namespace
- **Real-time capabilities**: WebSocket support for live updates
- **Performance optimizations**: Caching, selective loading, and indexing
- **Self-documenting**: Rich metadata and introspection endpoints

## Setup & Installation

### Prerequisites
- Java 17+
- Maven

### Quick Start
1. Clone the repository
2. Install dependencies:
   ```bash
   mvn clean install
   ```
3. Run the service:
   ```bash
   mvn spring-boot:run
   ```
4. The API runs on port 8080 by default

## Schema Definition
Create a `schema.graphql` file in the project root with your data model:

```graphql
# Define namespaces and entities
type Marketing {
  customers: [MarketingCustomer]
  orders: [MarketingOrder]
}

# Use directives to specify data sources
type MarketingCustomer @source(file: "data/marketing.customer.json") {
  id: ID!
  name: String
  email: String
  orders: [MarketingOrder]
}

type MarketingOrder @source(file: "data/marketing.order.json") {
  id: ID!
  total: String
  customerId: ID
  customer: MarketingCustomer
}
```

## Data Sources
Place your JSON data files in the `data/` directory:
- Each file should contain an array of objects
- File names should match the schema definition (e.g., `marketing.customer.json`)

## API Usage

### REST API
**POST /api/query**

Basic query:
```json
{
  "query": {
    "collection": "MarketingCustomer",
    "fields": ["id", "name", "email"]
  }
}
```

Query with filtering:
```json
{
  "query": {
    "collection": "MarketingCustomer",
    "fields": ["id", "name"],
    "where": { 
      "id": { "$in": ["100", "101"] },
      "name": { "$contains": "Al" }
    }
  }
}
```

Query with relationship inclusion:
```json
{
  "query": {
    "collection": "MarketingCustomer",
    "fields": ["id", "name"],
    "include": {
      "orders": ["id", "total", "date"]
    }
  }
}
```

Advanced query with multiple features:
```json
{
  "query": {
    "collection": "MarketingOrder",
    "fields": ["id", "total", "date"],
    "where": {
      "total": { "$gt": "90.00" }
    },
    "include": {
      "customer": {
        "fields": ["name", "email"]
      }
    },
    "sortBy": "date",
    "sortOrder": "desc",
    "limit": 10,
    "offset": 0
  }
}
```

Namespace-specific query:
```json
{
  "query": {
    "Marketing": {
      "Customer": {
        "fields": ["id", "name", "email"]
      }
    }
  }
}
```

### WebSocket API
Connect to `/ws` endpoint using SockJS and STOMP:
- Send queries to `/app/querySocket`
- Receive results on `/topic/responses`

### Metadata & Introspection
**GET /api/metadata** - Returns schema information

**POST /api/query**
```json
{
  "query": {
    "metadata": {
      "fields": ["types", "namespaces", "relationships"]
    }
  }
}
```

### Debug Endpoints
- **GET /debug/datastore** - View loaded data sources
- **GET /debug/schema** - View schema structure
- **GET /debug/relationships** - View relationship mappings

## Filter Operators
- `$eq`: Equal to (default)
- `$ne`: Not equal to
- `$gt`: Greater than
- `$lt`: Less than
- `$gte`: Greater than or equal to
- `$lte`: Less than or equal to
- `$in`: In array
- `$nin`: Not in array
- `$contains`: String contains
- `$startsWith`: String starts with
- `$endsWith`: String ends with

## Performance Optimizations
- Query results are cached
- Relationships are optimized with indexes
- Field selection minimizes data transfer
- Pagination prevents large result sets

## Database Integration
1. Add JPA/Mongo dependencies in your pom.xml.
2. Define environment variable: source.type=db
3. Implement database access methods in GQL.

## Custom Directives
- @deprecated
- @cached

## License
Open source under the MIT License.
