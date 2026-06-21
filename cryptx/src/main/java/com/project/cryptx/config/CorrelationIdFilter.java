package com.project.cryptx.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    private static final String MDC_KEY = "CRYPTX_uniqueId";
    private static final String HEADER_NAME = "CRYPTX_uniqueId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Generate a clean 12-character unique ID
        String uniqueId = "CRYPTX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        MDC.put(MDC_KEY, uniqueId);

        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader(HEADER_NAME, uniqueId);
            // Expose the header to frontend running on different origins (CORS)
            httpResponse.setHeader("Access-Control-Expose-Headers", HEADER_NAME);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
