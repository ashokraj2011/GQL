# GraphQL-like JSON API with WebSocket integration

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
- **🔐 Authentication & Authorization**: Field and type-level access control
- **📊 Pagination support**: Built-in pagination for large result sets
- **📈 Analytics domain**: Support for aggregated data and business insights
- **⏱️ Time Travel**: Query data as it existed at any point in time

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
directive @auth(requires: String) on FIELD_DEFINITION | OBJECT
directive @paginate(defaultLimit: Int = 10, maxLimit: Int = 100) on FIELD_DEFINITION
directive @log on FIELD_DEFINITION

# Marketing domain types
type Marketing @params(fields: ["id"]) {
  id: ID
  customers: [MarketingCustomer]
  orders: [MarketingOrder]
  campaigns: [MarketingCampaign]
  leads: [MarketingLead]
  events: [MarketingEvent]
}

type MarketingCustomer @source(file: "data/marketing.customer.json") {
  id: ID!
  name: String
  email: String
  orders: [MarketingOrder]
  leadSource: String
  customerSegment: String
  campaignHistory: [MarketingCampaign]
  totalSpend: Float @deprecated(reason: "Use analytics.customerValue instead")
  leadId: ID
  lead: MarketingLead
}

# Finance domain types
type Finance @params(fields: ["id"]) {
  id: ID
  customers: [FinanceCustomer]
  invoices: [FinanceInvoice]
  transactions: [FinanceTransaction]
}

# Analytics domain for aggregated data and insights
type Analytics @params(fields: ["id"]) {
  id: ID
  customerAnalytics: [CustomerAnalytics]
  campaignPerformance: [CampaignPerformance]
  salesTrends: [SalesTrend]
}
```

### Available Directives

- `@source(file: "path/to/file.json")`: Specify JSON data source
- `@api(url: "https://api.example.com/resource")`: Specify external API source
- `@params(fields: ["field1", "field2"])`: Define namespace parameters
- `@deprecated(reason: "Use newField instead")`: Mark deprecated fields
- `@cached(seconds: 300)`: Cache field results
- `@auth(requires: "ROLE_NAME")`: Restrict access to specific roles
- `@paginate(defaultLimit: 10, maxLimit: 100)`: Enable pagination with limits
- `@log`: Enable logging for a field or operation

## 📂 Data Sources

Place your JSON data files in the `data/` directory:

```
data/
├── marketing.customer.json
├── marketing.order.json
├── marketing.campaign.json
├── marketing.lead.json
├── marketing.event.json
├── finance.customer.json
├── finance.invoice.json
├── finance.transaction.json
├── analytics.customer.json
├── analytics.campaign.json
├── analytics.sales.json
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

#### Analytics Query (Requires Authentication)

```json
{
  "query": {
    "analytics": {
      "customerAnalytics": {
        "fields": ["customerId", "lifetime_value", "engagement_score"]
      }
    }
  }
}
```

#### Paginated Query

```json
{
  "query": {
    "marketingCustomers": {
      "fields": ["id", "name", "email"],
      "pagination": {
        "limit": 10,
        "offset": 0
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
| POST | `/api/query/at` | Process a point-in-time query |
| POST | `/api/nl-query` | Process a natural language query (if AI enabled) |
| GET | `/api/schema` | Get schema information |
| GET | `/api/data-info` | Get data statistics |
| GET | `/api/namespaces` | Get namespace information |
| GET | `/api/relationships` | Get relationship information |
| GET | `/api/history/{typeName}/{id}` | Get record history |
| GET | `/api/health` | Service health and system status information |

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

## 🔄 Relationships

The system automatically detects and manages relationships between entities based on the schema definition. Relationships are tracked through the `RelationshipInfo` class, which contains:

- `sourceType`: The type containing the relationship field
- `fieldName`: The field name in the source type that references the target
- `targetType`: The type being referenced
- `isList`: Whether the relationship is a one-to-many (true) or one-to-one (false)

Relationships are automatically inferred from:

1. Field types matching other entity types
2. Foreign key fields (e.g., `customerId` would link to a `Customer` type)
3. Field names matching entity types in lowercase

You can query relationships explicitly with:

```json
{
  "query": {
    "metadata": {
      "fields": ["relationships"]
    }
  }
}
```

This returns all detected relationships in the system:

```json
{
  "metadata": {
    "relationships": [
      {
        "sourceType": "MarketingOrder",
        "fieldName": "customer",
        "targetType": "MarketingCustomer",
        "isList": false
      },
      {
        "sourceType": "MarketingCustomer",
        "fieldName": "orders",
        "targetType": "MarketingOrder",
        "isList": true
      }
    ]
  }
}
```

## 🔐 Authentication & Authorization

The API supports role-based access control through the `@auth` directive:

```graphql
type Analytics @auth(requires: "ROLE_ANALYST") {
  # Only accessible to users with ROLE_ANALYST
  customerAnalytics: [CustomerAnalytics]
}

type MarketingCampaign {
  id: ID!
  name: String
  budget: Float @auth(requires: "ROLE_MANAGER") # Field-level restriction
}
```

To use authenticated endpoints, include an authorization header with your request:

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token" \
  -d '{"query": {"analytics": {"customerAnalytics": {"fields": ["customerId", "lifetime_value"]}}}}'
```

## 📊 Analytics Data

The API includes a dedicated Analytics domain for business intelligence data:

### Customer Analytics

```json
{
  "query": {
    "analytics": {
      "customerAnalytics": {
        "fields": ["customerId", "lifetime_value", "engagement_score", "churn_risk"]
      }
    }
  }
}
```

### Campaign Performance

```json
{
  "query": {
    "analytics": {
      "campaignPerformance": {
        "fields": ["campaignId", "spend", "revenue", "roi", "conversions"]
      }
    }
  }
}
```

### Sales Trends

```json
{
  "query": {
    "analytics": {
      "salesTrends": {
        "fields": ["period", "revenue", "growth_rate", "top_products"]
      }
    }
  }
}
```

## 📄 Pagination

The API supports pagination for large datasets using the `@paginate` directive:

```graphql
type Query {
  marketingCustomers: [MarketingCustomer] @paginate(defaultLimit: 20)
}
```

To paginate results, include pagination parameters in your query:

```json
{
  "query": {
    "marketingCustomers": {
      "fields": ["id", "name", "email"],
      "pagination": {
        "limit": 10,  
        "offset": 20
      }
    }
  }
}
```

The response includes pagination metadata:

```json
{
  "marketingCustomers": {
    "data": [
      { "id": "121", "name": "Customer 21", "email": "c21@example.com" },
      // ... more items
    ],
    "pagination": {
      "total": 150,
      "limit": 10,
      "offset": 20,
      "hasNext": true,
      "hasPrevious": true
    }
  }
}
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

## ⏱️ Time Travel Queries

The API provides comprehensive time travel capabilities through the `TimeTravel` service, allowing you to query data as it existed at any point in time.

### Querying Historical Data

To query data as it existed at a specific timestamp, use the `/api/query/at` endpoint:

```bash
curl -X POST http://localhost:8080/api/query/at?timestamp=2023-06-01T00:00:00Z \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "marketing": {
        "customers": {
          "fields": ["id", "name", "email"]
        }
      }
    }
  }'
```

This returns the data as it existed on June 1, 2023.

### Retrieving Record History

To retrieve the complete history of a specific record:

```bash
curl -X GET http://localhost:8080/api/history/MarketingCustomer/123
```

This returns all historical versions of the customer with ID 123, with their respective validity periods.

### Historical Data Format

Historical data files should be stored alongside the current data with a `.history.json` suffix:

```
data/
├── marketing.customer.json         # Current data
├── marketing.customer.history.json # Historical versions
```

Each record in the history file should include `validFrom` and `validTo` timestamps:

```json
[
  {
    "id": "123",
    "name": "Old Company Name",
    "email": "contact@example.com",
    "validFrom": "2022-01-01T00:00:00Z",
    "validTo": "2023-01-01T00:00:00Z"
  },
  {
    "id": "123",
    "name": "New Company Name",
    "email": "updated@example.com",
    "validFrom": "2023-01-01T00:00:00Z",
    "validTo": null
  }
]
```

The `validTo` field is `null` for current records.

### Time Travel Implementation

The time travel functionality is implemented by:

1. The `TimeTravel` service acts as a coordinator for time-based queries
2. The `DataLoader` maintains historical versions of records
3. When a time-travel query is received, the system:
   - Sets the point-in-time context
   - Filters records based on their valid time ranges
   - Returns only data that was valid at the requested timestamp
   - Resets the context to avoid affecting subsequent queries

This enables sophisticated historical analysis and auditing capabilities without modifying your query structure.

## 🔮 AI-Assisted Query Generation

The API includes an optional AI query generation feature that allows users to describe what data they need in natural language, and the system will generate a structured query automatically.

### Natural Language Query Endpoint

```bash
curl -X POST http://localhost:8080/api/nl-query \
  -H "Content-Type: application/json" \
  -d '{"query": "Get me all marketing customers who spent more than $1000 last month"}'
```

This produces a response with both the generated query and the results:

```json
{
  "generatedQuery": {
    "query": {
      "marketing": {
        "customers": {
          "fields": ["id", "name", "email", "totalSpend", "orders"],
          "where": {
            "totalSpend": 1000
          }
        }
      }
    }
  },
  "result": {
    "marketing": {
      "customers": [
        {"id": "123", "name": "Acme Corp", "email": "contact@acme.com", "totalSpend": 1500},
        // ... additional results
      ]
    }
  }
}
```

### AI Provider Integration

To enable AI query generation, implement the `AIQueryGenerator` interface with your preferred AI provider:

```java
@Service
public class OpenAIQueryGenerator implements AIQueryGenerator {
    // Implementation using OpenAI's API
}
```

## 🧠 Enhanced Schema Types

The system uses a rich schema type model that extends beyond the GraphQL schema definition:

### `SchemaType` Properties

| Property | Description |
|----------|-------------|
| `name` | The type name (e.g., "MarketingCustomer") |
| `namespace` | The business domain namespace (e.g., "marketing") |
| `sourceFile` | Path to the JSON data source |
| `apiUrl` | URL for API data source (alternative to sourceFile) |
| `fields` | List of fields defined for this type |
| `log` | Whether to enable detailed logging for this type |

### SchemaField Methods

The schema includes helper methods for field operations:

```java
// Check if a type has a specific field
if (schemaType.hasField("email")) {
    // Field exists
}

// Get a field by name
Optional<SchemaField> field = schemaType.getFieldByName("email");
```

## 📋 Advanced Query Examples

### Cross-Domain Query with Relationships

This query retrieves marketing customers and their related finance data:

```json
{
  "query": {
    "marketing": {
      "customers": {
        "fields": ["id", "name", "email"],
        "where": {"customerSegment": "Enterprise"}
      }
    },
    "finance": {
      "customers": {
        "fields": ["id", "accountBalance", "outstandingInvoices"],
        "where": {"accountStatus": "Active"}
      }
    }
  }
}
```

### Historical Data Analysis

This query compares current data with historical state:

```json
// Current state query
{
  "query": {
    "marketing": {
      "campaigns": {
        "fields": ["id", "name", "budget", "roi"]
      }
    }
  }
}

// Historical state (6 months ago)
// POST /api/query/at?timestamp=2023-10-01T00:00:00Z
{
  "query": {
    "marketing": {
      "campaigns": {
        "fields": ["id", "name", "budget", "roi"]
      }
    }
  }
}
```

### Metadata and Data Inspection

This comprehensive query provides both schema information and data samples:

```json
{
  "query": {
    "metadata": {
      "fields": ["types", "namespaces", "relationships"]
    },
    "marketing": {
      "customers": {
        "fields": ["id", "name"],
        "pagination": {
          "limit": 3
        }
      }
    }
  }
}
```

## 📋 Namespace Resolution Logic

The API implements sophisticated namespace resolution to make cross-domain queries intuitive. The resolution follows these rules:

1. **Explicit Namespace**: Types with a `@namespace` directive are assigned to that namespace
2. **Name-based Resolution**: Types named like `MarketingCustomer` are mapped to the `marketing` namespace
3. **Common Entity Resolution**: Entity names like "Customer" are mapped to domain-specific implementations
4. **Fallback Resolution**: If a clear namespace can't be determined, the entity is available in all namespaces

This allows for intuitive queries like:

```json
{
  "query": {
    "marketing": {
      "customers": { /* ... */ }
    }
  }
}
```

Which automatically resolves to `MarketingCustomer` without requiring the full type name.

## 📊 Integration Options

### Spring Boot Application Integration

```java
@Autowired
private QueryProcessor queryProcessor;

public JsonNode performQuery(String jsonQuery) {
    JsonNode queryNode = new ObjectMapper().readTree(jsonQuery);
    return queryProcessor.processQuery(queryNode);
}
```

### External System Integration

The API can be integrated with external systems through:

1. **REST API calls** - Standard HTTP clients
2. **WebSocket connections** - For real-time data needs
3. **Embedded mode** - Include as a dependency in your application

### Database Integration

While the system primarily works with JSON files, you can extend it to work with databases:

```java
@Component
public class DatabaseDataLoader implements DataSourceAdapter {
    @Override
    public List<Map<String, Object>> loadData(String typeName) {
        // Query database and return results
    }
}
```

## ⚡ Performance Considerations

- **Use field selection**: Request only the fields you need to reduce response size
- **Leverage namespaces**: Organize queries by business domain
- **Enable caching**: Use the `@cached` directive for frequently accessed data
- **Consider relationship depth**: Deep relationship chains may impact performance
- **Use pagination**: For large datasets, always use pagination to limit response size
- **Time travel queries**: These are more resource-intensive, so use them judiciously

## 🏥 Health Monitoring

The API includes a comprehensive health monitoring endpoint that provides detailed information about the service status and system resources.

### Health Endpoint

```bash
curl -X GET http://localhost:8080/api/health
```

### Health Response Structure

```json
{
  "status": "UP",
  "timestamp": "2023-04-01T14:30:45.123Z",
  "components": {
    "schema": {
      "status": "UP",
      "types": 24
    },
    "data": {
      "status": "UP", 
      "sources": 8
    }
  },
  "system": {
    "cpuLoad": 0.42,
    "cpuLoadPercentage": "42.00%",
    "availableProcessors": 8,
    "osName": "Mac OS X",
    "osVersion": "11.6",
    "osArch": "x86_64"
  },
  "metrics": {
    "totalRequests": 12345,
    "requestsPerSecond": "42.50"
  }
}
```

### Health Data Fields

| Field | Description |
|-------|-------------|
| `status` | Overall service status ("UP" or "DOWN") |
| `timestamp` | Current server time (ISO-8601 format) |
| `components.schema.types` | Number of schema types loaded |
| `components.data.sources` | Number of data sources available |
| `system.cpuLoad` | System CPU load (0.0-1.0 scale) |
| `system.cpuLoadPercentage` | CPU load as percentage |
| `system.availableProcessors` | Number of available CPU cores |
| `system.osName` | Operating system name |
| `system.osVersion` | Operating system version |
| `system.osArch` | Operating system architecture |
| `metrics.totalRequests` | Total number of API requests since startup |
| `metrics.requestsPerSecond` | Current request rate (calculated from recent window) |

### Health Monitoring Best Practices

- **Regular Polling**: Set up scheduled health checks to monitor system status
- **Alert Thresholds**: Configure alerts if CPU usage exceeds 80% for extended periods
- **Component Status**: Track component-level issues to identify specific problems
- **Integration**: Use with monitoring tools like Prometheus, Grafana, or New Relic
- **Responsive Scaling**: Use health metrics to trigger auto-scaling in containerized deployments

This endpoint is particularly useful for:
- Container orchestration health checks (Kubernetes, Docker Swarm)
- Load balancer status checks
- Operations monitoring dashboards
- Automated system scaling decisions

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

4. **Authentication errors**:
   - Verify your JWT token is valid and not expired
   - Ensure the user has the required role for the resource
   - Check authorization header format

5. **Time travel query issues**:
   - Verify the timestamp format (ISO 8601: YYYY-MM-DDThh:mm:ssZ)
   - Check that historical data files exist with the correct naming convention
   - Ensure records have proper `validFrom` and `validTo` fields

6. **Health monitoring issues**:
   - Verify the service is running and accessible
   - Check that you have appropriate permissions to access system metrics
   - On some systems, CPU load information might be limited or unavailable
   - Resource metrics may be inaccurate in containerized environments

## 📊 Architecture Overview

The system is built with the following components:

1. **Schema Parser**: Parses GraphQL schema and builds type definitions
2. **Data Loader**: Loads data from various sources based on schema
3. **Query Processor**: Processes JSON queries and resolves relationships
4. **Relationship Manager**: Tracks and resolves relationships between entities
5. **Time Travel Service**: Enables point-in-time querying
6. **Metadata Provider**: Provides introspection capabilities
7. **AI Query Generator**: (Optional) Translates natural language to structured queries

## 👥 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

`