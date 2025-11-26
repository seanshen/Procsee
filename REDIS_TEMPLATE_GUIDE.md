# RedisTemplate Test Code Documentation

## Overview

Added `RedisController.java` to the java-session-demo project, demonstrating direct Redis operations using Spring's `RedisTemplate`. This provides examples of reading and writing various data types to Redis without using Spring Session.

## New Files

### 1. RedisController.java
Location: `/java-session-demo/src/main/java/com/example/demo/RedisController.java`

A comprehensive REST controller with endpoints for:
- **String operations**: Read/write simple key-value pairs
- **Hash operations**: Store and retrieve maps/objects as Redis hashes
- **List operations**: Manage ordered lists
- **Object operations**: Store complex nested objects
- **Counter operations**: Increment/decrement numeric values
- **Key management**: Pattern matching, deletion
- **TTL support**: Set expiration times on keys

### 2. test_redis_template.sh
Location: `/test_redis_template.sh`

Bash script demonstrating all RedisTemplate endpoints with example requests.

## API Endpoints

All endpoints are under `/api/redis`:

### String Operations

**Write String:**
```bash
POST /api/redis/string
Body: {"key": "mykey", "value": "myvalue", "ttl": 300}
```

**Read String:**
```bash
GET /api/redis/string/{key}
```

### Hash Operations

**Write Hash:**
```bash
POST /api/redis/hash
Body: {"key": "user:123", "data": {"name": "John", "age": 30}}
```

**Read Hash:**
```bash
GET /api/redis/hash/{key}
```

### List Operations

**Write List:**
```bash
POST /api/redis/list
Body: {"key": "tasks", "values": ["task1", "task2", "task3"]}
```

**Read List:**
```bash
GET /api/redis/list/{key}
```

### Object Operations

**Write Object:**
```bash
POST /api/redis/object
Body: {"key": "person:1", "data": {"name": "Alice", "hobbies": ["reading"]}}
```

**Read Object:**
```bash
GET /api/redis/object/{key}
```

### Counter Operations

**Increment Counter:**
```bash
POST /api/redis/increment
Body: {"key": "counter", "delta": 1}
```

### Key Management

**Get Keys by Pattern:**
```bash
GET /api/redis/keys?pattern=user:*
```

**Delete Key:**
```bash
DELETE /api/redis/{key}
```

## Usage

### 1. Rebuild the Application

```bash
cd java-session-demo
mvn clean package -DskipTests
```

### 2. Restart the Server(s)

**If using run_servers.sh:**
```bash
# Stop existing servers
pkill -f "demo-0.0.1-SNAPSHOT.jar"

# Restart
./run_servers.sh
```

**Or manually:**
```bash
java -jar target/demo-0.0.1-SNAPSHOT.jar --server.port=8081
```

### 3. Run Tests

```bash
./test_redis_template.sh
```

### 4. View Data with redis_to_json.py

After writing data via the API, you can view it using the Python script:

```bash
# View a hash
python3 redis_to_json.py "user:1001"

# View a list
python3 redis_to_json.py "tasks:today"

# View an object
python3 redis_to_json.py "person:2001"
```

## Example Workflow

```bash
# 1. Write a user hash to Redis
curl -X POST "http://localhost:8081/api/redis/hash" \
  -H "Content-Type: application/json" \
  -d '{"key": "user:5000", "data": {"name": "Charlie", "email": "charlie@example.com", "role": "admin"}}'

# 2. Read it back via API
curl "http://localhost:8081/api/redis/hash/user:5000" | jq '.'

# 3. View the raw Redis data
python3 redis_to_json.py "user:5000"

# 4. Increment a page view counter
curl -X POST "http://localhost:8081/api/redis/increment" \
  -H "Content-Type: application/json" \
  -d '{"key": "page:views:home", "delta": 1}'

# 5. Get all user keys
curl "http://localhost:8081/api/redis/keys?pattern=user:*" | jq '.'
```

## Integration with redis_to_json.py

The RedisTemplate endpoints create data that can be viewed using the `redis_to_json.py` script:

- **Strings**: Displayed as simple values
- **Hashes**: Displayed as JSON objects with all fields
- **Lists**: Can be viewed but may show as serialized data
- **Objects**: Complex objects will be Java-serialized and may show as base64 (due to LinkedHashMap limitation)

## Key Features

1. **TTL Support**: All write operations support optional TTL (time-to-live) in seconds
2. **Type Safety**: Proper handling of different Redis data types
3. **Error Handling**: Returns meaningful error messages
4. **Metadata**: Read operations include existence checks, TTL info, and data types
5. **Pattern Matching**: Find keys using wildcards (e.g., `user:*`)

## Notes

- The application must be restarted after adding `RedisController.java` for the endpoints to be available
- All data written via RedisTemplate is separate from Spring Session data
- Use the `/api/session` endpoints for session-related operations
- Use the `/api/redis` endpoints for direct Redis operations
