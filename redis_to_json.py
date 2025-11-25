#!/usr/bin/env python3
"""
Convert Redis hash data to JSON format.
Handles Spring Session data stored in Redis, including nested Java objects.

This script connects directly to Redis to avoid issues with redis-cli output parsing.

Usage:
    # Get a specific session
    python3 redis_to_json.py "spring:session:sessions:SESSION_ID"
    
    # Specify Redis host/port
    python3 redis_to_json.py "spring:session:sessions:SESSION_ID" --host localhost --port 6379
    
    # Use with Docker
    python3 redis_to_json.py "spring:session:sessions:SESSION_ID" --host localhost --port 6379

Requirements:
    pip3 install redis javaobj-py3
"""

import sys
import json
import base64
import argparse
from typing import Any, Dict

try:
    import redis
    REDIS_AVAILABLE = True
except ImportError:
    REDIS_AVAILABLE = False
    print("Error: redis-py not installed.", file=sys.stderr)
    print("Install with: pip3 install redis", file=sys.stderr)
    sys.exit(1)

try:
    import javaobj
    JAVAOBJ_AVAILABLE = True
except ImportError:
    JAVAOBJ_AVAILABLE = False
    print("Warning: javaobj-py3 not installed. Complex objects will be base64-encoded.", file=sys.stderr)
    print("Install with: pip3 install javaobj-py3", file=sys.stderr)


def java_to_python(obj: Any) -> Any:
    """
    Recursively convert Java objects to Python-native JSON-serializable types.
    Handles nested structures, collections, and custom objects.
    """
    # Handle None
    if obj is None:
        return None
    
    # Handle primitive types
    if isinstance(obj, (bool, int, float, str)):
        return obj
    
    # Handle bytes
    if isinstance(obj, bytes):
        try:
            return obj.decode('utf-8')
        except:
            return base64.b64encode(obj).decode('ascii')
    
    # Handle lists/tuples (Java ArrayList, arrays, etc.)
    if isinstance(obj, (list, tuple)):
        return [java_to_python(item) for item in obj]
    
    # Handle dictionaries (Java HashMap, etc.)
    if isinstance(obj, dict):
        return {str(key): java_to_python(value) for key, value in obj.items()}
    
    # Handle javaobj JavaObject instances
    if hasattr(obj, 'classdesc') and hasattr(obj, '__dict__'):
        class_name = getattr(obj.classdesc, 'name', '') if hasattr(obj, 'classdesc') else ''
        
        # Special handling for Java primitive wrappers
        if class_name in ['java.lang.Integer', 'java.lang.Long', 'java.lang.Float', 
                          'java.lang.Double', 'java.lang.Boolean', 'java.lang.Byte',
                          'java.lang.Short', 'java.lang.Character']:
            # Extract the primitive value
            if hasattr(obj, 'value'):
                return obj.value
        
        # Special handling for String
        if class_name == 'java.lang.String':
            if hasattr(obj, 'value'):
                return obj.value
        
        # For other Java objects, extract fields (excluding metadata)
        result = {}
        for key, value in obj.__dict__.items():
            # Skip internal javaobj metadata
            if key not in ['classdesc', 'annotations', '_fields']:
                result[key] = java_to_python(value)
        
        # If the result only has one field, return just that value
        # This handles simple wrapper objects
        if len(result) == 1 and 'value' in result:
            return result['value']
        
        return result if result else str(obj)
    
    # Handle other objects with __dict__ attribute
    if hasattr(obj, '__dict__'):
        result = {}
        for key, value in obj.__dict__.items():
            # Skip private/internal attributes
            if not key.startswith('_'):
                result[key] = java_to_python(value)
        return result
    
    # Fallback: convert to string
    try:
        return str(obj)
    except:
        return f"<unparseable: {type(obj).__name__}>"


def parse_java_serialized_value(data: bytes) -> Any:
    """
    Parse Java serialized objects using javaobj-py3.
    Returns the parsed value or base64-encoded string if parsing fails.
    """
    if not data:
        return None
    
    # Check if it's a Java serialized object (starts with 0xAC 0xED)
    if len(data) >= 2 and data[0] == 0xAC and data[1] == 0xED:
        if JAVAOBJ_AVAILABLE:
            try:
                # Use javaobj to deserialize
                java_obj = javaobj.loads(data)
                # Convert to Python-native types
                return java_to_python(java_obj)
            except Exception as e:
                # If deserialization fails, return base64 with error info
                return {
                    "_type": "java_serialized",
                    "_error": str(e),
                    "_base64": base64.b64encode(data).decode('ascii')
                }
        else:
            # javaobj not available, return base64
            return {
                "_type": "java_serialized",
                "_note": "Install javaobj-py3 to deserialize",
                "_base64": base64.b64encode(data).decode('ascii')
            }
    
    # Try to decode as UTF-8 string
    try:
        return data.decode('utf-8')
    except:
        # Return base64-encoded if not parseable
        return {
            "_type": "binary",
            "_base64": base64.b64encode(data).decode('ascii')
        }


def get_redis_hash(redis_client: redis.Redis, key: str) -> Dict[str, Any]:
    """
    Get a Redis hash and convert all values from Java serialized format to JSON.
    """
    # Get all fields and values from the hash
    hash_data = redis_client.hgetall(key)
    
    if not hash_data:
        return {"error": f"Key '{key}' not found or is empty"}
    
    result = {}
    for field_bytes, value_bytes in hash_data.items():
        # Decode field name (always UTF-8)
        field = field_bytes.decode('utf-8')
        
        # Parse the value (might be Java serialized)
        parsed_value = parse_java_serialized_value(value_bytes)
        
        result[field] = parsed_value
    
    return result


def main():
    """Main function to connect to Redis and convert hash data to JSON."""
    parser = argparse.ArgumentParser(
        description='Convert Redis hash data to JSON, with support for Java serialized objects.'
    )
    parser.add_argument('key', help='Redis key to retrieve (e.g., "spring:session:sessions:SESSION_ID")')
    parser.add_argument('--host', default='localhost', help='Redis host (default: localhost)')
    parser.add_argument('--port', type=int, default=6379, help='Redis port (default: 6379)')
    parser.add_argument('--db', type=int, default=0, help='Redis database number (default: 0)')
    parser.add_argument('--password', help='Redis password (if required)')
    
    args = parser.parse_args()
    
    try:
        # Connect to Redis
        redis_client = redis.Redis(
            host=args.host,
            port=args.port,
            db=args.db,
            password=args.password,
            decode_responses=False  # Important: get raw bytes
        )
        
        # Test connection
        redis_client.ping()
        
        # Get and convert the hash
        result = get_redis_hash(redis_client, args.key)
        
        # Output as JSON
        print(json.dumps(result, indent=2, ensure_ascii=False))
        
    except redis.ConnectionError as e:
        print(json.dumps({"error": f"Failed to connect to Redis: {e}"}, indent=2), file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(json.dumps({"error": f"Unexpected error: {e}"}, indent=2), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
