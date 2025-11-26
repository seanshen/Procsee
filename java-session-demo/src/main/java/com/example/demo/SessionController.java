package com.example.demo;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
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

    /**
     * Create and store a deeply nested Employee object in the session.
     * 
     * Example request body:
     * {
     * "id": 1001,
     * "name": "John Doe",
     * "title": "Senior Developer",
     * "salary": 95000.0,
     * "contact": {
     * "phone": "555-1234",
     * "email": "john@example.com",
     * "address": {
     * "street": "123 Main St",
     * "city": "New York",
     * "state": "NY",
     * "zipCode": "10001"
     * }
     * },
     * "company": {
     * "name": "Tech Corp",
     * "industry": "Software",
     * "contact": {
     * "phone": "555-5678",
     * "email": "info@techcorp.com",
     * "address": {
     * "street": "456 Corporate Blvd",
     * "city": "San Francisco",
     * "state": "CA",
     * "zipCode": "94102"
     * }
     * }
     * }
     * }
     */
    @PostMapping("/employee")
    public Map<String, Object> createEmployee(@RequestBody Map<String, Object> payload,
            HttpSession session, HttpServletResponse response) {
        response.setHeader("session-id", session.getId());

        // Create Employee from payload
        Employee employee = new Employee();
        employee.setId(((Number) payload.get("id")).longValue());
        employee.setName((String) payload.get("name"));
        employee.setTitle((String) payload.get("title"));
        employee.setSalary(((Number) payload.get("salary")).doubleValue());

        // Create personal Contact
        @SuppressWarnings("unchecked")
        Map<String, Object> contactData = (Map<String, Object>) payload.get("contact");
        if (contactData != null) {
            Contact contact = new Contact();
            contact.setPhone((String) contactData.get("phone"));
            contact.setEmail((String) contactData.get("email"));

            @SuppressWarnings("unchecked")
            Map<String, Object> addressData = (Map<String, Object>) contactData.get("address");
            if (addressData != null) {
                Address address = new Address(
                        (String) addressData.get("street"),
                        (String) addressData.get("city"),
                        (String) addressData.get("state"),
                        (String) addressData.get("zipCode"));
                contact.setAddress(address);
            }
            employee.setContact(contact);
        }

        // Create Company with nested Contact
        @SuppressWarnings("unchecked")
        Map<String, Object> companyData = (Map<String, Object>) payload.get("company");
        if (companyData != null) {
            Company company = new Company();
            company.setName((String) companyData.get("name"));
            company.setIndustry((String) companyData.get("industry"));

            @SuppressWarnings("unchecked")
            Map<String, Object> companyContactData = (Map<String, Object>) companyData.get("contact");
            if (companyContactData != null) {
                Contact companyContact = new Contact();
                companyContact.setPhone((String) companyContactData.get("phone"));
                companyContact.setEmail((String) companyContactData.get("email"));

                @SuppressWarnings("unchecked")
                Map<String, Object> companyAddressData = (Map<String, Object>) companyContactData.get("address");
                if (companyAddressData != null) {
                    Address companyAddress = new Address(
                            (String) companyAddressData.get("street"),
                            (String) companyAddressData.get("city"),
                            (String) companyAddressData.get("state"),
                            (String) companyAddressData.get("zipCode"));
                    companyContact.setAddress(companyAddress);
                }
                company.setContact(companyContact);
            }
            employee.setCompany(company);
        }

        // Store in session
        session.setAttribute("employee", employee);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("employee", employee);
        result.put("serverPort", serverPort);
        result.put("sessionId", session.getId());
        return result;
    }

    /**
     * Retrieve the Employee object from the session.
     */
    @GetMapping("/employee")
    public Map<String, Object> getEmployee(HttpSession session, HttpServletResponse response) {
        response.setHeader("session-id", session.getId());

        Employee employee = (Employee) session.getAttribute("employee");

        Map<String, Object> result = new HashMap<>();
        result.put("employee", employee);
        result.put("exists", employee != null);
        result.put("serverPort", serverPort);
        result.put("sessionId", session.getId());
        return result;
    }

    /**
     * Modify a nested property of the Employee object.
     * 
     * Example request body:
     * {"path": "name", "value": "Jane Doe"}
     * {"path": "contact.email", "value": "jane@example.com"}
     * {"path": "company.contact.address.city", "value": "Boston"}
     */
    @PutMapping("/employee/nested")
    public Map<String, Object> modifyNestedProperty(@RequestBody Map<String, Object> payload,
            HttpSession session, HttpServletResponse response) {
        response.setHeader("session-id", session.getId());

        Employee employee = (Employee) session.getAttribute("employee");
        if (employee == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No employee found in session");
            error.put("sessionId", session.getId());
            return error;
        }

        String path = (String) payload.get("path");
        Object value = payload.get("value");

        try {
            setNestedProperty(employee, path, value);
            // With SaveMode.ALWAYS the following line is not needed
            // session.setAttribute("employee", employee);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("path", path);
            result.put("value", value);
            result.put("employee", employee);
            result.put("serverPort", serverPort);
            result.put("sessionId", session.getId());
            return result;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to set property: " + e.getMessage());
            error.put("path", path);
            error.put("sessionId", session.getId());
            return error;
        }
    }

    /**
     * Get a specific nested property value.
     * 
     * Example: GET /api/session/employee/nested?path=company.contact.address.city
     */
    @GetMapping("/employee/nested")
    public Map<String, Object> getNestedProperty(@RequestParam String path,
            HttpSession session, HttpServletResponse response) {
        response.setHeader("session-id", session.getId());

        Employee employee = (Employee) session.getAttribute("employee");
        if (employee == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No employee found in session");
            error.put("sessionId", session.getId());
            return error;
        }

        try {
            Object value = getNestedProperty(employee, path);

            Map<String, Object> result = new HashMap<>();
            result.put("path", path);
            result.put("value", value);
            result.put("serverPort", serverPort);
            result.put("sessionId", session.getId());
            return result;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get property: " + e.getMessage());
            error.put("path", path);
            error.put("sessionId", session.getId());
            return error;
        }
    }

    /**
     * Helper method to set a nested property using reflection.
     */
    private void setNestedProperty(Object obj, String path, Object value) throws Exception {
        String[] parts = path.split("\\.");
        Object current = obj;

        // Navigate to the parent object
        for (int i = 0; i < parts.length - 1; i++) {
            Field field = current.getClass().getDeclaredField(parts[i]);
            field.setAccessible(true);
            current = field.get(current);
            if (current == null) {
                throw new IllegalStateException("Null value encountered at path: " + parts[i]);
            }
        }

        // Set the final property
        String finalProperty = parts[parts.length - 1];
        Field field = current.getClass().getDeclaredField(finalProperty);
        field.setAccessible(true);

        // Convert value to appropriate type
        Object convertedValue = convertValue(value, field.getType());
        field.set(current, convertedValue);
    }

    /**
     * Helper method to get a nested property using reflection.
     */
    private Object getNestedProperty(Object obj, String path) throws Exception {
        String[] parts = path.split("\\.");
        Object current = obj;

        for (String part : parts) {
            Field field = current.getClass().getDeclaredField(part);
            field.setAccessible(true);
            current = field.get(current);
            if (current == null) {
                return null;
            }
        }

        return current;
    }

    /**
     * Convert value to the target type.
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == Long.class || targetType == long.class) {
            return ((Number) value).longValue();
        } else if (targetType == Integer.class || targetType == int.class) {
            return ((Number) value).intValue();
        } else if (targetType == Double.class || targetType == double.class) {
            return ((Number) value).doubleValue();
        } else if (targetType == Float.class || targetType == float.class) {
            return ((Number) value).floatValue();
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.valueOf(value.toString());
        }

        return value;
    }
}
