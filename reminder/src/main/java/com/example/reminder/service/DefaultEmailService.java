package com.example.reminder.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendTextEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom("danil18124@gmail.com"); 

        mailSender.send(message);
    }
}
