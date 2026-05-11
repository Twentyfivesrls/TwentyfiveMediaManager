package com.example.twentyfivemediamanager.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
public class CallerResolver {

    public String resolveCaller(HttpServletRequest request) {
        String origin        = request.getHeader(HttpHeaders.ORIGIN);
        String referer       = request.getHeader(HttpHeaders.REFERER);
        String forwardedHost = firstHeaderValue(request, "X-Forwarded-Host");
        String host          = request.getHeader(HttpHeaders.HOST);
        String serverName    = request.getServerName();
        int    serverPort    = request.getServerPort();

        log.info("=== CallerResolver: incoming headers ===");
        log.info("  Origin           : {}", origin        != null ? origin        : "<absent>");
        log.info("  Referer          : {}", referer       != null ? referer       : "<absent>");
        log.info("  X-Forwarded-Host : {}", forwardedHost != null ? forwardedHost : "<absent>");
        log.info("  Host             : {}", host          != null ? host          : "<absent>");
        log.info("  serverName:port  : {}:{}", serverName, serverPort);

        if (hasText(origin)) {
            String caller = extractHostAndPort(origin);
            if (caller != null) {
                log.info("  >>> WINNER: Origin -> caller=[{}]", caller);
                return caller;
            }
            log.info("  Origin presente ma non parsabile, si continua");
        }

        if (hasText(referer)) {
            String caller = extractHostAndPort(referer);
            if (caller != null) {
                log.info("  >>> WINNER: Referer -> caller=[{}]", caller);
                return caller;
            }
            log.info("  Referer presente ma non parsabile, si continua");
        }

        if (hasText(forwardedHost)) {
            String caller = normalizeHostAndPort(forwardedHost);
            log.info("  >>> WINNER: X-Forwarded-Host -> caller=[{}]", caller);
            return caller;
        }

        if (hasText(host)) {
            String caller = normalizeHostAndPort(host);
            log.info("  >>> WINNER: Host -> caller=[{}]", caller);
            return caller;
        }

        if (hasText(serverName)) {
            String caller = normalizeHostAndPort(serverName + ":" + serverPort);
            log.info("  >>> WINNER: serverName:port -> caller=[{}]", caller);
            return caller;
        }

        log.info("  >>> WINNER: nessuno - caller non risolvibile");
        return null;
    }

    private String extractHostAndPort(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }

            int port = uri.getPort();
            String value = port > 0 ? host + ":" + port : host;
            return normalizeHostAndPort(value);
        } catch (Exception ex) {
            log.warn("Failed to parse caller URL. url={}", url);
            return null;
        }
    }

    private String normalizeHostAndPort(String value) {
        String normalized = value.trim().toLowerCase();

        if (normalized.contains(",")) {
            normalized = normalized.split(",")[0].trim();
        }

        return normalized;
    }

    private String firstHeaderValue(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (value == null) {
            return null;
        }

        List<String> parts = List.of(value.split(","));
        return parts.isEmpty() ? null : parts.get(0).trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}