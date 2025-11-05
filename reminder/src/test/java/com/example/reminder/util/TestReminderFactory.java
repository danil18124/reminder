/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder.util;

/**
 *
 * @author danil
 */
import com.example.reminder.model.Reminder;
import com.example.reminder.model.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

public class TestReminderFactory {

    public static Reminder createReminder(Long id, String title, String description, Instant remind, User user) {
        Reminder reminder = Reminder.builder()
                .title(title)
                .description(description)
                .remind(remind)
                .user(user)
                .build();
        ReflectionTestUtils.setField(reminder, "id", id);
        return reminder;
    }
}
