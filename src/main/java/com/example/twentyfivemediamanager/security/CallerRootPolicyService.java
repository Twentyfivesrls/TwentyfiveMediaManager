package com.example.twentyfivemediamanager.security;

import com.example.twentyfivemediamanager.config.FileSecurityProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class CallerRootPolicyService {

    private final Map<String, Set<String>> callerToRoots = new HashMap<>();

    public CallerRootPolicyService(FileSecurityProperties properties) {
        for (FileSecurityProperties.CallerPolicy policy : properties.getCallerPolicies()) {
            callerToRoots
                    .computeIfAbsent(normalize(policy.getCaller()), k -> new HashSet<>())
                    .addAll(policy.getRoots());
        }
    }

    @PostConstruct
    public void logLoadedPolicies() {
        log.info("Loaded caller/root policies: {}", callerToRoots);
    }

    public boolean isAllowed(String caller, String root) {
        if (caller == null || caller.isBlank() || root == null || root.isBlank()) {
            log.debug("Caller/root validation failed because caller or root is blank. caller={}, root={}",
                    caller, root);
            return false;
        }

        String normalizedCaller = normalize(caller);

        Set<String> exactAllowedRoots = callerToRoots.getOrDefault(normalizedCaller, Set.of());
        if (exactAllowedRoots.contains(root)) {
            log.debug("Caller/root matched with exact caller. caller={}, root={}, allowedRoots={}",
                    normalizedCaller, root, exactAllowedRoots);
            return true;
        }

        String callerWithoutPort = stripPort(normalizedCaller);
        Set<String> fallbackAllowedRoots = callerToRoots.getOrDefault(callerWithoutPort, Set.of());
        boolean allowed = fallbackAllowedRoots.contains(root);

        log.debug("Caller/root fallback evaluation. caller={}, callerWithoutPort={}, root={}, exactAllowedRoots={}, fallbackAllowedRoots={}, allowed={}",
                normalizedCaller, callerWithoutPort, root, exactAllowedRoots, fallbackAllowedRoots, allowed);

        return allowed;
    }

    private String stripPort(String caller) {
        int colonIndex = caller.lastIndexOf(':');
        if (colonIndex > 0) {
            String possiblePort = caller.substring(colonIndex + 1);
            if (possiblePort.matches("\\d+")) {
                return caller.substring(0, colonIndex);
            }
        }
        return caller;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}