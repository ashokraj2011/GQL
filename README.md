# GraphQL-like JSON API with WebSocket integration

[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

**Author:** Ashok Raj (ashok.nair.raj@gmail.com)

A lightweight, schema-driven REST API providing GraphQL-like capabilities over JSON data sources with intelligent namespace handling and relationship resolution.

## 🌟 Overview

This API enables GraphQL-like querying over various data sources without requiring a full GraphQL implementation. Built with Spring Boot, it's designed for rapid development and prototyping of data-driven applications with a focus on domain-driven design. WebSocket capability makes it suitable for low latency applications with stateful connections.

```
┌─────────────┐     ┌────────────────┐     ┌──────────┐
│ Client App  │────▶│ GraphQL-like   │────▶│ JSON     │
│ or Service  │◀────│ JSON API       │◀────│ Data     │
└─────────────┘     └────────────────┘     └──────────┘
       │                    │                    ▲
       │                    │                    │
       │                    ▼                    │
       │             ┌──────────────┐     ┌──────────────┐
       │             │ External     │     │ Database     │
       │             │ APIs         │     │ (PostgreSQL, │
       │             └──────────────┘     │  H2, etc.)   │
       │                                  └──────────────┘
       │            WebSocket Connection
       └───────────────────────────────────┐
                                           ▼
                                    ┌─────────────────┐
                                    │ Real-time       │
                                    │ Clients         │
                                    └─────────────────┘
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
- **Database Integration**: Direct mapping to JPA entities for real database access
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
  - Databases (via `@db` directive)
  - Custom data sources (implementing `DBDataSource`)
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
- PostgreSQL or H2 database (for database features)

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
| `spring.datasource.url` | Database URL | varies by profile |
| `spring.datasource.username` | Database username | sa |
| `spring.datasource.password` | Database password | password |

## 📐 Schema Definition

The schema uses GraphQL syntax with custom directives. Create a `schema.graphql` file like this:

```graphql
# Custom directives to specify data sources
directive @source(file: String) on OBJECT
directive @api(url: String) on OBJECT
directive @db(entity: String) on OBJECT
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

# Database-backed Employee type
type Employee @db(entity: "Employee") {
  id: ID!
  firstName: String!
  lastName: String!
  email: String!
  department: String
  position: String
  hireDate: String
  salary: Float
}

# Finance domain types
type Finance @params(fields: ["id"]) {
  id: ID
  customers: [FinanceCustomer]
  invoices: [FinanceInvoice]
  transactions: [FinanceTransaction]
  employees: [Employee]
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
- `@db(entity: "EntityName")`: Specify database entity source
- `@params(fields: ["field1", "field2"])`: Define namespace parameters
- `@deprecated(reason: "Use newField instead")`: Mark deprecated fields
- `@cached(seconds: 300)`: Cache field results
- `@auth(requires: "ROLE_NAME")`: Restrict access to specific roles
- `@paginate(defaultLimit: 10, maxLimit: 100)`: Enable pagination with limits
- `@log`: Enable logging for a field or operation

## 📂 Data Sources

### JSON Files

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

### Database Entities

To use database entities as data sources:

1. Define JPA entities in your project:

```java
@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    // Other fields...
    
    // Getters and setters...
}
```

2. Create a Spring Data JPA repository:

```java
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    List<Employee> findByDepartment(String department);
    List<Employee> findBySalaryGreaterThan(Double salary);
}
```

3. Implement a DBDataSource to connect your entity to the GQL API:

```java
@Component
public class EmployeeDBDataSource implements DBDataSource {
    @Autowired
    private EmployeeRepository repository;
    
    @Override
    public String getEntityType() {
        return "Employee";  // Must match schema type name
    }
    
    @Override
    public List<Map<String, Object>> getAllEntities() {
        return repository.findAll().stream()
            .map(this::convertToMap)
            .collect(Collectors.toList());
    }
    
    // Implement other required methods...
    
    private Map<String, Object> convertToMap(Employee employee) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", employee.getId().toString());
        map.put("firstName", employee.getFirstName());
        map.put("lastName", employee.getLastName());
        // Map other fields...
        return map;
    }
}
```

4. Define your type in the schema with the `@db` directive:

```graphql
type Employee @db(entity: "Employee") {
  id: ID!
  firstName: String!
  lastName: String!
  email: String!
  department: String
  position: String
  hireDate: String
  salary: Float
}
```

## 📡 API Usage

### Query Format Options

The API now supports two query formats:

1. **Standard GraphQL Syntax** - Standard GraphQL query language
2. **Internal JSON Query Format** - The original format used by the API

### GraphQL Syntax Queries

To send a GraphQL-style query to the API:

```bash
curl -X POST http://localhost:8080/api/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { marketing { customers { id name email } } }"
  }'
```

#### GraphQL Query Examples

**Simple Query:**
```graphql
query {
  marketing {
    customers {
      id
      name
      email
    }
  }
}
```

**Query with Arguments:**
```graphql
query {
  marketing {
    customers(where: { name: "Alice" }) {
      id
      name
      email
    }
  }
}
```

**Query with Pagination:**
```graphql
query {
  marketing {
    customers(limit: 10, offset: 20) {
      id
      name
      email
    }
  }
}
```

**Multiple Entity Types:**
```graphql
query {
  marketing {
    customers {
      id
      name
    }
    campaigns {
      id
      name
      budget
    }
  }
}
```

See `src/main/resources/examples/graphql-examples.md` for more examples of GraphQL queries.

### Basic JSON Query Structure

The API continues to accept JSON queries with the following structure:

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

#### Database Entity Query

```json
{
  "query": {
    "employees": {
      "fields": ["id", "firstName", "lastName", "department", "salary"],
      "where": { "department": "Engineering" }
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

#### Query with Database and File-Based Sources

```json
{
  "query": {
    "finance": {
      "employees": {
        "fields": ["id", "firstName", "lastName", "salary"],
        "where": { "salary": 100000 }
      },
      "customers": {
        "fields": ["id", "name", "accountNumber"]
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
| POST | `/api/query` | Process a JSON format query |
| POST | `/api/graphql` | Process a standard GraphQL syntax query |
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

## 💾 Database Integration

The API seamlessly integrates with relational databases through JPA, allowing you to use your existing database schema with minimal configuration.

### Database Configuration

Configure your database connection in `application.properties` or `application.yml`:

```properties
# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/mydatabase
spring.datasource.username=postgres
spring.datasource.password=password

# JPA/Hibernate settings
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

### Entity Configuration

1. **