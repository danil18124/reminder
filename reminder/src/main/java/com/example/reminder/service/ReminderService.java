package com.example.reminder.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.reminder.controller.payload.NewReminderPayload;
import com.example.reminder.controller.payload.UpdateReminderPayload;
import com.example.reminder.model.OAuthProvider;
import com.example.reminder.model.dto.ReminderDto;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;

public interface ReminderService {

	public ReminderDto createReminder(NewReminderPayload payload,
            OAuthProvider provider,
            String providerId,
            String email);

	public void deleteReminder(Long reminderId,
            OAuthProvider provider,
            String providerId, 
            String email);

	ReminderDto updateReminder(Long reminderId,
            UpdateReminderPayload payload,
            OAuthProvider provider,
            String providerId,
            String email);
	
	Page<ReminderDto> findAllSortedByTitle(Pageable pageable,
            OAuthProvider provider,
            String providerId,
            String email);
	
	Page<ReminderDto> findAllSortedByDate(Pageable pageable,
            OAuthProvider provider,
            String providerId,
            String email);
	
	Page<ReminderDto> findRemindersByTitle(String title,
            Pageable pageable,
            OAuthProvider provider,
            String providerId,
            String email);

	Page<ReminderDto> findRemindersByDateRange(Instant start,
            Instant end,
            Pageable pageable,
            OAuthProvider provider,
            String providerId,
            String email);

	ReminderDto getById(Long id, OAuthProvider provider,
            String providerId,
            String email);

}
