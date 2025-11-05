package com.example.reminder_client.service;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.reminder_client.model.ReminderDto;
import com.example.reminder_client.service.dto.PagedResponse;
import com.example.reminder_client.service.dto.Result;
import com.example.reminder_client.service.payload.NewReminderPayload;
import com.example.reminder_client.service.payload.UpdateReminderPayload;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestClientReminderRestClient implements ReminderRestClient {

    private final RestClient restClient;
    private final SafeRestClient safeRestClient;
    
    @Override
public Result<ReminderDto> createReminder(NewReminderPayload payload) {
    return safeRestClient.post("/api/v1/reminder", payload, ReminderDto.class);
}

@Override
public Result<ReminderDto> updateReminder(Long reminderId, UpdateReminderPayload payload) {
    return safeRestClient.patch("/api/v1/reminder/{id}", payload, ReminderDto.class, reminderId);
}


    @Override
    public ReminderDto getReminderById(Long id) {
        return restClient
                .get()
                .uri("/api/v1/reminder/{id}", id)
                .retrieve()
                .body(ReminderDto.class);
    }

    @Override
    public void deleteReminder(Long reminderId) {
        restClient
                .delete()
                .uri("/api/v1/reminder/{id}", reminderId)
                .retrieve().toBodilessEntity(); // возвращает пустой ResponseEntity
    }

    @Override
    public PagedResponse<ReminderDto> findAllSortedByTitle(int page, int size) {
        return restClient
                .get()
                .uri("/api/v1/reminder/sort/title?page={page}&size={size}&sort=title,asc", page, size)
                .retrieve()
                .body(new ParameterizedTypeReference<PagedResponse<ReminderDto>>() {
                });
    }

    @Override
    public PagedResponse<ReminderDto> findAllSortedByDate(int page, int size) {
        return restClient
                .get()
                .uri("/api/v1/reminder/sort/date?page={page}&size={size}&sort=title,asc", page, size)
                .retrieve()
                .body(new ParameterizedTypeReference<PagedResponse<ReminderDto>>() {
                });
    }

    @Override
    public PagedResponse<ReminderDto> findRemindersByTitle(String title, int page, int size, String sort) {
        return restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                .path("/api/v1/reminder")
                .queryParam("title", title)
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sort", sort) // например "remind,desc"
                .build()
                )
                .retrieve()
                .body(new ParameterizedTypeReference<PagedResponse<ReminderDto>>() {
                });
    }

    @Override
    public PagedResponse<ReminderDto> findRemindersByDate(LocalDate date, int page, int size, String sort) {
        return restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                .path("/api/v1/reminder/search-by-date")
                .queryParam("date", date) // LocalDate → "2025-09-20"
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sort", sort)
                .build()
                )
                .retrieve()
                .body(new ParameterizedTypeReference<PagedResponse<ReminderDto>>() {
                });
    }

    @Override
    public PagedResponse<ReminderDto> findRemindersByDateRange(Instant start, Instant end, int page, int size, String sort) {
        return restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                .path("/api/v1/reminder/filter/date")
                .queryParam("start", start.toString()) // ISO-8601 (например 2025-08-10T00:00:00Z)
                .queryParam("end", end.toString())
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sort", sort) // например "remind,asc"
                .build()
                )
                .retrieve()
                .body(new ParameterizedTypeReference<PagedResponse<ReminderDto>>() {
                });
    }

}
