package com.example.twentyfivemediamanager.security;

import com.example.twentyfivemediamanager.config.FileSecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurePathResolver {

    private final FileSecurityProperties properties;

    public Path resolveRelativePath(String[] pathSegments) {
        Path current = properties.storageRootPath();

        if (log.isDebugEnabled()) {
            log.debug("Resolving path from segments. storageRoot={}, segments={}",
                    properties.storageRootPath(), java.util.Arrays.toString(pathSegments));
        }

        for (String segment : pathSegments) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            current = current.resolve(segment);
        }

        Path normalized = current.normalize().toAbsolutePath();

        if (log.isDebugEnabled()) {
            log.debug("Resolved path from segments. candidatePath={}, normalizedPath={}", current, normalized);
        }

        if (!normalized.startsWith(properties.storageRootPath())) {
            log.warn("Blocked path traversal attempt. storageRoot={}, resolvedPath={}",
                    properties.storageRootPath(), normalized);
            throw new SecurityException("Path traversal detected");
        }

        return normalized;
    }

    public Path resolveRelativePath(String rawPath) {
        String normalized = rawPath == null ? "" : rawPath.replace("\\", "/");

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (log.isDebugEnabled()) {
            log.debug("Resolving raw path. rawPath={}, normalizedRawPath={}", rawPath, normalized);
        }

        String[] segments = normalized.split("/");
        return resolveRelativePath(segments);
    }
}