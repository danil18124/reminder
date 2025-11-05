package com.example.reminder.quartz.job;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import com.example.reminder.service.EmailService;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class EmailReminderJob implements Job {
	
	
	private final EmailService emailService;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap data = context.getMergedJobDataMap();
        String to = data.getString("email");
        String subject = data.getString("subject");
        String message = data.getString("message");
        
        System.out.println("Quartz!");

        emailService.sendTextEmail(to, subject, message);
    }
}

