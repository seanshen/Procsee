# Nested Objects in HttpSession with SaveMode.ALWAYS

## Overview

This implementation demonstrates Spring Session's ability to persist deeply nested Java objects in HttpSession with `SaveMode.ALWAYS`. It includes comprehensive Postman tests to verify that modifications to nested object properties are correctly saved and retrieved across HTTP requests.

## Architecture

### Nesting Structure (3-4 levels deep)

```
Employee (Level 1)
├── id, name, title, salary (primitives)
├── Contact (Level 2)
│   ├── phone, email (primitives)
│   └── Address (Level 3)
│       └── street, city, state, zipCode (primitives)
├── Company (Level 2)
│   ├── name, industry (primitives)
│   └── Contact (Level 3)
│       ├── phone, email (primitives)
│       └── Address (Level 4)
│           └── street, city, state, zipCode (primitives)
└── List<Address> previousAddresses (Level 2)
```

## New Files Created

### Model Classes

1. **`Address.java`** - Basic address (street, city, state, zipCode)
2. **`Contact.java`** - Contact info with nested Address
3. **`Company.java`** - Company info with nested Contact
4. **`Employee.java`** - Main class with deep nesting

All classes implement `Serializable` for Redis storage.

### Configuration

**`SessionConfig.java`** - Spring Session configuration:
```java
@EnableRedisHttpSession(saveMode = SaveMode.ALWAYS)
```

**SaveMode.ALWAYS** ensures the session is saved on every request, even when only nested object properties are modified. This is crucial for detecting changes in deeply nested objects.

### Controller Endpoints

Added to `SessionController.java`:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/session/employee` | POST | Create nested Employee object |
| `/api/session/employee` | GET | Retrieve Employee from session |
| `/api/session/employee/nested` | PUT | Modify nested property by path |
| `/api/session/employee/nested?path=...` | GET | Get specific nested property |

### Postman Collection

**`postman/nested-session-test.postman_collection.json`**

10 comprehensive tests:
1. Create Employee with nested objects
2. Read Employee - verify initial state
3. Modify Level 1 property (`name`)
4. Verify Level 1 modification persisted
5. Modify Level 2 property (`contact.email`)
6. Verify Level 2 modification persisted
7. Modify Level 3 property (`company.contact.address.city`)
8. Verify Level 3 modification persisted
9. Modify multiple properties (`salary`)
10. Verify all modifications persisted

## Usage

### 1. Rebuild and Restart Servers

```bash
cd java-session-demo
mvn clean package -DskipTests

# Stop existing servers
pkill -f "demo-0.0.1-SNAPSHOT.jar"

# Restart servers
./run_servers.sh
```

### 2. Test with Postman

**Option A: Import and Run in Postman GUI**
1. Open Postman
2. Import `postman/nested-session-test.postman_collection.json`
3. Run the collection with Collection Runner
4. All 10 tests should pass ✅

**Option B: Run with Newman (CLI)**
```bash
newman run postman/nested-session-test.postman_collection.json
```

### 3. Manual Testing with curl

**Create Employee:**
```bash
curl -c cookies.txt -X POST "http://localhost:8080/api/session/employee" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1001,
    "name": "John Doe",
    "title": "Senior Developer",
    "salary": 95000.0,
    "contact": {
      "phone": "555-1234",
      "email": "john@example.com",
      "address": {
        "street": "123 Main St",
        "city": "New York",
        "state": "NY",
        "zipCode": "10001"
      }
    },
    "company": {
      "name": "Tech Corp",
      "industry": "Software",
      "contact": {
        "phone": "555-5678",
        "email": "info@techcorp.com",
        "address": {
          "street": "456 Corporate Blvd",
          "city": "San Francisco",
          "state": "CA",
          "zipCode": "94102"
        }
      }
    }
  }' | jq '.'
```

**Modify Nested Property:**
```bash
curl -b cookies.txt -X PUT "http://localhost:8080/api/session/employee/nested" \
  -H "Content-Type: application/json" \
  -d '{"path": "company.contact.address.city", "value": "Boston"}' | jq '.'
```

**Verify Modification:**
```bash
curl -b cookies.txt "http://localhost:8080/api/session/employee" | jq '.employee.company.contact.address.city'
# Should output: "Boston"
```

**Get Specific Nested Property:**
```bash
curl -b cookies.txt "http://localhost:8080/api/session/employee/nested?path=company.contact.address.city" | jq '.'
```

### 4. View Redis Data

```bash
# Get session ID from response
SESSION_ID="your-session-id-here"

# View the serialized Employee object in Redis
python3 redis_to_json.py "spring:session:sessions:$SESSION_ID"
```

## How It Works

### SaveMode.ALWAYS

Without `SaveMode.ALWAYS`, Spring Session only saves the session when:
- A new attribute is added
- An attribute is removed
- The session is explicitly marked as dirty

With `SaveMode.ALWAYS`, the session is saved on **every request**, which ensures:
- Modifications to nested object properties are detected
- Changes persist even if you only modify a deeply nested field
- No need to manually mark the session as dirty

### Reflection-Based Property Modification

The `modifyNestedProperty` endpoint uses Java reflection to:
1. Parse the property path (e.g., `"company.contact.address.city"`)
2. Navigate through the object graph
3. Set the final property value
4. Re-set the session attribute to trigger save

### Path Syntax

Property paths use dot notation:
- `"name"` - Level 1 (direct property)
- `"contact.email"` - Level 2 (nested in Contact)
- `"contact.address.city"` - Level 3 (nested in Address within Contact)
- `"company.contact.address.city"` - Level 4 (deeply nested)

## Postman Test Verification

Each test in the Postman collection:
1. **Modifies** a nested property
2. **Stores** the expected value in a collection variable
3. **Reads** the employee in the next request
4. **Compares** the actual value with the expected value
5. **Asserts** they match

This ensures that:
- ✅ Modifications are applied correctly
- ✅ Changes persist across requests
- ✅ SaveMode.ALWAYS is working
- ✅ Nested object serialization/deserialization works

## Key Features

1. **Deep Nesting**: 3-4 levels of nested objects
2. **SaveMode.ALWAYS**: Automatic session save on every request
3. **Reflection-Based Modification**: Dynamic property updates via path
4. **Comprehensive Tests**: 10 Postman tests covering all scenarios
5. **Type Conversion**: Automatic conversion of JSON values to Java types
6. **Error Handling**: Graceful handling of missing properties or null values

## Expected Test Results

When running the Postman collection, you should see:
```
✓ Status code is 200
✓ Response contains employee
✓ Employee exists in session
✓ All nested properties match original
✓ Modification successful
✓ Level 1 property modification persisted
✓ Level 2 property modification persisted
✓ Level 3 property modification persisted
✓ All modifications persisted correctly
```

All 10 requests should complete successfully with all assertions passing.

## Troubleshooting

**If modifications don't persist:**
1. Verify `SessionConfig.java` has `@EnableRedisHttpSession(saveMode = SaveMode.ALWAYS)`
2. Check that servers were restarted after code changes
3. Ensure Redis is running: `docker ps | grep redis`
4. Verify session cookie is being sent in requests

**If tests fail:**
1. Check server logs for errors
2. Verify the session ID is being captured correctly
3. Ensure all 3 server instances are running
4. Try running tests individually to isolate issues

## Next Steps

- Run the Postman collection to verify all tests pass
- Examine Redis data with `redis_to_json.py` to see serialized objects
- Try modifying different nested properties
- Test with different server instances (8081, 8082, 8083) to verify Redis-backed session sharing
