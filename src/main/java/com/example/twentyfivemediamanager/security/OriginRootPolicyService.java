package com.example.twentyfivemediamanager.security;

import com.example.twentyfivemediamanager.config.FileSecurityProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class OriginRootPolicyService {

    private final Map<String, Set<String>> originToRoots = new HashMap<>();

    public OriginRootPolicyService(FileSecurityProperties properties) {
        for (FileSecurityProperties.RootPolicy policy : properties.getRootPolicies()) {
            for (String origin : policy.getOrigins()) {
                originToRoots
                        .computeIfAbsent(normalize(origin), k -> new HashSet<>())
                        .add(policy.getRoot());
            }
        }
    }

    @PostConstruct
    public void logLoadedPolicies() {
        log.info("Loaded origin/root policies: {}", originToRoots);
    }

    public boolean isAllowed(String origin, String root) {
        if (origin == null || origin.isBlank() || root == null || root.isBlank()) {
            log.debug("Policy check failed due to blank origin/root. origin={}, root={}", origin, root);
            return false;
        }

        String normalizedOrigin = normalize(origin);
        boolean allowed = originToRoots.getOrDefault(normalizedOrigin, Set.of()).contains(root);

        if (log.isDebugEnabled()) {
            log.debug("Policy check. origin={}, normalizedOrigin={}, root={}, allowedRoots={}, allowed={}",
                    origin,
                    normalizedOrigin,
                    root,
                    originToRoots.getOrDefault(normalizedOrigin, Set.of()),
                    allowed);
        }

        return allowed;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}