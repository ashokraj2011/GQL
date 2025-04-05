# GraphQL-like JSON API

[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

A lightweight, schema-driven REST API providing GraphQL-like capabilities over JSON data sources.

## 🌟 Overview

This API enables GraphQL-like querying over various data sources without requiring a full GraphQL implementation. Built with Spring Boot, it's designed for rapid development and prototyping of data-driven applications.

```
┌─────────────┐     ┌────────────────┐     ┌──────────┐
│ Client App  │────▶│ GraphQL-like   │────▶│ JSON     │
│ or Service  │◀────│ JSON API       │◀────│ Data     │
└─────────────┘     └────────────────┘     └──────────┘
                           │                     ▲
                           ▼                     │
                    ┌──────────────┐     ┌──────────────┐
                    │ External     │     │ Database     │
                    │ APIs         │     │ (Optional)   │
                    └──────────────┘     └──────────────┘
```

## ✨ Key Features

- **📊 Schema-driven architecture**: Define your data model using GraphQL syntax
- **🔌 Multiple data sources**: Connect to JSON files, APIs, and databases
- **🔍 Rich querying capabilities**:
  - Field selection (GraphQL-style)
  - Advanced filtering (`$eq`, `$gt`, `$lt`, `$in`, etc.)
  - Pagination and sorting
  - Relationship resolution (automatic joins)
- **🗂️ Namespacing**: Organize entities by domain
- **⚡ Real-time updates**: WebSocket support
- **🚀 Performance optimizations**: Caching, indexing, and selective loading
- **📚 Self-documenting**: Rich metadata and introspection

## 🚀 Setup & Installation

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Quick Start

1. Clone the repository
   ```bash
   git clone https://github.com/yourusername/graphql-json-api.git
   cd graphql-json-api
   ```

2. Install dependencies
   ```bash
   mvn clean install
   ```

3. Run the service
   ```bash
   mvn spring-boot:run
   ```

4. Access the API at `http://localhost:8080/api`

### Configuration Options

| Property | Description | Default |
|----------|-------------|---------|
| `server.port` | HTTP port | 8080 |
| `source.type` | Data source type (file, db, api) | file |
| `schema.path` | Path to schema file | schema.graphql |
| `data.directory` | Data directory | data/ |

## 📐 Schema Definition

Create a `schema.graphql` file in the project root:

```graphql
# Define namespaces using the @params directive
type Marketing @params(fields: ["id"]) {
  customers: [MarketingCustomer]
  orders: [MarketingOrder]
}

# Use @source directive to specify data source
type MarketingCustomer @source(file: "data/marketing.customer.json") {
  id: ID!
  name: String
  email: String
  orders: [MarketingOrder]  # Relationship field
}

type MarketingOrder @source(file: "data/marketing.order.json") {
  id: ID!
  total: String
  customerId: ID            # Foreign key field
  date: String
  customer: MarketingCustomer  # Relationship field
}
```

### Available Directives

- `@source(file: "path/to/file.json")`: Specify JSON data source
- `@api(url: "https://api.example.com/resource")`: Specify external API source
- `@params(fields: ["field1", "field2"])`: Define namespace parameters
- `@deprecated(reason: "Use newField instead")`: Mark deprecated fields
- `@cached(seconds: 300)`: Cache field results

## 📂 Data Sources

Place your JSON data files in the `data/` directory:

```
data/
├── marketing.customer.json
├── marketing.order.json
├── marketing.campaign.json
├── finance.customer.json
└── ...
```

Each file should contain a JSON array of objects:

```json
[
  { "id": "100", "name": "Alice", "email": "alice@example.com" },
  { "id": "101", "name": "Bob", "email": "bob@example.com" }
]
```

## 📡 API Usage

### REST Endpoint

**POST /api/query**

#### Basic Query

```json
{
  "query": {
    "collection": "MarketingCustomer",
    "fields": ["id", "name", "email"]
  }
}
```

Response:
```json
{
  "data": {
    "MarketingCustomer": [
      { "id": "100", "name": "Alice", "email": "alice@example.com" },
      { "id": "101", "name": "Bob", "email": "bob@example.com" }
    ]
  }
}
```

#### Query with Filtering

```json
{
  "query": {
    "collection": "MarketingCustomer",
    "fields": ["id", "name", "email"],
    "where": {
      "name": { "$contains": "Al" }
    }
  }
}
```

Response:
```json
{
  "data": {
    "MarketingCustomer": [
      { "id": "100", "name": "Alice", "email": "alice@example.com" }
    ]
  }
}
```

#### Query with Relationship Resolution

```json
{
  "query": {
    "collection": "MarketingCustomer",
    "fields": ["id", "name"],
    "include": {
      "orders": {
        "fields": ["id", "total", "date"]
      }
    }
  }
}
```

Response:
```json
{
  "data": {
    "MarketingCustomer": [
      { 
        "id": "100", 
        "name": "Alice",
        "orders": [
          { "id": "1001", "total": "125.99", "date": "2023-10-15" },
          { "id": "1003", "total": "210.75", "date": "2023-10-18" }
        ]
      },
      { 
        "id": "101", 
        "name": "Bob",
        "orders": [
          { "id": "1002", "total": "89.50", "date": "2023-10-16" },
          { "id": "1004", "total": "45.25", "date": "2023-10-20" }
        ]
      }
    ]
  }
}
```

#### Advanced Query

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

#### Namespace Query

```json
{
  "query": {
    "Marketing": {
      "Customer": {
        "fields": ["id", "name", "email"]
      },
      "Order": {
        "fields": ["id", "total", "date"],
        "where": {
          "customerId": "100"
        }
      }
    }
  }
}
```

### WebSocket API

Connect to `/ws` endpoint using SockJS and STOMP:

```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, frame => {
  console.log('Connected: ' + frame);
  
  stompClient.subscribe('/topic/responses', response => {
    const data = JSON.parse(response.body);
    console.log(data);
  });
  
  stompClient.send("/app/querySocket", {}, 
    JSON.stringify({
      "query": {
        "collection": "MarketingCustomer",
        "fields": ["id", "name"]
      }
    })
  );
});
```

### Metadata & Introspection

**GET /api/metadata** - Returns schema information

Or query through the standard API:

```json
{
  "query": {
    "metadata": {
      "fields": ["types", "namespaces", "relationships"]
    }
  }
}
```

## 🔍 Filter Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `$eq` | Equals (default) | `{"id": {"$eq": "100"}}` or `{"id": "100"}` |
| `$ne` | Not equals | `{"status": {"$ne": "deleted"}}` |
| `$gt` | Greater than | `{"price": {"$gt": 50}}` |
| `$gte` | Greater than or equal | `{"price": {"$gte": 50}}` |
| `$lt` | Less than | `{"price": {"$lt": 100}}` |
| `$lte` | Less than or equal | `{"price": {"$lte": 100}}` |
| `$in` | In array | `{"status": {"$in": ["active", "pending"]}}` |
| `$nin` | Not in array | `{"status": {"$nin": ["deleted", "archived"]}}` |
| `$contains` | String contains | `{"name": {"$contains": "John"}}` |
| `$startsWith` | String starts with | `{"name": {"$startsWith": "J"}}` |
| `$endsWith` | String ends with | `{"email": {"$endsWith": "@example.com"}}` |

## 🛠️ Debug Endpoints

- **GET /debug/datastore** - View loaded data sources
- **GET /debug/schema** - View schema structure
- **GET /debug/relationships** - View relationship mappings

## ⚡ Performance Optimizations

- **Query caching**: Common query results are cached
- **Indexing**: Relationships are optimized with in-memory indexes
- **Field selection**: Only requested fields are returned
- **Pagination**: Prevents large result sets
- **Lazy loading**: Related entities are loaded only when requested

## 📊 Database Integration

To use a database instead of JSON files:

1. Configure your data source in `application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/yourdb
   spring.datasource.username=postgres
   spring.datasource.password=password
   ```

2. Set the source type:
   ```
   -Dsource.type=db
   ```

3. The API will use JPA repositories for data access

## 🔧 Troubleshooting

**API returns empty results**
- Verify JSON files exist in the data/ directory
- Check that file names match schema definitions
- Validate JSON syntax in data files

**Relationship resolution not working**
- Ensure foreign key fields follow naming conventions (`entityId`)
- Check that both entity types are defined in schema
- Verify foreign key values exist in related entity

**Performance issues**
- Use field selection to request only needed fields
- Add pagination for large collections
- Check for circular relationship dependencies

## 👥 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
