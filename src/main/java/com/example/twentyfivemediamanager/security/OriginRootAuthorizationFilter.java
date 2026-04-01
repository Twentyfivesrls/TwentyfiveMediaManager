package com.example.twentyfivemediamanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OriginRootAuthorizationFilter extends OncePerRequestFilter {

    private final RequestedRootExtractor requestedRootExtractor;
    private final OriginRootPolicyService policyService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean skip = !request.getRequestURI().contains("/twentyfiveserver/");
        if (skip && log.isDebugEnabled()) {
            log.debug("Skipping OriginRootAuthorizationFilter for uri={}", request.getRequestURI());
        }
        return skip;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            if (log.isDebugEnabled()) {
                log.debug("Allowing preflight request. method={}, uri={}", request.getMethod(), request.getRequestURI());
            }
            filterChain.doFilter(request, response);
            return;
        }

        String origin = request.getHeader(HttpHeaders.ORIGIN);
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (log.isDebugEnabled()) {
            log.debug("Incoming protected request. method={}, uri={}, origin={}", method, uri, origin);
        }

        String requestedRoot = requestedRootExtractor.extractRequestedRoot(request);

        if (log.isDebugEnabled()) {
            log.debug("Extracted requested root. method={}, uri={}, origin={}, requestedRoot={}",
                    method, uri, origin, requestedRoot);
        }

        if (requestedRoot == null || requestedRoot.isBlank()) {
            log.warn("Blocked request: requested root missing. method={}, uri={}, origin={}", method, uri, origin);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        if (origin == null || origin.isBlank()) {
            log.warn("Blocked request: origin missing. method={}, uri={}, requestedRoot={}", method, uri, requestedRoot);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        boolean allowed = policyService.isAllowed(origin, requestedRoot);

        if (log.isDebugEnabled()) {
            log.debug("Policy evaluation completed. origin={}, requestedRoot={}, allowed={}", origin, requestedRoot, allowed);
        }

        if (!allowed) {
            log.warn("Blocked request: origin not allowed for root. method={}, uri={}, origin={}, requestedRoot={}",
                    method, uri, origin, requestedRoot);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        log.info("Allowed request by origin/root policy. method={}, uri={}, origin={}, requestedRoot={}",
                method, uri, origin, requestedRoot);

        filterChain.doFilter(request, response);
    }
}