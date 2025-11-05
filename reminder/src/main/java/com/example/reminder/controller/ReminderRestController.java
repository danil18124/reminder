package com.example.reminder.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;


import com.example.reminder.controller.payload.UpdateReminderPayload;
import com.example.reminder.model.OAuthProvider;

import com.example.reminder.model.dto.ReminderDto;
import com.example.reminder.service.ReminderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/reminder")
public class ReminderRestController {

	private final ReminderService reminderService;

	// DELETE /api/v1/reminder/42
	@DeleteMapping("/{reminderId}")
	public ResponseEntity<Void> deleteReminder(@PathVariable Long reminderId, @AuthenticationPrincipal Jwt jwt) {
		
		String providerId = jwt.getSubject();
	    String email      = jwt.getClaimAsString("email"); 
		
		reminderService.deleteReminder(reminderId, OAuthProvider.GOOGLE, providerId, email);
		return ResponseEntity.noContent().build();
	}

	// PUT /api/v1/reminder/42
	@PatchMapping("/{reminderId}")
	public ResponseEntity<ReminderDto> updateReminder(@PathVariable Long reminderId,
			@Valid @RequestBody UpdateReminderPayload payload, @AuthenticationPrincipal Jwt jwt) {
		
		String providerId = jwt.getSubject();
	    String email      = jwt.getClaimAsString("email"); 

		ReminderDto updated = reminderService.updateReminder(reminderId, payload, OAuthProvider.GOOGLE, providerId, email);
		return ResponseEntity.ok(updated);
	}

	// ------------------------------------------------------------------

	// I had to add this endpoint to implement the functionality in the browser

	@GetMapping("/{reminderId}")
	public ResponseEntity<ReminderDto> getReminderById(@PathVariable("reminderId") Long id, @AuthenticationPrincipal Jwt jwt) {
		
		String providerId = jwt.getSubject();
	    String email      = jwt.getClaimAsString("email"); 
		
		ReminderDto reminder = reminderService.getById(id, OAuthProvider.GOOGLE, providerId, email);
		return ResponseEntity.ok(reminder);
	}

}
