# TODO Java Mongo App

A professional, production-ready TODO REST API built with Java 17, MongoDB, Redis, and Docker. Implements best practices for caching, error handling, and deployment.

---
- UUID-based unique IDs for all tasks
- MongoDB operations are **thread-safe**, ensuring safe concurrent access
- Uses MongoDB Atlas as the cloud database, connection string is secured via **environment variable** (MONGODB_URI)
- Redis caching implemented

## Features

- List TODOs (`GET /todos`)
- Create TODO (`POST /todos`)
- Get TODO by ID (`GET /todos/{id}`)
- Update TODO (`PUT /todos/{id}`)
- Delete TODO (`DELETE /todos/{id}`)
- Filter TODOs by completion status (`GET /todos?completed=true` or `false`)
- Search TODOs by keyword in title/description (`GET /todos?search=keyword`)

## API Error Handling

- All error responses are returned as JSON in the format `{ "error": "Message" }`.
- Examples:
  - `{ "error": "Not Found" }`
  - `{ "error": "Internal Server Error" }`

## Query Support

- The `/todos` endpoint supports optional query parameters:
  - `completed=true|false` — filter by completion status
  - `search=keyword` — filter by keyword in title or description
  - Both can be combined: `/todos?completed=true&search=work`

---

## Project Notes & Design Decisions

- **MongoDB Indexes:**
  - Uses indexes for efficient queries (recommended: create indexes on `completed`, `deleted`, and text index on `title` for search).
- **Redis & Jedis:**
  - Uses Redis for caching lists and items, with TTL and pattern-based invalidation. Jedis is used as the Java Redis client.
- **Build Tools:**
  - Maven for Java builds. Bazel/Gravel not used but considered for future extensibility and reproducible builds.
- **Error Handling:**
  - All errors are returned as JSON `{ "error": "..." }` with appropriate HTTP codes. Application never crashes on bad input—errors are always returned gracefully.
- **PUT vs PATCH:**
  - `PUT` replaces the entire resource. `PATCH` supports partial updates (only provided fields are changed).
- **Repository Pattern:**
  - All database queries are segregated into a dedicated repository class for maintainability and separation of concerns.
- **Robustness:**
  - Defensive code ensures that exceptions are caught, logged, and returned as error responses instead of killing the application.

---
**Solution:**
- Use `docker-compose.yml` to simplify starting all services.
- Use the provided `docker-cleanup.sh` script to remove exited containers and dangling images.

### On AWS EC2 (personal)
```bash
ssh -i ~/Downloads/CCMBootcamp.pem ec2-user@ec2-13-235-245-61.ap-south-1.compute.amazonaws.com
```

## How to Run

```bash
mvn clean compile exec:java -Dexec.mainClass=todo.TodoApp
```

server will start at:
http://localhost:8000


Use postman/curl commands for testing:

Get Todo List:
```bash
curl http://localhost:8000/todos
```

Get Todo (by id):
```bash
curl http://localhost:8000/todos/006dbc21-1944-4fb7-bc88-ee5355565e06
```

Post Todos:
```bash
curl -X POST http://localhost:8000/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"Buy milk","description":"Grocery store","completed":false}'
```

Put Todos:
```bash
curl -X POST http://localhost:8000/todos/006dbc21-1944-4fb7-bc88-ee5355565e06 \
  -H "Content-Type: application/json" \
  -d '{"title":"Buy milk","description":"Grocery store","completed":true}'
```

Delete Todo (soft delete):
```bash
curl -X DELETE http://localhost:8000/todos/006dbc21-1944-4fb7-bc88-ee5355565e06
```
