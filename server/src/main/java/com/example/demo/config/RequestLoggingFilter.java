package com.example.demo.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Enumeration;

@Component
public class RequestLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        long startTime = System.currentTimeMillis();

        // 요청 로깅
        logRequest(httpRequest);

        // 요청 처리
        chain.doFilter(request, response);

        // 응답 로깅
        long duration = System.currentTimeMillis() - startTime;
        logResponse(httpRequest, httpResponse, duration);
    }

    private void logRequest(HttpServletRequest request) {
        StringBuilder logMsg = new StringBuilder();
        logMsg.append("\n=== HTTP REQUEST ===\n");
        logMsg.append(String.format("%s %s\n", request.getMethod(), request.getRequestURI()));

        // Query String
        if (request.getQueryString() != null) {
            logMsg.append(String.format("Query: %s\n", request.getQueryString()));
        }

        // Headers
        logMsg.append("Headers:\n");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            logMsg.append(String.format("  %s: %s\n", headerName, headerValue));
        }

        // Client Info
        logMsg.append(String.format("Remote Address: %s\n", request.getRemoteAddr()));
        logMsg.append("===================");

        logger.info(logMsg.toString());
    }

    private void logResponse(HttpServletRequest request, HttpServletResponse response, long duration) {
        logger.info("=== HTTP RESPONSE === {} {} - Status: {} - Duration: {}ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);
    }
}
