/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder.service;

import ch.qos.logback.classic.Level;
import com.example.reminder.exception.InvalidDateRangeException;
import com.example.reminder.exception.InvalidPageRequestException;
import com.example.reminder.exception.ReminderNotFoundException;
import com.example.reminder.model.OAuthProvider;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.util.TestLogUtils;
import com.example.reminder.util.TestReminderFactory;
import com.example.reminder.util.TestUserFactory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 *
 * @author danil
 */
@ExtendWith(MockitoExtension.class)
public class DefaultReminderServiceFindRemindersTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DefaultReminderService reminderService;

    // findAllSortedByTitle
    @Test
    void shouldReturnPagedRemindersSortedByTitle_whenValidPageRequest() {
        // given
        var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
        var reminder1 = TestReminderFactory.createReminder(1L, "A", "desc", Instant.now(), user);
        var reminder2 = TestReminderFactory.createReminder(2L, "B", "desc", Instant.now(), user);

        when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(user));
        when(reminderRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reminder1, reminder2)));

        var logAppender = TestLogUtils.attachAppender(DefaultReminderService.class);

        // when
        var result = reminderService.findAllSortedByTitle(PageRequest.of(0, 10), OAuthProvider.GOOGLE, "google-123", "test@example.com");

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).title()).isEqualTo("A");

        verify(reminderRepository).findByUser(eq(user), any(Pageable.class));

        TestLogUtils.assertLogged(logAppender, Level.INFO, "User 42 requested reminders page 0 size 10");
    }

    @Test
    void shouldThrowInvalidPageRequestException_whenPageableIsInvalid() {
        // given
        var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
        when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(user));

        // создаём фейковый Pageable с некорректным номером страницы
        Pageable invalidPageable = new Pageable() {
            @Override
            public int getPageNumber() {
                return -1;
            }

            @Override
            public int getPageSize() {
                return 10;
            }

            @Override
            public long getOffset() {
                return 0;
            }

            @Override
            public Sort getSort() {
                return Sort.unsorted();
            }

            @Override
            public Pageable next() {
                return this;
            }

            @Override
            public Pageable previousOrFirst() {
                return this;
            }

            @Override
            public Pageable first() {
                return this;
            }

            @Override
            public boolean hasPrevious() {
                return false;
            }

            @Override
            public Pageable withPage(int pageNumber) {
                return this;
            } // ✅ новый метод
        };

        // when + then
        assertThatThrownBy(() -> reminderService.findAllSortedByTitle(invalidPageable,
                OAuthProvider.GOOGLE, "google-123", "test@example.com"))
                .isInstanceOf(InvalidPageRequestException.class);

        verify(reminderRepository, never()).findByUser(any(), any());
    }

    // --------------------------------
// findAllSortedByDate
    @Test
    void shouldReturnPagedRemindersSortedByDate_whenValidPageRequest() {
        // given
        var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
        var reminder1 = TestReminderFactory.createReminder(1L, "A", "desc", Instant.now().plusSeconds(1000), user);
        var reminder2 = TestReminderFactory.createReminder(2L, "B", "desc", Instant.now().plusSeconds(2000), user);

        when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(user));
        when(reminderRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reminder1, reminder2)));

        var logAppender = TestLogUtils.attachAppender(DefaultReminderService.class);

        // when
        var result = reminderService.findAllSortedByDate(PageRequest.of(0, 10), OAuthProvider.GOOGLE, "google-123", "test@example.com");

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).title()).isEqualTo("A");

        verify(reminderRepository).findByUser(eq(user), any(Pageable.class));
        TestLogUtils.assertLogged(logAppender, Level.INFO, "User 42 requested reminders page 0 size 10");
    }

    @Test
    void shouldThrowInvalidPageRequestException_whenPageableIsInvalidForFindAllSortedByDate() {
        // given
        var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
        when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(user));

        // кастомный Pageable с некорректным номером страницы
        Pageable invalidPageable = new Pageable() {
            @Override
            public int getPageNumber() {
                return -1;
            }

            @Override
            public int getPageSize() {
                return 10;
            }

            @Override
            public long getOffset() {
                return 0;
            }

            @Override
            public Sort getSort() {
                return Sort.unsorted();
            }

            @Override
            public Pageable next() {
                return this;
            }

            @Override
            public Pageable previousOrFirst() {
                return this;
            }

            @Override
            public Pageable first() {
                return this;
            }

            @Override
            public boolean hasPrevious() {
                return false;
            }

            @Override
            public Pageable withPage(int pageNumber) {
                return this;
            } // обязательно для новых версий Spring
        };

        // when + then
        assertThatThrownBy(() -> reminderService.findAllSortedByDate(
                invalidPageable,
                OAuthProvider.GOOGLE,
                "google-123",
                "test@example.com"))
                .isInstanceOf(InvalidPageRequestException.class);

        verify(reminderRepository, never()).findByUser(any(), any());
    }

    // -----------------------------------------
    // findRemindersByTitle
    @Test
    void shouldReturnPagedRemindersByTitle_whenValidPageRequest() {
        // given
        var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
        var reminder1 = TestReminderFactory.createReminder(1L, "Meeting", "desc", Instant.now(), user);
        var reminder2 = TestReminderFactory.createReminder(2L, "Meeting with boss", "desc", Instant.now(), user);

        when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(user));
        when(reminderRepository.findByUserAndTitleContainingIgnoreCase(eq(user), eq("Meeting"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reminder1, reminder2)));

        var logAppender = TestLogUtils.attachAppender(DefaultReminderService.class);

        // when
        var result = reminderService.findRemindersByTitle("Meeting", PageRequest.of(0, 5), OAuthProvider.GOOGLE, "google-123", "test@example.com");

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).title()).contains("Meeting");

        verify(reminderRepository).findByUserAndTitleContainingIgnoreCase(eq(user), eq("Meeting"), any(Pageable.class));
        TestLogUtils.assertLogged(logAppender, Level.INFO, "User 42 searched reminders with title 'Meeting'");
    }

    @Test
    void shouldThrowInvalidPageRequestException_whenPageableIsInvalidForFindRemindersByTitle() {
        // given
        var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
        when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(user));

        Pageable invalidPageable = new Pageable() {
            @Override
            public int getPageNumber() {
                return -1;
            }

            @Override
            public int getPageSize() {
                return 10;
            }

            @Override
            public long getOffset() {
                return 0;
            }

            @Override
            public Sort getSort() {
                return Sort.unsorted();
            }

            @Override
            public Pageable next() {
                return this;
            }

            @Override
            public Pageable previousOrFirst() {
                return this;
            }

            @Override
            public Pageable first() {
                return this;
            }

            @Override
            public boolean hasPrevious() {
                return false;
            }

            @Override
            public Pageable withPage(int pageNumber) {
                return this;
            } // обязательно для новых версий Spring
        };

        // when + then
        assertThatThrownBy(() -> reminderService.findRemindersByTitle("Test", invalidPageable, OAuthProvider.GOOGLE, "google-123", "test@example.com"))
                .isInstanceOf(InvalidPageRequestException.class);

        verify(reminderRepository, never()).findByUserAndTitleContainingIgnoreCase(any(), any(), any());
    }

    // -----------------------------------------
    // findRemindersByDateRange 
    @Test
    void shouldReturnRemindersByDateRange_whenValidRangeAndPageRequest() {
        // given
        var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
        var start = Instant.now();
        var end = start.plusSeconds(3600);

        var reminder1 = TestReminderFactory.createReminder(1L, "Title1", "desc", start.plusSeconds(600), user);
        var reminder2 = TestReminderFactory.createReminder(2L, "Title2", "desc", start.plusSeconds(1200), user);

        when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(user));
        when(reminderRepository.findByUserAndRemindBetween(eq(user), eq(start), eq(end), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reminder1, reminder2)));

        var logAppender = TestLogUtils.attachAppender(DefaultReminderService.class);

        // when
        var result = reminderService.findRemindersByDateRange(start, end, PageRequest.of(0, 5),
                OAuthProvider.GOOGLE, "google-123", "test@example.com");

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).title()).isEqualTo("Title1");

        verify(reminderRepository).findByUserAndRemindBetween(eq(user), eq(start), eq(end), any(Pageable.class));
        TestLogUtils.assertLogged(logAppender, Level.INFO, "User 42 requested reminders from", start.toString(), end.toString());
    }

    @Test
    void shouldThrowInvalidDateRangeException_whenStartAfterEnd() {
        // given
        var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
        var start = Instant.now().plusSeconds(3600);
        var end = Instant.now();

        // when + then
        assertThatThrownBy(() -> reminderService.findRemindersByDateRange(start, end, PageRequest.of(0, 5),
                OAuthProvider.GOOGLE, "google-123", "test@example.com"))
                .isInstanceOf(InvalidDateRangeException.class);

        verify(reminderRepository, never()).findByUserAndRemindBetween(any(), any(), any(), any());
    }

    @Test
    void shouldThrowInvalidPageRequestException_whenPageableIsInvalidForDateRange() {
        // given
        var user = TestUserFactory.createUser(42L, "test@example.com", OAuthProvider.GOOGLE, "google-123");
        when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(user));

        var start = Instant.now();
        var end = start.plusSeconds(3600);

        Pageable invalidPageable = new Pageable() {
            @Override
            public int getPageNumber() {
                return -1;
            }

            @Override
            public int getPageSize() {
                return 5;
            }

            @Override
            public long getOffset() {
                return 0;
            }

            @Override
            public Sort getSort() {
                return Sort.unsorted();
            }

            @Override
            public Pageable next() {
                return this;
            }

            @Override
            public Pageable previousOrFirst() {
                return this;
            }

            @Override
            public Pageable first() {
                return this;
            }

            @Override
            public boolean hasPrevious() {
                return false;
            }

            @Override
            public Pageable withPage(int pageNumber) {
                return this;
            } // обязательно для новых версий Spring
        };

        // when + then
        assertThatThrownBy(() -> reminderService.findRemindersByDateRange(start, end, invalidPageable,
                OAuthProvider.GOOGLE, "google-123", "test@example.com"))
                .isInstanceOf(InvalidPageRequestException.class);

        verify(reminderRepository, never()).findByUserAndRemindBetween(any(), any(), any(), any());
    }

}
