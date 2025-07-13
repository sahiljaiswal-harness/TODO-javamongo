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

## Common Problems and How We Solved Them

### 1. Docker image not runnable / No main manifest attribute
**Problem:** The built JAR did not specify the main class, so `java -jar app.jar` failed.
**Solution:** Added `maven-jar-plugin` configuration in `pom.xml` to set the `Main-Class` manifest attribute.

### 2. JAR missing dependencies (NoClassDefFoundError)
**Problem:** The app JAR did not include dependencies like Gson, causing runtime errors.
**Solution:** Added `maven-assembly-plugin` to build a "fat JAR" containing all dependencies.

### 3. Redis connection errors inside Docker
**Problem:** The app could not connect to Redis when running in Docker, even with `REDIS_HOST=host.docker.internal` or `127.0.0.1`.
**Solution:**
- Updated the Java code to read the Redis host from the `REDIS_HOST` environment variable.
- Switched to using Docker Compose to run both Redis and the app as services on the same network. Set `REDIS_HOST=redis` in the app service.

### 4. Container name conflicts
**Problem:** Running `docker run` with the same container name (`--name todo-app`) caused conflicts if the container already existed.
**Solution:** Use `docker rm todo-app` or the provided `docker-cleanup.sh` script to remove old containers before starting new ones.

### 5. General Docker workflow
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
