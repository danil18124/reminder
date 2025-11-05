package com.example.reminder.service;

import java.time.Instant;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.reminder.controller.payload.NewReminderPayload;
import com.example.reminder.controller.payload.UpdateReminderPayload;
import com.example.reminder.exception.InvalidDateRangeException;
import com.example.reminder.exception.InvalidPageRequestException;
import com.example.reminder.exception.ReminderNotFoundException;
import com.example.reminder.model.OAuthProvider;
import com.example.reminder.model.Reminder;
import com.example.reminder.model.Role;
import com.example.reminder.model.User;
import com.example.reminder.model.dto.ReminderDto;
import com.example.reminder.quartz.scheduler.EmailScheduler;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.UserRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DefaultReminderService implements ReminderService {

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final EmailScheduler emailScheduler;

    private User getOrCreateUser(OAuthProvider provider, String providerId, String email) {

        String normEmail = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
        String normProviderId = providerId == null ? null : providerId.trim();

        return userRepository.findByProviderAndProviderId(provider, normProviderId)
                .orElseGet(() -> {
                    User u = User.builder()
                            .email(normEmail)
                            .provider(provider)
                            .providerId(normProviderId)
                            .role(Role.USER)
                            .build();
                    try {
                        return userRepository.saveAndFlush(u);
                    } catch (DataIntegrityViolationException race) {
                        log.warn("Race condition when creating user with providerId {}", normProviderId, race.getMessage());
                        return userRepository.findByProviderAndProviderId(provider, normProviderId)
                                .orElseThrow(() -> race);
                    }
                }); 
    }

    @Override
    public ReminderDto createReminder(NewReminderPayload payload,
            OAuthProvider provider,
            String providerId,
            String email) {

        User user = getOrCreateUser(provider, providerId, email);

        OffsetDateTime offsetReminder = payload.remind();
        Instant instantReminder = offsetReminder.toInstant();

        Reminder reminder = Reminder.builder()
                .title(payload.title())
                .description(payload.description())
                .remind(instantReminder)
                .user(user)
                .build();

        emailScheduler.schedule(reminder);

        Reminder saved = reminderRepository.save(reminder);

        log.info("Created reminder {} for user {} ({})", saved.getId(), user.getId(), user.getEmail());

        return mapToDto(saved);
    }

    @Override
    public void deleteReminder(Long reminderId,
            OAuthProvider provider,
            String providerId,
            String email) {

        User currentUser = getOrCreateUser(provider, providerId, email);

        // сразу ищем только своё напоминание
        Reminder reminder = reminderRepository.findByIdAndUser(reminderId, currentUser)
                .orElseThrow(() -> new ReminderNotFoundException(reminderId));

        reminderRepository.delete(reminder);
        log.info("Deleted reminder {} for user {} ({}, provider={})",
                reminderId, currentUser.getId(), currentUser.getEmail(), provider);

    }

    @Override
    public ReminderDto updateReminder(Long reminderId,
            UpdateReminderPayload payload,
            OAuthProvider provider,
            String providerId,
            String email) {

        User currentUser = getOrCreateUser(provider, providerId, email);

        Reminder reminder = reminderRepository.findByIdAndUser(reminderId, currentUser)
                .orElseThrow(() -> new ReminderNotFoundException(reminderId));

        OffsetDateTime offsetReminder = payload.remind();
        Instant instantReminder = offsetReminder.toInstant();

        // ------------------------
        // Idempotency
        boolean changed = false;

        if (payload.title() != null && !Objects.equals(reminder.getTitle(), payload.title())) {
            reminder.setTitle(payload.title());
            changed = true;
        }

        if (payload.description() != null && !Objects.equals(reminder.getDescription(), payload.description())) {
            reminder.setDescription(payload.description());
            changed = true;
        }

        if (payload.remind() != null && !Objects.equals(reminder.getRemind(), payload.remind().toInstant())) {
            reminder.setRemind(instantReminder);
            changed = true;
        }

        Reminder updated = changed ? reminderRepository.save(reminder) : reminder;

        emailScheduler.reschedule(updated);

        // ------------------------
        log.info("Updated reminder {} for user {} ({}, provider={})",
                updated.getId(), currentUser.getId(), currentUser.getEmail(), provider);

        return mapToDto(updated);
    }

    @Override
    public Page<ReminderDto> findAllSortedByTitle(Pageable pageable,
            OAuthProvider provider,
            String providerId,
            String email) {

        User currentUser = getOrCreateUser(provider, providerId, email);

        Pageable sorted;
        try {
            sorted = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("title").ascending()
            );
        } catch (IllegalArgumentException ex) {
            throw new InvalidPageRequestException(pageable.getPageNumber(), pageable.getPageSize());
        }

        log.info("User {} requested reminders page {} size {} sort {}",
                currentUser.getId(), pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return reminderRepository.findByUser(currentUser, sorted)
                .map(this::mapToDto);
    }

    @Override
    public Page<ReminderDto> findAllSortedByDate(Pageable pageable,
            OAuthProvider provider,
            String providerId,
            String email) {

        User currentUser = getOrCreateUser(provider, providerId, email);

        Pageable sorted;
        try {
            sorted = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("remind").ascending()
            );
        } catch (IllegalArgumentException ex) {
            throw new InvalidPageRequestException(pageable.getPageNumber(), pageable.getPageSize());
        }

        log.info("User {} requested reminders page {} size {} sort {}",
                currentUser.getId(), pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return reminderRepository.findByUser(currentUser, sorted)
                .map(this::mapToDto);
    }

    @Override
    public Page<ReminderDto> findRemindersByTitle(String title,
            Pageable pageable,
            OAuthProvider provider,
            String providerId,
            String email) {
        User currentUser = getOrCreateUser(provider, providerId, email);

        Pageable validated;
        try {
            validated = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    pageable.getSort()
            );
        } catch (IllegalArgumentException ex) {
            throw new InvalidPageRequestException(pageable.getPageNumber(), pageable.getPageSize());
        }

        log.info("User {} searched reminders with title '{}' page {} size {}",
                currentUser.getId(), title, validated.getPageNumber(), validated.getPageSize());

        return reminderRepository
                .findByUserAndTitleContainingIgnoreCase(currentUser, title, validated)
                .map(this::mapToDto);
    }

    @Override
    public Page<ReminderDto> findRemindersByDateRange(Instant start,
            Instant end,
            Pageable pageable,
            OAuthProvider provider,
            String providerId,
            String email) {
        if (start.isAfter(end)) {
            throw new InvalidDateRangeException(start, end);
        }

        User currentUser = getOrCreateUser(provider, providerId, email);

        Pageable validated;
        try {
            validated = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    pageable.getSort()
            );
        } catch (IllegalArgumentException ex) {
            throw new InvalidPageRequestException(pageable.getPageNumber(), pageable.getPageSize());
        }

        log.info("User {} requested reminders from {} to {} (page {}, size {})",
                currentUser.getId(), start, end, validated.getPageNumber(), validated.getPageSize());

        return reminderRepository
                .findByUserAndRemindBetween(currentUser, start, end, validated)
                .map(this::mapToDto);
    }

    private ReminderDto mapToDto(Reminder reminder) {

        Instant instantReminder = reminder.getRemind();
        OffsetDateTime offsetReminder = instantReminder.atOffset(ZoneOffset.UTC);

        return new ReminderDto(
                reminder.getId(),
                reminder.getTitle(),
                reminder.getDescription(),
                offsetReminder,
                reminder.getUser().getId()
        );
    }

    @Override
    public ReminderDto getById(Long id, OAuthProvider provider,
            String providerId,
            String email) {

        User currentUser = getOrCreateUser(provider, providerId, email);

        Reminder reminder = reminderRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ReminderNotFoundException(id));

        log.debug("User {} ({}) fetched reminder {}", currentUser.getId(), currentUser.getEmail(), id);

        return mapToDto(reminder);
    }

}
