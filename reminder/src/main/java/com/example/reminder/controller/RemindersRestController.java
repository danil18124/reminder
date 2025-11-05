package com.example.reminder.controller;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.reminder.controller.payload.NewReminderPayload;
import com.example.reminder.model.OAuthProvider;
import com.example.reminder.model.dto.ReminderDto;
import com.example.reminder.service.ReminderService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/reminder")
public class RemindersRestController {

	private final ReminderService reminderService;

//{
//  "title": "Buy protein",
//  "description": "Take after gym session",
//  "remind": "2025-09-10T18:00:00Z"
//}

        // /api/v1/reminder
	@PostMapping
	public ResponseEntity<ReminderDto> createReminder(
	        @Valid @RequestBody NewReminderPayload payload,
	        @AuthenticationPrincipal Jwt jwt) {

	    String providerId = jwt.getSubject();
	    String email      = jwt.getClaimAsString("email"); 

	    ReminderDto dto = reminderService.createReminder(payload,
	            OAuthProvider.GOOGLE, providerId, email);

	    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
	            .path("/{id}")
	            .buildAndExpand(dto.id())
	            .toUri();

	    return ResponseEntity.created(location).body(dto);
	}




// GET /api/v1/reminder/sort/title?page=0&size=3&sort=title,desc
	@GetMapping("/sort/title")
	public ResponseEntity<Page<ReminderDto>> findAllSortedByTitle(Pageable pageable, @AuthenticationPrincipal Jwt jwt) {
		String providerId = jwt.getSubject();
	    String email      = jwt.getClaimAsString("email"); 
		Page<ReminderDto> page = reminderService.findAllSortedByTitle(pageable, OAuthProvider.GOOGLE, providerId, email);
		return ResponseEntity.ok(page);
	}

// GET /api/v1/reminder/sort/date?page=0&size=3&sort=remind,desc
	@GetMapping("/sort/date")
	public ResponseEntity<Page<ReminderDto>> findAllSortedByDate(Pageable pageable, @AuthenticationPrincipal Jwt jwt) {
		String providerId = jwt.getSubject();
	    String email      = jwt.getClaimAsString("email"); 
		Page<ReminderDto> page = reminderService.findAllSortedByDate(pageable, OAuthProvider.GOOGLE, providerId, email);
		return ResponseEntity.ok(page);
	}

// GET /api/v1/reminder?title=test&page=0&size=2&sort=remind,desc
	@GetMapping
	public ResponseEntity<Page<ReminderDto>> findRemindersByTitle(@RequestParam("title") @NotBlank(message = "Title must not be empty") String title,
			Pageable pageable, @AuthenticationPrincipal Jwt jwt) {
		String providerId = jwt.getSubject();
	    String email      = jwt.getClaimAsString("email"); 
		Page<ReminderDto> page = reminderService.findRemindersByTitle(title, pageable, OAuthProvider.GOOGLE, providerId, email);
		return ResponseEntity.ok(page);
	}

// GET /api/v1/reminder/search-by-date?date=2025-08-28&page=0&size=3&sort=remind,asc
	@GetMapping("/search-by-date")
	public ResponseEntity<Page<ReminderDto>> findRemindersByDate(
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Pageable pageable, @AuthenticationPrincipal Jwt jwt) {

		Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

		String providerId = jwt.getSubject();
                String email      = jwt.getClaimAsString("email"); 
		
		Page<ReminderDto> page = reminderService.findRemindersByDateRange(startOfDay, endOfDay, pageable, OAuthProvider.GOOGLE, providerId, email);

		return ResponseEntity.ok(page);
	}

// GET /api/v1/reminder/filter/date?start=2025-08-10T00:00:00Z&end=2025-08-30T23:59:59Z&page=0&size=5
	@GetMapping("/filter/date")
	public ResponseEntity<Page<ReminderDto>> findRemindersByDateRange(
			@RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
			@RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end, Pageable pageable, @AuthenticationPrincipal Jwt jwt) {

		String providerId = jwt.getSubject();
	    String email      = jwt.getClaimAsString("email"); 
		
		Page<ReminderDto> page = reminderService.findRemindersByDateRange(start, end, pageable, OAuthProvider.GOOGLE, providerId, email);
		return ResponseEntity.ok(page);
	}
        
        

}
