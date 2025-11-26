# Redis HGETALL to JSON Converter

A Python utility to convert Redis `HGETALL` command output into JSON format. Specifically designed to handle Spring Session data stored in Redis, which contains Java-serialized objects.

## Features

- ✅ Direct Redis connection (no redis-cli needed)
- ✅ Parses Java serialized `Integer`, `Long`, `Float`, `Double`, `Boolean` objects
- ✅ Handles nested structures and arrays
- ✅ Handles binary data with base64 encoding fallback
- ✅ Preserves field names and structure
- ✅ Pretty-printed JSON output

## Usage

### Basic Usage

```bash
# Get session data from Redis and convert to JSON
python3 redis_to_json.py "spring:session:sessions:SESSION_ID"

# Specify Redis host/port
python3 redis_to_json.py "spring:session:sessions:SESSION_ID" --host localhost --port 6379

# With password
python3 redis_to_json.py "spring:session:sessions:SESSION_ID" --password mypassword

# Different database
python3 redis_to_json.py "spring:session:sessions:SESSION_ID" --db 1
```

### Windows Usage

**Command Prompt (cmd.exe):**
```batch
REM Using batch script
view_session.bat SESSION_ID

REM Or directly
python redis_to_json.py "spring:session:sessions:SESSION_ID" --host localhost --port 6379
```

**PowerShell:**
```powershell
# Using PowerShell script
.\view_session.ps1 SESSION_ID

# Or directly
python redis_to_json.py "spring:session:sessions:SESSION_ID" --host localhost --port 6379
```

**Note for Windows:** Use `python` instead of `python3` if `python3` is not available.

### Example with Procsee Demo

```bash
# Get a session ID from the Java app
SESSION_ID=$(curl -s -X POST -H "Content-Type: application/json" \
  -d '{"testAttr": 123}' http://localhost:8081/api/session | \
  jq -r '.sessionId')

# Convert the Redis session data to JSON
python3 redis_to_json.py "spring:session:sessions:$SESSION_ID"
```

### Save to File

```bash
python3 redis_to_json.py "spring:session:sessions:SESSION_ID" > session.json
```

## Output Format

The script converts Spring Session data like this:

**Redis HGETALL output:**
```
sessionAttr:testAttr
<binary Java serialized Integer>
lastAccessedTime
<binary Java serialized Long>
creationTime
<binary Java serialized Long>
maxInactiveInterval
<binary Java serialized Integer>
```

**JSON output:**
```json
{
  "sessionAttr:testAttr": 123,
  "lastAccessedTime": 1764050936771,
  "creationTime": 1764050936769,
  "maxInactiveInterval": 1800
}
```

## Supported Data Types

| Java Type | Conversion |
|-----------|------------|
| `java.lang.Integer` | Parsed to JSON number |
| `java.lang.Long` | Parsed to JSON number |
| Other serialized objects | Base64-encoded with type hint |
| UTF-8 strings | Preserved as strings |
| Binary data | Base64-encoded |

## Handling Unknown Types

For Java objects that cannot be parsed (custom classes, complex objects), the script returns a structured object:

```json
{
  "_type": "java_serialized",
  "_base64": "rO0ABXNyABNqYXZhLnV0aWwuQXJyYXlMaXN0..."
}
```

## Requirements

- Python 3.6+
- `javaobj-py3` library: `pip3 install javaobj-py3`

## Known Limitations

### LinkedHashMap Deserialization

The current version of `javaobj-py3` (v0.4.4) has a known bug when deserializing `java.util.LinkedHashMap` objects. When the script encounters a LinkedHashMap, it will:

1. Attempt to deserialize it
2. If deserialization fails, fall back to base64 encoding
3. Return an object with `_type`, `_error`, and `_base64` fields

**Example of failed deserialization:**
```json
{
  "sessionAttr:complexObject": {
    "_type": "java_serialized",
    "_error": "Stream has been ended unexpectedly while unmarshaling.",
    "_base64": "rO0ABXNyABdqYXZhLnV0aWwuTGlua2VkSGFzaE1hcDTATlwQbMD7..."
  }
}
```

**Workaround:**
- Use `java.util.HashMap` instead of `LinkedHashMap` in your Java code if possible
- For simple nested structures, the script works correctly with primitive types, Strings, and basic collections

**What works:**
- ✅ Primitive wrappers (Integer, Long, Float, Double, Boolean)
- ✅ Strings
- ✅ Simple nested objects with primitive fields
- ✅ ArrayList (in most cases)
- ❌ LinkedHashMap (known bug in javaobj-py3)

## Implementation Details

The script:
1. Reads binary data from stdin (redis-cli output)
2. Parses alternating field/value pairs from HGETALL
3. Detects Java serialization magic bytes (`0xAC 0xED`)
4. Extracts primitive values from serialized objects
5. Falls back to base64 encoding for unparseable data
