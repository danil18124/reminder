package com.example.reminder.controller.payload;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record NewReminderPayload(

	    @NotBlank(message = "{reminders.create.errors.title_is_blank}")
	    @Size(min = 3, max = 255, message = "{reminders.create.errors.title_size_is_invalid}")
	    String title,

	    @NotBlank(message = "{reminders.create.errors.description_is_blank}")
	    @Size(min = 3, max = 4096, message = "{reminders.create.errors.description_size_is_invalid}")
	    String description,

	    @NotNull(message = "{reminders.create.errors.instant_is_null}")
	    @Future(message = "{reminders.create.errors.instant_is_invalid}")
	    OffsetDateTime remind

	) {}

