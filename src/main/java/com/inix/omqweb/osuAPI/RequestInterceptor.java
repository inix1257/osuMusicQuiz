package com.inix.omqweb.osuAPI;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class RequestInterceptor implements HandlerInterceptor {
    private final osuAPIService osuAPIService;
    private final ConcurrentMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private Logger logger = LoggerFactory.getLogger(RequestInterceptor.class);

    @PostConstruct
    public void init() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(requestCounts::clear, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try{
//            String clientIP = request.getRemoteAddr();
//            AtomicInteger count = requestCounts.computeIfAbsent(clientIP, k -> new AtomicInteger(0));
//
//            int MAX_REQUESTS_PER_MINUTE = 10000;
//            if (count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
//                response.setStatus(HttpStatus.SC_TOO_MANY_REQUESTS);
//                return false;
//            }

            String requestURL = request.getRequestURI();

            // If the request is for refreshing the token, skip the usual token validation
            if (requestURL.equals("/api/getRefreshToken")) {
                return true;
            }

            String token = request.getHeader("Authorization");

            if (token == null) {
                if (requestURL.equals("/api/daily")) {
                    return true;
                }
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            ResponseEntity<Player> userInfo = osuAPIService.getMeByToken(token);

            if (userInfo == null) {
                if (requestURL.equals("/api/daily")) {
                    return true;
                }
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            if (userInfo.getStatusCode() == HttpStatusCode.valueOf(401)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            } else if (userInfo.getStatusCode() == HttpStatusCode.valueOf(429)) {
                response.setStatus(429);
                return false;
            }

            if (userInfo.getBody().getJoin_date().after(Date.from(LocalDateTime.now().minusMonths(1).atZone(ZoneId.systemDefault()).toInstant()))) {
                response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                return false;
            }

            if (userInfo.getBody().isBan()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }

            request.setAttribute("userInfo", userInfo.getBody());
        }catch (Exception e){
            logger.error("RequestInterceptor Error: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }

        return true;
    }
}
