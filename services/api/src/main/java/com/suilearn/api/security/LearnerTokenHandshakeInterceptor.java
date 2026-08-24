package com.suilearn.api.security;

import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public final class LearnerTokenHandshakeInterceptor implements HandshakeInterceptor {
    public static final String LEARNER_ID_ATTRIBUTE = "suilearn.learnerId";
    public static final String AUTH_FAILED_ATTRIBUTE = "suilearn.authFailed";

    private final LearnerTokenRegistry registry;
    private final AgentAuthProperties properties;

    public LearnerTokenHandshakeInterceptor(LearnerTokenRegistry registry, AgentAuthProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!properties.isEnabled()) {
            return true;
        }
        String authorization = request.getHeaders().getFirst("Authorization");
        String token = null;
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = authorization.substring(7).strip();
        } else if (request.getURI() != null && request.getURI().getQuery() != null) {
            token = queryParameter(request.getURI().getQuery(), "access_token");
        }
        var principal = registry.resolve(token == null ? "" : token);
        if (principal.isPresent()) {
            attributes.put(LEARNER_ID_ATTRIBUTE, principal.orElseThrow().learnerId());
            attributes.put(AUTH_FAILED_ATTRIBUTE, false);
        } else {
            attributes.put(AUTH_FAILED_ATTRIBUTE, true);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {
    }

    private static String queryParameter(String query, String name) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String pair : query.split("&")) {
            int equals = pair.indexOf('=');
            if (equals < 0) {
                continue;
            }
            if (name.equals(pair.substring(0, equals).strip())) {
                return pair.substring(equals + 1).strip();
            }
        }
        return null;
    }
}
