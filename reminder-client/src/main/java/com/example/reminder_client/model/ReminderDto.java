package com.example.reminder_client.model;

import java.time.Instant;
import java.time.OffsetDateTime;

public record ReminderDto(
        Long id,
        String title,
        String description,
        OffsetDateTime remind,
        Long userId
) {}
