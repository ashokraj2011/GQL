# GraphQL-like JSON API

[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

**Author:** Ashok Raj (ashok.nair.raj@gmail.com)

A lightweight, schema-driven REST API providing GraphQL-like capabilities over JSON data sources with intelligent namespace handling and relationship resolution.

## 🌟 Overview

This API enables GraphQL-like querying over various data sources without requiring a full GraphQL implementation. Built with Spring Boot, it's designed for rapid development and prototyping of data-driven applications with a focus on domain-driven design.

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

## 🔄 Advantages Over Pure GraphQL

### 💡 Simplified Implementation
- **Lower Learning Curve**: Uses standard JSON for queries with familiar syntax
- **JSON-Native**: Both queries and responses use standard JSON
- **Familiar REST Endpoints**: Compatible with standard HTTP clients and tools

### 🚀 Performance Benefits
- **Optimized for JSON Sources**: Direct mapping between source files and API responses
- **Intelligent Caching**: Namespace-aware caching with directive-based configuration
- **Smart Indexing**: Automatic index creation for faster lookups on common fields

### 🧩 Domain-Oriented Design
- **Strong Namespacing**: Organize data by business domain (Marketing, Finance, etc.)
- **Integrated Relationships**: Cross-domain relationships with automatic resolution
- **Domain Isolation**: Clear boundaries between business concerns

### 🔧 Operational Advantages
- **File-Based Development**: Simple JSON files for rapid prototyping
- **Self-Documenting API**: Rich metadata and schema introspection
- **Flexible Deployment**: Run as standalone API or embed in existing applications

## ✨ Key Features

- **📊 Schema-driven architecture**: Define your data model using GraphQL syntax
- **🔍 Rich querying capabilities**:
  - Field selection (specify exactly what you need)
  - Filtering with powerful conditions
  - Relationship traversal across domains
  - Namespace-based organization
- **🔌 Multiple data sources**: Connect to:
  - JSON files (built-in)
  - External APIs (via `@api` directive)
  - Databases (future extension)
- **🗂️ Intelligent namespacing**: Query entities across domains with consistent access
- **⚡ WebSocket support**: Real-time query capabilities
- **🔍 Natural language queries**: Optional AI-powered query generation

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
| `schema.path` | Path to schema file | schema.graphql |
| `data.directory` | Data directory | data/ |

## 📐 Schema Definition

The schema uses GraphQL syntax with custom directives. Create a `schema.graphql` file like this:

```graphql
# Custom directives to specify data sources
directive @source(file: String) on OBJECT
directive @api(url: String) on OBJECT
directive @params(fields: [String!]) on OBJECT
directive @deprecated(reason: String) on FIELD_DEFINITION
directive @cached(seconds: Int) on FIELD_DEFINITION

# Marketing domain types
type Marketing @params(fields: ["id"]) {
  id: ID
  customers: [MarketingCustomer]
  orders: [MarketingOrder]
  campaigns: [MarketingCampaign]
}

type MarketingCustomer @source(file: "data/marketing.customer.json") {
  id: ID!
  name: String
  email: String
  orders: [MarketingOrder]
  campaignHistory: [MarketingCampaign]
}

type MarketingOrder @source(file: "data/marketing.order.json") {
  id: ID!
  total: String
  customerId: ID
  date: String
  customer: MarketingCustomer
  campaignId: ID
  campaign: MarketingCampaign
}

# Finance domain types
type Finance @params(fields: ["id"]) {
  id: ID
  customers: [FinanceCustomer]
}

type FinanceCustomer @source(file: "data/finance.customer.json") {
  id: ID!
  name: String
  email: String
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

### Basic Query Structure

The API accepts JSON queries with the following structure:

```json
{
  "query": {
    "namespace": {
      "entityType": {
        "fields": ["field1", "field2"],
        "where": { "fieldName": "value" }
      }
    }
  }
}
```

### Example Queries

#### Simple Collection Query

```json
{
  "query": {
    "marketingCustomers": {
      "fields": ["id", "name", "email"]
    }
  }
}
```

#### Namespace Query

```json
{
  "query": {
    "marketing": {
      "customers": {
        "fields": ["id", "name", "email"]
      },
      "orders": {
        "fields": ["id", "total", "date"]
      }
    }
  }
}
```

#### Query with Filtering

```json
{
  "query": {
    "marketing": {
      "customers": {
        "fields": ["id", "name", "email"],
        "where": {
          "name": "Alice"
        }
      }
    }
  }
}
```

#### Query with Relationships

```json
{
  "query": {
    "marketing": {
      "customers": {
        "fields": ["id", "name", "email", "orders"]
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
      "fields": ["types", "namespaces", "relationships"]
    }
  }
}
```

### REST Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/query` | Process a data query |
| POST | `/api/nl-query` | Process a natural language query (if AI enabled) |
| GET | `/api/schema` | Get schema information |
| GET | `/api/data-info` | Get data statistics |
| GET | `/api/namespaces` | Get namespace information |
| GET | `/api/relationships` | Get relationship information |

### WebSocket API

Connect to `/gql-websocket` endpoint using SockJS and STOMP:

```javascript
const socket = new SockJS('/gql-websocket');
const stompClient = Stomp.over(socket);

stompClient.connect({}, frame => {
  console.log('Connected: ' + frame);
  
  stompClient.subscribe('/topic/results', response => {
    const data = JSON.parse(response.body);
    console.log(data);
  });
  
  stompClient.send("/app/query", {}, 
    JSON.stringify({
      "query": {
        "marketing": {
          "customers": {
            "fields": ["id", "name"]
          }
        }
      }
    })
  );
});
```

## 🔍 Schema Introspection

You can query the schema structure with the metadata query:

```json
{
  "query": {
    "metadata": {
      "fields": ["types", "namespaces", "relationships", "directives"]
    }
  }
}
```

Response example:

```json
{
  "metadata": {
    "types": [
      {
        "name": "MarketingCustomer",
        "namespace": "marketing",
        "fields": [
          {"name": "id", "type": "ID!", "required": true, "isList": false, "isScalar": true},
          {"name": "name", "type": "String", "required": false, "isList": false, "isScalar": true},
          {"name": "email", "type": "String", "required": false, "isList": false, "isScalar": true}
        ],
        "source": {"type": "file", "path": "data/marketing.customer.json"}
      }
    ],
    "namespaces": [
      {
        "name": "marketing",
        "types": ["MarketingCustomer", "MarketingOrder", "MarketingCampaign"]
      },
      {
        "name": "finance",
        "types": ["FinanceCustomer"]
      }
    ],
    "relationships": [
      {
        "sourceType": "MarketingOrder",
        "fieldName": "customer",
        "targetType": "MarketingCustomer",
        "isList": false
      }
    ]
  }
}
```

## ⚡ Performance Considerations

- **Use field selection**: Request only the fields you need to reduce response size
- **Leverage namespaces**: Organize queries by business domain
- **Enable caching**: Use the `@cached` directive for frequently accessed data
- **Consider relationship depth**: Deep relationship chains may impact performance

## 🔧 Troubleshooting

**Common Issues:**

1. **Data files not found**:
   - Ensure JSON files exist in the data directory
   - Verify file paths in the schema match actual locations
   - Check file permissions

2. **Relationship resolution errors**:
   - Verify that both entities in the relationship exist in the schema
   - Ensure foreign key fields follow naming conventions (e.g., `customerId`)
   - Check that foreign key values exist in the related entity

3. **Schema parsing errors**:
   - Validate GraphQL syntax in your schema file
   - Check for missing closing brackets or quotes
   - Ensure directive syntax is correct

## 📊 Architecture Overview

The system is built with the following components:

1. **Schema Parser**: Parses GraphQL schema and builds type definitions
2. **Data Loader**: Loads data from various sources based on schema
3. **Query Processor**: Processes JSON queries and resolves relationships
4. **Metadata Provider**: Provides introspection capabilities

## 👥 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
