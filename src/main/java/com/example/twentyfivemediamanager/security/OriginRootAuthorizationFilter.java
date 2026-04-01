package com.example.twentyfivemediamanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OriginRootAuthorizationFilter extends OncePerRequestFilter {

    private final RequestedRootExtractor requestedRootExtractor;
    private final CallerResolver callerResolver;
    private final CallerRootPolicyService callerRootPolicyService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean skip = !request.getRequestURI().contains("/twentyfiveserver/");
        if (skip && log.isDebugEnabled()) {
            log.debug("Skipping authorization filter for uri={}", request.getRequestURI());
        }
        return skip;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("Allowing OPTIONS request. uri={}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod();
        String uri = request.getRequestURI();

        String caller = callerResolver.resolveCaller(request);
        String requestedRoot = requestedRootExtractor.extractRequestedRoot(request);

        log.debug("Protected request received. method={}, uri={}, caller={}, requestedRoot={}",
                method, uri, caller, requestedRoot);

        if (requestedRoot == null || requestedRoot.isBlank()) {
            log.warn("Blocked request: requested root missing. method={}, uri={}, caller={}",
                    method, uri, caller);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        if (caller == null || caller.isBlank()) {
            log.warn("Blocked request: caller unresolved. method={}, uri={}, requestedRoot={}",
                    method, uri, requestedRoot);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        boolean allowed = callerRootPolicyService.isAllowed(caller, requestedRoot);

        log.debug("Caller/root authorization result. caller={}, requestedRoot={}, allowed={}",
                caller, requestedRoot, allowed);

        if (!allowed) {
            log.warn("Blocked request: caller not allowed for requested root. method={}, uri={}, caller={}, requestedRoot={}",
                    method, uri, caller, requestedRoot);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        log.info("Allowed request. method={}, uri={}, caller={}, requestedRoot={}",
                method, uri, caller, requestedRoot);

        filterChain.doFilter(request, response);
    }
}