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
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (hasText(origin)) {
            String caller = extractHostAndPort(origin);
            if (caller != null) {
                log.debug("Caller resolved from Origin. origin={}, caller={}", origin, caller);
                return caller;
            }
        }

        String referer = request.getHeader(HttpHeaders.REFERER);
        if (hasText(referer)) {
            String caller = extractHostAndPort(referer);
            if (caller != null) {
                log.debug("Caller resolved from Referer. referer={}, caller={}", referer, caller);
                return caller;
            }
        }

        String forwardedHost = firstHeaderValue(request, "X-Forwarded-Host");
        if (hasText(forwardedHost)) {
            String caller = normalizeHostAndPort(forwardedHost);
            log.debug("Caller resolved from X-Forwarded-Host. forwardedHost={}, caller={}", forwardedHost, caller);
            return caller;
        }

        String host = request.getHeader(HttpHeaders.HOST);
        if (hasText(host)) {
            String caller = normalizeHostAndPort(host);
            log.debug("Caller resolved from Host. host={}, caller={}", host, caller);
            return caller;
        }

        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        if (hasText(serverName)) {
            String caller = normalizeHostAndPort(serverName + ":" + serverPort);
            log.debug("Caller resolved from serverName/serverPort. serverName={}, serverPort={}, caller={}",
                    serverName, serverPort, caller);
            return caller;
        }

        log.debug("Unable to resolve caller from request");
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