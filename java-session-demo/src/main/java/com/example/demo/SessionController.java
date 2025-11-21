package com.example.demo;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    @Value("${server.port}")
    private String serverPort;

    @PostMapping
    public Map<String, Object> setSessionAttribute(@RequestBody Map<String, Object> payload, HttpSession session,
            HttpServletResponse response) {
        response.setHeader("session-id", session.getId());
        if (payload.containsKey("testAttr")) {
            session.setAttribute("testAttr", payload.get("testAttr"));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("serverPort", serverPort);
        result.put("sessionId", session.getId());
        return result;
    }

    @GetMapping
    public Map<String, Object> getSessionAttribute(HttpSession session, HttpServletResponse response) {
        response.setHeader("session-id", session.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("testAttr", session.getAttribute("testAttr"));
        result.put("serverPort", serverPort);
        result.put("sessionId", session.getId());
        return result;
    }
}
