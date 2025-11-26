package com.example.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.SaveMode;

/**
 * Spring Session configuration.
 * Configures Redis-backed HTTP sessions with SaveMode.ALWAYS.
 * 
 * SaveMode.ALWAYS ensures that the session is saved on every request,
 * even if only nested object properties are modified. This is crucial
 * for detecting changes in deeply nested objects.
 */
@Configuration
@EnableRedisHttpSession(saveMode = SaveMode.ALWAYS)
public class SessionConfig {
    // SaveMode.ALWAYS will persist session data on every request
    // This ensures nested object modifications are detected and saved
}
