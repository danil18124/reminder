package com.example.reminder_client.service.payload;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record NewReminderPayload(
	    String title,
	    String description,
	    Instant remind
		) {

}
