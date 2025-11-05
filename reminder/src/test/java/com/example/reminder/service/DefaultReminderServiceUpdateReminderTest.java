/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder.service;

import ch.qos.logback.classic.Level;
import com.example.reminder.controller.payload.UpdateReminderPayload;
import com.example.reminder.exception.ReminderNotFoundException;
import com.example.reminder.model.OAuthProvider;
import com.example.reminder.model.Reminder;
import com.example.reminder.quartz.scheduler.EmailScheduler;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.util.TestLogUtils;
import com.example.reminder.util.TestReminderFactory;
import com.example.reminder.util.TestUserFactory;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
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
public class DefaultReminderServiceUpdateReminderTest {
    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailScheduler emailScheduler;

    @InjectMocks
    private DefaultReminderService reminderService;
    
    @Test
void shouldUpdateReminder_whenFieldsChanged() {
    // given
    var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
    var reminder = TestReminderFactory.createReminder(100L, "OldTitle", "OldDesc", Instant.now(), user);

    when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
            .thenReturn(Optional.of(user));
    when(reminderRepository.findByIdAndUser(100L, user))
            .thenReturn(Optional.of(reminder));
    when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var payload = new UpdateReminderPayload("NewTitle", "NewDesc", OffsetDateTime.now().plusHours(1));
    var logAppender = TestLogUtils.attachAppender(DefaultReminderService.class);

    // when
    var dto = reminderService.updateReminder(100L, payload, OAuthProvider.GOOGLE, "google-123", "test@example.com");

    // then
    assertThat(dto.title()).isEqualTo("NewTitle");
    assertThat(dto.description()).isEqualTo("NewDesc");

    verify(reminderRepository).save(reminder);
    verify(emailScheduler).reschedule(reminder);

    TestLogUtils.assertLogged(logAppender, Level.INFO, "Updated reminder", "user 42", "provider=GOOGLE");
}

@Test
void shouldRescheduleWithoutSaving_whenNoFieldsChanged() {
    // given
    var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
    var remindTime = Instant.now().plusSeconds(3600);
    var reminder = TestReminderFactory.createReminder(100L, "SameTitle", "SameDesc", remindTime, user);

    when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
            .thenReturn(Optional.of(user));
    when(reminderRepository.findByIdAndUser(100L, user))
            .thenReturn(Optional.of(reminder));

    var payload = new UpdateReminderPayload("SameTitle", "SameDesc", OffsetDateTime.ofInstant(remindTime, ZoneOffset.UTC));
    var logAppender = TestLogUtils.attachAppender(DefaultReminderService.class);

    // when
    var dto = reminderService.updateReminder(100L, payload, OAuthProvider.GOOGLE, "google-123", "test@example.com");

    // then
    assertThat(dto.title()).isEqualTo("SameTitle");
    assertThat(dto.description()).isEqualTo("SameDesc");

    verify(reminderRepository, never()).save(any());
    verify(emailScheduler).reschedule(reminder);

    TestLogUtils.assertLogged(logAppender, Level.INFO, "Updated reminder", "user 42");
}

@Test
void shouldThrowReminderNotFoundException_whenUpdatingNonexistentReminder() {
    // given
    var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
    when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
            .thenReturn(Optional.of(user));
    when(reminderRepository.findByIdAndUser(404L, user)).thenReturn(Optional.empty());

    var payload = new UpdateReminderPayload("X", "Y", OffsetDateTime.now());

    // when + then
    assertThatThrownBy(() -> reminderService.updateReminder(404L, payload, OAuthProvider.GOOGLE, "google-123", "test@example.com"))
            .isInstanceOf(ReminderNotFoundException.class);

    verify(reminderRepository, never()).save(any());
    verify(emailScheduler, never()).reschedule(any());
}

}
