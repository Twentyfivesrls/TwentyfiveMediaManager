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
        Set<String> allowedRoots = callerToRoots.getOrDefault(normalizedCaller, Set.of());
        boolean allowed = allowedRoots.contains(root);

        log.debug("Caller/root policy evaluated. caller={}, normalizedCaller={}, root={}, allowedRoots={}, allowed={}",
                caller, normalizedCaller, root, allowedRoots, allowed);

        return allowed;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}