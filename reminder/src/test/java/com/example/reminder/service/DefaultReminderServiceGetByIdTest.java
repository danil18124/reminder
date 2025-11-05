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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/**
 *
 * @author danil
 */
@ExtendWith(MockitoExtension.class)
public class DefaultReminderServiceGetByIdTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DefaultReminderService reminderService;

    // getById
    
    @Test
    void shouldReturnReminderById_whenReminderExistsForUser() {
        // given
        var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
        var reminder = TestReminderFactory.createReminder(100L, "Title", "Desc", Instant.now(), user);

        when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(user));
        when(reminderRepository.findByIdAndUser(100L, user))
                .thenReturn(Optional.of(reminder));

        var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(DefaultReminderService.class);
        logger.setLevel(Level.DEBUG);

        var logAppender = TestLogUtils.attachAppender(DefaultReminderService.class);

        // when
        var dto = reminderService.getById(100L, OAuthProvider.GOOGLE, "google-123", "test@example.com");

        // then
        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.title()).isEqualTo("Title");
        assertThat(dto.userId()).isEqualTo(42L);

        verify(reminderRepository).findByIdAndUser(100L, user);
        TestLogUtils.assertLogged(logAppender, Level.DEBUG, "User 42", "fetched reminder 100");
    }

    @Test
    void shouldThrowReminderNotFoundException_whenReminderNotExistsForUser() {
        // given
        var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");

        when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(user));
        when(reminderRepository.findByIdAndUser(404L, user)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> reminderService.getById(404L, OAuthProvider.GOOGLE, "google-123", "test@example.com"))
                .isInstanceOf(ReminderNotFoundException.class);

        verify(reminderRepository).findByIdAndUser(404L, user);
    }
}
