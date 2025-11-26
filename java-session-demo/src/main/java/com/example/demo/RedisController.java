package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Controller demonstrating direct Redis operations using RedisTemplate.
 * This provides examples of reading and writing data to Redis without using
 * Spring Session.
 */
@RestController
@RequestMapping("/api/redis")
public class RedisController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Write a simple string value to Redis.
     * 
     * Example: POST /api/redis/string
     * Body: {"key": "mykey", "value": "myvalue", "ttl": 300}
     */
    @PostMapping("/string")
    public Map<String, Object> writeString(@RequestBody Map<String, Object> payload) {
        String key = (String) payload.get("key");
        String value = (String) payload.get("value");
        Integer ttl = payload.containsKey("ttl") ? (Integer) payload.get("ttl") : null;

        if (ttl != null && ttl > 0) {
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(key, value);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("key", key);
        result.put("value", value);
        if (ttl != null) {
            result.put("ttl", ttl);
        }
        return result;
    }

    /**
     * Read a string value from Redis.
     * 
     * Example: GET /api/redis/string/mykey
     */
    @GetMapping("/string/{key}")
    public Map<String, Object> readString(@PathVariable String key) {
        Object value = redisTemplate.opsForValue().get(key);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("value", value);
        result.put("exists", value != null);
        if (ttl != null && ttl > 0) {
            result.put("ttl", ttl);
        }
        return result;
    }

    /**
     * Write a hash (map) to Redis.
     * 
     * Example: POST /api/redis/hash
     * Body: {"key": "user:123", "data": {"name": "John", "age": 30, "city": "NYC"}}
     */
    @PostMapping("/hash")
    public Map<String, Object> writeHash(@RequestBody Map<String, Object> payload) {
        String key = (String) payload.get("key");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        Integer ttl = payload.containsKey("ttl") ? (Integer) payload.get("ttl") : null;

        redisTemplate.opsForHash().putAll(key, data);

        if (ttl != null && ttl > 0) {
            redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("key", key);
        result.put("fieldCount", data.size());
        if (ttl != null) {
            result.put("ttl", ttl);
        }
        return result;
    }

    /**
     * Read a hash (map) from Redis.
     * 
     * Example: GET /api/redis/hash/user:123
     */
    @GetMapping("/hash/{key}")
    public Map<String, Object> readHash(@PathVariable String key) {
        Map<Object, Object> hashData = redisTemplate.opsForHash().entries(key);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("data", hashData);
        result.put("exists", !hashData.isEmpty());
        result.put("fieldCount", hashData.size());
        if (ttl != null && ttl > 0) {
            result.put("ttl", ttl);
        }
        return result;
    }

    /**
     * Write a list to Redis.
     * 
     * Example: POST /api/redis/list
     * Body: {"key": "tasks", "values": ["task1", "task2", "task3"]}
     */
    @PostMapping("/list")
    public Map<String, Object> writeList(@RequestBody Map<String, Object> payload) {
        String key = (String) payload.get("key");
        @SuppressWarnings("unchecked")
        java.util.List<String> values = (java.util.List<String>) payload.get("values");
        Integer ttl = payload.containsKey("ttl") ? (Integer) payload.get("ttl") : null;

        // Clear existing list and add new values
        redisTemplate.delete(key);
        redisTemplate.opsForList().rightPushAll(key, values.toArray());

        if (ttl != null && ttl > 0) {
            redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("key", key);
        result.put("count", values.size());
        if (ttl != null) {
            result.put("ttl", ttl);
        }
        return result;
    }

    /**
     * Read a list from Redis.
     * 
     * Example: GET /api/redis/list/tasks
     */
    @GetMapping("/list/{key}")
    public Map<String, Object> readList(@PathVariable String key) {
        Long size = redisTemplate.opsForList().size(key);
        java.util.List<Object> values = size != null && size > 0
                ? redisTemplate.opsForList().range(key, 0, -1)
                : new java.util.ArrayList<>();
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("values", values);
        result.put("exists", size != null && size > 0);
        result.put("count", size != null ? size : 0);
        if (ttl != null && ttl > 0) {
            result.put("ttl", ttl);
        }
        return result;
    }

    /**
     * Delete a key from Redis.
     * 
     * Example: DELETE /api/redis/mykey
     */
    @DeleteMapping("/{key}")
    public Map<String, Object> deleteKey(@PathVariable String key) {
        Boolean deleted = redisTemplate.delete(key);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("deleted", deleted != null && deleted);
        return result;
    }

    /**
     * Get all keys matching a pattern.
     * 
     * Example: GET /api/redis/keys?pattern=user:*
     */
    @GetMapping("/keys")
    public Map<String, Object> getKeys(@RequestParam(defaultValue = "*") String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);

        Map<String, Object> result = new HashMap<>();
        result.put("pattern", pattern);
        result.put("keys", keys);
        result.put("count", keys != null ? keys.size() : 0);
        return result;
    }

    /**
     * Increment a counter in Redis.
     * 
     * Example: POST /api/redis/increment
     * Body: {"key": "counter", "delta": 1}
     */
    @PostMapping("/increment")
    public Map<String, Object> increment(@RequestBody Map<String, Object> payload) {
        String key = (String) payload.get("key");
        Integer delta = payload.containsKey("delta") ? (Integer) payload.get("delta") : 1;

        Long newValue = redisTemplate.opsForValue().increment(key, delta);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("value", newValue);
        result.put("delta", delta);
        return result;
    }

    /**
     * Test endpoint to demonstrate complex object storage.
     * 
     * Example: POST /api/redis/object
     * Body: {"key": "person:1", "data": {"name": "Alice", "age": 25, "hobbies":
     * ["reading", "coding"]}}
     */
    @PostMapping("/object")
    public Map<String, Object> writeObject(@RequestBody Map<String, Object> payload) {
        String key = (String) payload.get("key");
        Object data = payload.get("data");
        Integer ttl = payload.containsKey("ttl") ? (Integer) payload.get("ttl") : null;

        redisTemplate.opsForValue().set(key, data);

        if (ttl != null && ttl > 0) {
            redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("key", key);
        result.put("dataType", data.getClass().getSimpleName());
        if (ttl != null) {
            result.put("ttl", ttl);
        }
        return result;
    }

    /**
     * Read a complex object from Redis.
     * 
     * Example: GET /api/redis/object/person:1
     */
    @GetMapping("/object/{key}")
    public Map<String, Object> readObject(@PathVariable String key) {
        Object value = redisTemplate.opsForValue().get(key);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("data", value);
        result.put("exists", value != null);
        if (value != null) {
            result.put("dataType", value.getClass().getSimpleName());
        }
        if (ttl != null && ttl > 0) {
            result.put("ttl", ttl);
        }
        return result;
    }
}
