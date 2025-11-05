/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder_client.service;

import com.example.reminder_client.service.dto.ApiErrorResponse;
import com.example.reminder_client.service.dto.Result;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 *
 * @author danil
 */

@Component
public class SafeRestClient {

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public SafeRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public <T> Result<T> post(String uri, Object body, Class<T> clazz) {
        try {
            T response = restClient.post()
                    .uri(uri)
                    .body(body)
                    .retrieve()
                    .body(clazz);
            return Result.success(response);
        } catch (RestClientResponseException ex) {
            return parseError(ex);
        }
    }

    public <T> Result<T> patch(String uri, Object body, Class<T> clazz, Object... uriVars) {
        try {
            T response = restClient.patch()
                    .uri(uri, uriVars)
                    .body(body)
                    .retrieve()
                    .body(clazz);
            return Result.success(response);
        } catch (RestClientResponseException ex) {
            return parseError(ex);
        }
    }

    private <T> Result<T> parseError(RestClientResponseException ex) {
        try {
            ApiErrorResponse<?> error = mapper.readValue(
                    ex.getResponseBodyAsString(),
                    new TypeReference<ApiErrorResponse<?>>() {}
            );
            return Result.failure(error);
        } catch (Exception parseEx) {
            throw new RuntimeException("Failed to parse error response", parseEx);
        }
    }
}

