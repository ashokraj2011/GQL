# GraphQL-like JSON API

## Overview
A simple API that mimics GraphQL features over JSON files or external services.

## Key Features
- Schema-driven data loading
- Field-based queries with filtering
- Pagination and sorting
- Namespace support
- Metadata endpoints for introspection

## Quick Start
1. Install dependencies:
```bash
mvn clean install
```
2. Run the service:
```bash
mvn spring-boot:run
```
3. The API runs on port 8080 by default.

## Usage Examples
POST to /api/query:
```json
{
  "query": {
    "collection": "MarketingCustomer",
    "fields": ["id", "name"],
    "where": { "id": { "$in": ["100"] } }
  }
}
```

## WebSocket Integration
You can query the server over WebSockets by sending your query to "/app/querySocket". 
The server will broadcast the results on "/topic/responses".

## Metadata
GET /api/metadata returns schema and relationship details for exploration.

## Debug
- GET /debug/datastore: View loaded data
- GET /debug/schema: View schema structure

## License
Open source under the MIT License.

`