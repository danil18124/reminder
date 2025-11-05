/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder.util;

/**
 *
 * @author danil
 */
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;
import org.assertj.core.api.Assertions;

public class TestLogUtils {

    public static ListAppender<ILoggingEvent> attachAppender(Class<?> clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    public static void assertLogged(ListAppender<ILoggingEvent> appender, Level minLevel, String... expectedParts) {
        boolean found = appender.list.stream().anyMatch(event
                -> event.getLevel().isGreaterOrEqual(minLevel)
                && containsAll(event.getFormattedMessage(), expectedParts)
        );
        Assertions.assertThat(found)
                .withFailMessage("Expected log with level >= %s containing parts %s but not found",
                        minLevel, String.join(", ", expectedParts))
                .isTrue();
    }

    private static boolean containsAll(String message, String... parts) {
        for (String part : parts) {
            if (!message.contains(part)) {
                return false;
            }
        }
        return true;
    }
}
