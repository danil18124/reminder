package com.example.reminder_client.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.reminder_client.model.ReminderDto;
import com.example.reminder_client.service.dto.PagedResponse;
import com.example.reminder_client.service.dto.Result;
import com.example.reminder_client.service.payload.NewReminderPayload;
import com.example.reminder_client.service.payload.UpdateReminderPayload;


public interface ReminderRestClient {
	Result<ReminderDto> createReminder(NewReminderPayload payload);
	
	void deleteReminder(Long reminderId);
	
	Result<ReminderDto> updateReminder(Long reminderId, UpdateReminderPayload payload);
	
	PagedResponse<ReminderDto> findAllSortedByTitle(int page, int size);
	
	ReminderDto getReminderById(Long id);
	
	PagedResponse<ReminderDto> findAllSortedByDate(int page, int size);
	
	PagedResponse<ReminderDto> findRemindersByTitle(String title, int page, int size, String sort);
	
	PagedResponse<ReminderDto> findRemindersByDate(LocalDate date, int page, int size, String sort);
	
	PagedResponse<ReminderDto> findRemindersByDateRange(Instant start, Instant end, int page, int size, String sort);
}
