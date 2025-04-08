# GraphQL Query Examples

This file contains examples of how to call the API using GraphQL query syntax.

## Basic Usage

The GraphQL endpoint is available at `/api/graphql` and accepts POST requests with a JSON payload containing a GraphQL query string.

### Using cURL

```bash
curl -X POST http://localhost:8080/api/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { marketing { customers { id name email } } }"
  }'
```

### Using JavaScript Fetch API

```javascript
fetch('http://localhost:8080/api/graphql', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    query: `
      query {
        marketing {
          customers {
            id
            name
            email
          }
        }
      }
    `
  })
})
.then(response => response.json())
.then(data => console.log(data));
```

### Using Axios

```javascript
axios.post('http://localhost:8080/api/graphql', {
  query: `
    query {
      marketing {
        customers {
          id
          name
          email
        }
      }
    }
  `
})
.then(response => console.log(response.data));
```

## Example GraphQL Queries

### Simple Query

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

### Query with Arguments (Filtering)

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

### Query with Pagination

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

### Multiple Entity Types

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

### Cross-Domain Query

```graphql
query {
  marketing {
    customers {
      id
      name
    }
  }
  finance {
    invoices {
      id
      amount
      customerId
    }
  }
}
```

### Query with Nested Relationships

```graphql
query {
  marketing {
    customers {
      id
      name
      orders {
        id
        amount
        date
      }
    }
  }
}
```

## Response Format

The response follows the standard GraphQL response format:

```json
{
  "marketing": {
    "customers": [
      {
        "id": "1",
        "name": "Acme Corp",
        "email": "info@acmecorp.com"
      },
      {
        "id": "2",
        "name": "Globex",
        "email": "contact@globex.com"
      }
    ]
  }
}
```

In development mode, the response includes the transformed internal query format:

```json
{
  "transformedQuery": {
    "query": {
      "marketing": {
        "customers": {
          "fields": ["id", "name", "email"]
        }
      }
    }
  },
  "result": {
    "marketing": {
      "customers": [
        {
          "id": "1",
          "name": "Acme Corp",
          "email": "info@acmecorp.com"
        }
      ]
    }
  }
}
```

## Common Errors and Solutions

### Syntax Errors

If you receive a parsing error, check your GraphQL syntax. Common issues include:
- Missing curly braces
- Missing commas
- Incorrect nesting

### Field Not Found

If a field doesn't exist in the schema, you'll get an error. Use the schema introspection to check available fields:

```graphql
query {
  metadata {
    fields: ["types"]
  }
}
```

### Type Not Found

Make sure the type names match the schema. The API supports domain-prefixed types (e.g., `MarketingCustomer`) 
and namespace-based access (e.g., `marketing { customers }`) for better organization.
