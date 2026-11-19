package com.microservice.gateway.security;

import com.microservice.common.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * First line of defence: rejects any request without a valid JWT before it reaches a service.
 * Public routes: POST /api/users/login, POST /api/users/register, GET /api/products/**
 * (ecommerce-service and payment-service also validate the token themselves).
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    public JwtAuthGlobalFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        String path = request.getURI().getPath();

        // CORS preflight requests carry no token
        if (HttpMethod.OPTIONS.equals(method) || isPublic(method, path)) {
            return chain.filter(exchange);
        }

        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        try {
            jwtUtil.validate(header.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange);
        }
        return chain.filter(exchange);
    }

    private boolean isPublic(HttpMethod method, String path) {
        if (path.contains("..")) {
            return false; // never treat path-traversal tricks as public
        }
        if (HttpMethod.POST.equals(method)
                && (path.equals("/api/users/login") || path.equals("/api/users/register"))) {
            return true;
        }
        return HttpMethod.GET.equals(method)
                && (path.equals("/api/products") || path.startsWith("/api/products/"));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"error\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -1; // run before routing filters
    }
}