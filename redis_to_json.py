#!/usr/bin/env python3
"""
Convert Redis HGETALL output to JSON format.
Handles Spring Session data stored in Redis, including nested Java objects.

Usage:
    redis-cli --raw HGETALL "spring:session:sessions:SESSION_ID" | python3 redis_to_json.py
    
IMPORTANT: Use --raw flag with redis-cli to preserve binary data!

Requirements:
    pip3 install javaobj-py3
"""

import sys
import json
import base64
from typing import Any, Dict

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


def parse_hgetall_raw_output(input_bytes: bytes) -> Dict[str, Any]:
    """
    Parse redis-cli --raw HGETALL output from raw bytes.
    
    The --raw format outputs field and value separated by newlines (0x0a):
    field1\nvalue1\nfield2\nvalue2\n...
    
    This is the recommended way to use this script.
    """
    result = {}
    
    # Split by newline byte
    parts = input_bytes.split(b'\n')
    
    # Process in pairs (field, value)
    i = 0
    while i < len(parts) - 1:  # -1 because we need pairs
        field_bytes = parts[i]
        value_bytes = parts[i + 1]
        
        # Decode field as UTF-8 (field names are always text)
        try:
            field = field_bytes.decode('utf-8').strip()
        except:
            field = field_bytes.decode('latin1').strip()
        
        if not field:  # Skip empty fields
            i += 1
            continue
        
        # Parse the value (which might be binary Java serialized data)
        parsed_value = parse_java_serialized_value(value_bytes)
        
        result[field] = parsed_value
        i += 2
    
    return result


def main():
    """Main function to read from stdin and output JSON."""
    # Read all input in binary mode
    input_bytes = sys.stdin.buffer.read()
    
    if not input_bytes.strip():
        print(json.dumps({"error": "No input data"}, indent=2))
        return
    
    # Parse the HGETALL --raw output
    result = parse_hgetall_raw_output(input_bytes)
    
    # Output as JSON
    print(json.dumps(result, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
