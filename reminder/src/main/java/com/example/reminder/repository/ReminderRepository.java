package com.example.reminder.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.reminder.model.Reminder;
import com.example.reminder.model.User;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    Page<Reminder> findByUser(User user, Pageable pageable);

    Page<Reminder> findByUserAndTitleContainingIgnoreCase(User user, String title, Pageable pageable);

    Page<Reminder> findByUserAndRemindBetween(User user, Instant start, Instant end, Pageable pageable);

    Optional<Reminder> findByIdAndUser(Long id, User user);

}
