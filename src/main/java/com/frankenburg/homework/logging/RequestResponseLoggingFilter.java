package com.frankenburg.homework.logging;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.frankenburg.homework.logging.domain.MessageIn;
import com.frankenburg.homework.logging.domain.MessageOut;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_LOGGED_REQUEST_BODY_BYTES = 64 * 1024;

    private final JsonMapper jsonMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper cachingRequest = new ContentCachingRequestWrapper(request, MAX_LOGGED_REQUEST_BODY_BYTES);
        ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(response);
        OffsetDateTime requestTime = OffsetDateTime.now(ZoneOffset.UTC);
        Throwable unhandled = null;
        try {
            chain.doFilter(cachingRequest, cachingResponse);
        } catch (IOException | ServletException | RuntimeException ex) {
            unhandled = ex;
            throw ex;
        } finally {
            log(new MessageIn(cachingRequest.getContentAsString(), request.getMethod(), getUrl(request), requestTime.toString()));

            Throwable exception = unhandled != null ? unhandled : getException(request);
            String fault = exception == null ? null : getStackTrace(exception);
            log(new MessageOut(getBody(cachingResponse), OffsetDateTime.now(ZoneOffset.UTC).toString(), fault));

            cachingResponse.copyBodyToResponse();
        }
    }

    private static String getBody(ContentCachingResponseWrapper response) {
        return new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private static Throwable getException(HttpServletRequest request) {
        return request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE) instanceof Throwable ex ? ex : null;
    }

    private static String getUrl(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURL().toString() : request.getRequestURL() + "?" + query;
    }

    private static String getStackTrace(Throwable ex) {
        StringWriter out = new StringWriter();
        ex.printStackTrace(new PrintWriter(out));
        return out.toString();
    }

    private void log(Object entry) {
        log.info(jsonMapper.writeValueAsString(entry));
    }

}
