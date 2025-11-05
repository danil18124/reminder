package com.example.reminder.controller.payload;

import java.time.Instant;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record UpdateReminderPayload(

		@Nullable
	    @Size(min = 3, max = 255, message = "{reminders.update.errors.title_size_is_invalid}")
	    String title,

	    @Nullable
	    @Size(min=3, max = 4096, message = "{reminders.update.errors.description_size_is_invalid}")
	    String description,

	    @Nullable
	    @Future(message = "{reminders.update.errors.instant_is_invalid}")
	    OffsetDateTime remind

	) {}


