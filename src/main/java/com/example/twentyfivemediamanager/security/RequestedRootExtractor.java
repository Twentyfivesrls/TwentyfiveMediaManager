package com.example.twentyfivemediamanager.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RequestedRootExtractor {

    public String extractRequestedRoot(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (log.isDebugEnabled()) {
            log.debug("Extracting requested root from uri={}", uri);
        }

        if (uri.contains("/uploadkkk/")) {
            return extractFirstSegmentAfter(uri, "/uploadkkk/");
        }
        if (uri.contains("/downloadkkk/")) {
            return extractFirstSegmentAfter(uri, "/downloadkkk/");
        }
        if (uri.contains("/deletekkk/")) {
            return extractFirstSegmentAfter(uri, "/deletekkk/");
        }
        if (uri.contains("/infokkk/")) {
            return extractFirstSegmentAfter(uri, "/infokkk/");
        }
        if (uri.contains("/copykkk")) {
            return extractFirstSegment(request.getParameter("source"));
        }
        if (uri.contains("/renamekkk")) {
            return extractFirstSegment(request.getParameter("source"));
        }
        if (uri.contains("/delete-folderkkk")) {
            return extractFirstSegment(request.getParameter("target"));
        }

        log.debug("No matching legacy marker found for uri={}", uri);
        return null;
    }

    private String extractFirstSegmentAfter(String uri, String marker) {
        String value = uri.substring(uri.indexOf(marker) + marker.length());
        String result = extractFirstSegment(value);

        if (log.isDebugEnabled()) {
            log.debug("Extracted first segment after marker. marker={}, rawValue={}, extractedRoot={}",
                    marker, value, result);
        }

        return result;
    }

    private String extractFirstSegment(String value) {
        if (value == null || value.isBlank()) {
            log.debug("Cannot extract root from blank value");
            return null;
        }

        String decoded = URLDecoder.decode(value, StandardCharsets.UTF_8);
        String normalized = decoded.replace("\\", "/");

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        int slashIndex = normalized.indexOf('/');
        String result = slashIndex >= 0 ? normalized.substring(0, slashIndex) : normalized;

        if (log.isDebugEnabled()) {
            log.debug("Normalized path for root extraction. input={}, decoded={}, normalized={}, extractedRoot={}",
                    value, decoded, normalized, result);
        }

        return result;
    }
}