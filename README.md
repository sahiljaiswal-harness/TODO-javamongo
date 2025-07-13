# TODO Java REST API with MongoDB

A Java-based TODO REST API that supports **CRUD** operations using **MongoDB** as a persistent backend.

Built using:
- Uses **pure Java**
- Built with **Maven** for dependency management
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


## How to Run

```bash
mvn clean compile exec:java -Dexec.mainClass=todo.TodoApp
```

server will start at:
http://localhost:8000


Use curl commands for testing:

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
