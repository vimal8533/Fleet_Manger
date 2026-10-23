package com.example.fleetmanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor protecting state-modifying robot control operations (POST, PUT, DELETE, and manual monitor triggers).
 * If ROBOT_API_KEY is configured in the environment, callers must provide a matching 'X-API-KEY' header.
 * When ROBOT_API_KEY is empty/unset (e.g. local dev & automated tests), requests proceed without restriction.
 */
@Component
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    @Value("${robot.security.api-key:}")
    private String configuredApiKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // If no API key is enforced in environment, permit all requests
        if (configuredApiKey == null || configuredApiKey.trim().isEmpty()) {
            return true;
        }

        String method = request.getMethod();
        String path = request.getRequestURI();

        // Mutating robot control APIs or manual monitoring triggers require API key
        boolean isProtectedAction = "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)
                || path.contains("/monitor");

        if (isProtectedAction) {
            String clientKey = request.getHeader("X-API-KEY");
            if (clientKey == null || !clientKey.trim().equals(configuredApiKey.trim())) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized: Missing or invalid 'X-API-KEY' header for robot control operation"
                );
            }
        }

        return true;
    }
}
