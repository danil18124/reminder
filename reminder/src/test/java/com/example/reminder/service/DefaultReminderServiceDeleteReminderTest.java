/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder.service;

import ch.qos.logback.classic.Level;
import com.example.reminder.exception.ReminderNotFoundException;
import com.example.reminder.model.OAuthProvider;
import com.example.reminder.quartz.scheduler.EmailScheduler;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.util.TestLogUtils;
import com.example.reminder.util.TestReminderFactory;
import com.example.reminder.util.TestUserFactory;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 *
 * @author danil
 */
@ExtendWith(MockitoExtension.class)
public class DefaultReminderServiceDeleteReminderTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailScheduler emailScheduler;

    @InjectMocks
    private DefaultReminderService reminderService;

    @Test
    void shouldDeleteReminder_whenReminderExistsForCurrentUser() {
        // given
        var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
        var reminder = TestReminderFactory.createReminder(100L, "Title", "Desc", Instant.now(), user);

        when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(user));
        when(reminderRepository.findByIdAndUser(100L, user))
                .thenReturn(Optional.of(reminder));

        var logAppender = TestLogUtils.attachAppender(DefaultReminderService.class);

        // when
        reminderService.deleteReminder(100L, OAuthProvider.GOOGLE, "google-123", "test@example.com");

        // then
        verify(reminderRepository).delete(reminder);
        TestLogUtils.assertLogged(logAppender, Level.INFO, "Deleted reminder", "user 42", "provider=GOOGLE");
    }

    @Test
    void shouldThrowReminderNotFoundException_whenReminderDoesNotExistForCurrentUser() {
        // given
        var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
        when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(user));
        when(reminderRepository.findByIdAndUser(999L, user))
                .thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> reminderService.deleteReminder(999L, OAuthProvider.GOOGLE, "google-123", "test@example.com"))
                .isInstanceOf(ReminderNotFoundException.class);

        verify(reminderRepository, never()).delete(any());
    }

}
