/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder_client.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

import java.time.OffsetDateTime;
import java.util.*;

/**
 *
 * @author danil
 */
@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    // =========================
    // 4xx/5xx от REST-сервиса
    // =========================
    @ExceptionHandler({RestClientResponseException.class, HttpStatusCodeException.class})
    public ModelAndView handleRestClientResponseException(RestClientResponseException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getRawStatusCode());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ParsedApiError api = parseApiErrorSafely(ex.getResponseBodyAsString());

        log.warn("REST error {} {} -> UI '{}': {}", ex.getRawStatusCode(), ex.getStatusText(),
                request.getRequestURI(), api.message());

        ModelAndView mav = baseMav("errors/4xx5xx", status, request);
        mav.addObject("errorMessage", api.message());
        mav.addObject("errors", normalizeDetails(api.details())); // Map<String, List<String>>
        return mav;
    }

    // =========================
    // Любой другой сбой клиента
    // =========================
    @ExceptionHandler(RestClientException.class)
    public ModelAndView handleGenericRestClientException(RestClientException ex,
            HttpServletRequest request) {
        log.error("REST client failure on '{}'", request.getRequestURI(), ex);
        ModelAndView mav = baseMav("errors/5xx", HttpStatus.BAD_GATEWAY, request);
        mav.addObject("errorMessage", "Сервис временно недоступен. Повторите попытку позже.");
        mav.addObject("errors", Map.of());
        return mav;
    }

    // =========================
    // 404 для страниц (UI)
    // =========================
    @ExceptionHandler(NoHandlerFoundException.class)
    public ModelAndView handleNoHandler(NoHandlerFoundException ex,
            HttpServletRequest request) {
        log.info("UI 404: {}", request.getRequestURI());
        ModelAndView mav = baseMav("errors/404", HttpStatus.NOT_FOUND, request);
        mav.addObject("errorMessage", "Страница не найдена");
        mav.addObject("errors", Map.of());
        return mav;
    }

    // =========================
    // 405 для страниц (UI)
    // =========================
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ModelAndView handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        log.info("UI 405 on '{}': {}", request.getRequestURI(), ex.getMethod());
        ModelAndView mav = baseMav("errors/405", HttpStatus.METHOD_NOT_ALLOWED, request);
        mav.addObject("errorMessage", "Метод не поддерживается");
        mav.addObject("errors", Map.of("method", List.of(ex.getMethod())));
        return mav;
    }

    @ExceptionHandler(RuntimeException.class)
    public ModelAndView handleRuntime(RuntimeException ex, HttpServletRequest request) {
        // достанем/создадим correlation id (см. фильтр ниже)
        String cid = (String) Optional.ofNullable(request.getAttribute(Correlation.CID_REQ_ATTR))
                .orElseGet(() -> Correlation.ensureCid(request));

        log.error("UI 500 on '{}' [cid={}]", request.getRequestURI(), cid, ex);

        ModelAndView mav = baseMav("errors/5xx", HttpStatus.INTERNAL_SERVER_ERROR, request);
        mav.addObject("errorMessage", "Внутренняя ошибка. Мы уже разбираемся.");
        mav.addObject("errors", Map.of());
        mav.addObject("correlationId", cid);
        return mav;
    }

    // ===== Helpers =====
    private ModelAndView baseMav(String view, HttpStatus status, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView(view);
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("path", request.getRequestURI());
        mav.addObject("timestamp", OffsetDateTime.now());
        return mav;
    }

    private record ParsedApiError(String message, Map<String, ?> details) {

    }

    /**
     * Пытаемся распарсить ваш ApiErrorResponse; если класс недоступен — читаем
     * generic JSON.
     */
    private ParsedApiError parseApiErrorSafely(String body) {
        if (body == null || body.isBlank()) {
            return new ParsedApiError("Неизвестная ошибка", Map.of());
        }
        try {
            // Популярный формат: {"message": "...", "details": {...}}
            Map<String, Object> root = mapper.readValue(body, new TypeReference<>() {
            });
            String message = Optional.ofNullable(root.get("message"))
                    .map(Object::toString)
                    .orElse("Ошибка запроса");
            @SuppressWarnings("unchecked")
            Map<String, ?> details = Optional.ofNullable(root.get("details"))
                    .filter(Map.class::isInstance)
                    .map(m -> (Map<String, ?>) m)
                    .orElse(Map.of());
            return new ParsedApiError(message, details);
        } catch (Exception e) {
            log.debug("Cannot parse ApiErrorResponse: raw='{}'", body, e);
            return new ParsedApiError("Ошибка запроса", Map.of("raw", List.of(truncate(body, 500))));
        }
    }

    /**
     * Приводим значения details к Map<String, List<String>>
     */
    private Map<String, List<String>> normalizeDetails(Map<String, ?> raw) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        raw.forEach((k, v) -> {
            if (v == null) {
                out.put(k, List.of("null"));
            } else if (v instanceof Collection<?> c) {
                out.put(k, c.stream().map(String::valueOf).toList());
            } else {
                out.put(k, List.of(String.valueOf(v)));
            }
        });
        return out;
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
