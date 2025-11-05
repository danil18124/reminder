package com.example.reminder.exception;

public class ReminderNotFoundException extends RuntimeException {

    private final String errorCode = "REMINDER_NOT_FOUND";
    private final Long reminderId;
    private final Long userId;
    private final String email;

    public ReminderNotFoundException(Long reminderId) {
        super("Reminder with id " + reminderId + " not found");
        this.reminderId = reminderId;
        this.userId = null;
        this.email = null;
    }

    public ReminderNotFoundException(Long reminderId, Long userId, String email) {
        super(String.format("Reminder with id %d not found for user %d (%s)", reminderId, userId, email));
        this.reminderId = reminderId;
        this.userId = userId;
        this.email = email;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Long getReminderId() {
        return reminderId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }
}


