package com.example.twentyfivemediamanager.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PathOperationValidator {

    private final SecurePathResolver resolver;

    public void validateSameAuthorizedRoot(String source, String target) {
        String sourceRoot = extractRoot(source);
        String targetRoot = extractRoot(target);

        if (sourceRoot == null || targetRoot == null || !sourceRoot.equals(targetRoot)) {
            throw new SecurityException("Cross-root operations are not allowed");
        }

        resolver.resolveRelativePath(source);
        resolver.resolveRelativePath(target);
    }

    public String extractRoot(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        String normalized = path.replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        int idx = normalized.indexOf('/');
        return idx >= 0 ? normalized.substring(0, idx) : normalized;
    }
}