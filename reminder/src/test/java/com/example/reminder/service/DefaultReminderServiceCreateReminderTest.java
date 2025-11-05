/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder.service;

import ch.qos.logback.classic.Level;
import com.example.reminder.controller.payload.NewReminderPayload;
import com.example.reminder.model.OAuthProvider;
import com.example.reminder.model.Reminder;
import com.example.reminder.model.User;
import com.example.reminder.quartz.scheduler.EmailScheduler;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.util.TestLogUtils;
import com.example.reminder.util.TestReminderFactory;
import com.example.reminder.util.TestUserFactory;
import java.time.OffsetDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 *
 * @author danil
 */
@ExtendWith(MockitoExtension.class)
public class DefaultReminderServiceCreateReminderTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailScheduler emailScheduler;

    @InjectMocks
    private DefaultReminderService reminderService;

    private void mockFindUser(OAuthProvider provider, String providerId, User user) {
        when(userRepository.findByProviderAndProviderId(provider, providerId))
                .thenReturn(Optional.ofNullable(user));
    }

    private void mockSaveReminder(Reminder expectedReminder) {
        when(reminderRepository.save(any(Reminder.class))).thenReturn(expectedReminder);
    }

    private void mockSaveReminderAssigningId(long id) {
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> {
            Reminder r = invocation.getArgument(0);
            return TestReminderFactory.createReminder(id, r.getTitle(), r.getDescription(), r.getRemind(), r.getUser());
        });
    }

    @Test
    void shouldSaveReminderAndReturnDto_whenUserExists() {
        // given
        var payload = new NewReminderPayload("Title", "Description", OffsetDateTime.now().plusHours(1));
        var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
        var reminder = TestReminderFactory.createReminder(100L, "Title", "Description", payload.remind().toInstant(), user);

        mockFindUser(OAuthProvider.GOOGLE, "google-123", user);
        mockSaveReminder(reminder);

        var logAppender = TestLogUtils.attachAppender(DefaultReminderService.class);

        // when
        var dto = reminderService.createReminder(payload, OAuthProvider.GOOGLE, "google-123", "test@example.com");

        // then
        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.userId()).isEqualTo(42L);

        verify(emailScheduler).schedule(any(Reminder.class));
        verify(reminderRepository).save(any(Reminder.class));
        verifyNoMoreInteractions(emailScheduler, reminderRepository, userRepository);

        TestLogUtils.assertLogged(logAppender, Level.INFO, "Created reminder", "user 42");
    }

    @Test
    void shouldCreateNewUser_whenUserNotExists() {
        // given
        var payload = new NewReminderPayload("Title", "Description", OffsetDateTime.now().plusHours(2));
        when(userRepository.findByProviderAndProviderId(any(), any())).thenReturn(Optional.empty());

        var savedUser = TestUserFactory.createUser(99L, "new@example.com", OAuthProvider.GOOGLE, "google-999");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);

        mockSaveReminderAssigningId(200L);

        var logAppender = TestLogUtils.attachAppender(DefaultReminderService.class);

        // when
        var dto = reminderService.createReminder(payload, OAuthProvider.GOOGLE, "google-999", "new@example.com");

        // then
        assertThat(dto.userId()).isEqualTo(99L);
        assertThat(dto.id()).isEqualTo(200L);

        verify(userRepository).saveAndFlush(any(User.class));
        verify(reminderRepository).save(any(Reminder.class));
        verify(emailScheduler).schedule(any(Reminder.class));

        TestLogUtils.assertLogged(logAppender, Level.INFO, "Created reminder", "user 99");

    }

    @Test
    void shouldHandleRaceConditionGracefully_whenConcurrentUserCreation() {
        // given
        var payload = new NewReminderPayload("Title", "Desc", OffsetDateTime.now().plusMinutes(30));
        var persistedUser = TestUserFactory.createUser(123L, "race@example.com", OAuthProvider.GOOGLE, "race-123");

        when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "race-123"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(persistedUser));
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        mockSaveReminderAssigningId(300L);

        var logAppender = TestLogUtils.attachAppender(DefaultReminderService.class);

        // when
        var dto = reminderService.createReminder(payload, OAuthProvider.GOOGLE, "race-123", "race@example.com");

        // then
        assertThat(dto.userId()).isEqualTo(123L);
        assertThat(dto.id()).isEqualTo(300L);

        verify(userRepository, times(2)).findByProviderAndProviderId(any(), any());
        verify(userRepository).saveAndFlush(any(User.class));
        verify(reminderRepository).save(any(Reminder.class));
        verify(emailScheduler).schedule(any(Reminder.class));

        TestLogUtils.assertLogged(logAppender, Level.WARN, "Race condition", "race-123");
    }

}
