package com.example.reminder.quartz.scheduler;

import java.time.ZoneId;
import java.util.Date;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Service;

import com.example.reminder.model.Reminder;
import com.example.reminder.quartz.job.EmailReminderJob;

import lombok.RequiredArgsConstructor;
import org.quartz.TriggerKey;

@Service
@RequiredArgsConstructor
public class EmailScheduler {

    private final Scheduler scheduler;

    public void schedule(Reminder reminder) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("email", reminder.getUser().getEmail());
        dataMap.put("subject", reminder.getTitle());
        dataMap.put("message", reminder.getDescription());

        JobDetail jobDetail = JobBuilder.newJob(EmailReminderJob.class)
                .withIdentity("email-job-" + reminder.getId(), "email-jobs")
                .setJobData(dataMap)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("email-trigger-" + reminder.getId(), "email-jobs")
                .startAt(Date.from(reminder.getRemind().atZone(ZoneId.systemDefault()).toInstant()))
                .build();

        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to schedule email job", e);
        }
    }

    public void reschedule(Reminder reminder) {
        try {
            // ключи совпадают с теми, что в schedule()
            TriggerKey triggerKey = new TriggerKey("email-trigger-" + reminder.getId(), "email-jobs");

            // создаём новый trigger с обновлённым временем
            Trigger newTrigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .startAt(Date.from(reminder.getRemind().atZone(ZoneId.systemDefault()).toInstant()))
                    .build();

            scheduler.rescheduleJob(triggerKey, newTrigger);
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to reschedule email job", e);
        }
    }

}
