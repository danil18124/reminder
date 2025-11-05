package com.example.reminder.service;

public interface EmailService {
	public void sendTextEmail(String to, String subject, String body);
}
