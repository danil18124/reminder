/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder_client.controller.payload;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author danil
 */
public record UpdateReminderForm (String title,
    String description,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime remind) {
}
