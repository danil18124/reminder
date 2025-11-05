/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder.controller.IT;

import com.example.reminder.config.TestSecurityConfig;
import com.example.reminder.controller.AbstractIntegrationTest;
import com.example.reminder.model.OAuthProvider;
import com.example.reminder.model.Reminder;
import com.example.reminder.model.Role;
import com.example.reminder.model.User;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 *
 * @author danil
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReminderRestControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    private User testUser;
    private Reminder testReminder;

    @BeforeEach
    void setUp() {

        reminderRepository.deleteAll();
        userRepository.deleteAll();

        // 1. создаём юзера
        String uniqueProviderId = "google-" + UUID.randomUUID();

        testUser = userRepository.save(User.builder()
                .email("test@example.com")
                .provider(OAuthProvider.GOOGLE)
                .providerId(uniqueProviderId)
                .role(Role.USER)
                .build());

        // 2. создаём напоминание
        testReminder = reminderRepository.save(Reminder.builder()
                .title("Test title")
                .description("Test desc")
                .remind(Instant.now().plusSeconds(3600))
                .user(testUser)
                .build());

        // 3. мок JWT
        // заглушаем decode()
        Jwt jwt = new Jwt(
                "fake-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of(
                        "sub", uniqueProviderId,
                        "email", "test@example.com"
                )
        );
        when(jwtDecoder.decode("fake-token")).thenReturn(jwt);
    }

    // -------------------------------------------
    // deleteReminder
    @Test
    void shouldDeleteReminder_whenReminderExistsForUser() throws Exception {
        // ensure reminder exists before delete
        assertThat(reminderRepository.findById(testReminder.getId())).isPresent();

        // вызов DELETE /api/v1/reminder/{id}
        mockMvc.perform(delete("/api/v1/reminder/{id}", testReminder.getId())
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // проверяем что запись реально удалена
        assertThat(reminderRepository.findById(testReminder.getId())).isEmpty();
    }

    @Test
    void shouldReturnNotFound_whenDeletingReminderOfAnotherUser() throws Exception {
        // создаём другого пользователя
        User otherUser = userRepository.save(User.builder()
                .email("other@example.com")
                .provider(OAuthProvider.GOOGLE)
                .providerId("google-" + UUID.randomUUID())
                .role(Role.USER)
                .build());

        Reminder otherReminder = reminderRepository.save(Reminder.builder()
                .title("Other reminder")
                .description("Other desc")
                .remind(Instant.now().plusSeconds(7200))
                .user(otherUser)
                .build());

        // выполняем DELETE с JWT testUser (но reminder принадлежит другому user)
        mockMvc.perform(delete("/api/v1/reminder/{id}", otherReminder.getId())
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // убедимся, что reminder не удалился из БД
        assertThat(reminderRepository.findById(otherReminder.getId())).isPresent();
    }

    // -------------------------------
    // updateReminder
    @Test
    void shouldUpdateReminder_whenReminderExistsForUser() throws Exception {
        String payloadJson = """
            {
              "title": "Updated title",
              "description": "Updated desc",
              "remind": "%s"
            }
            """.formatted(Instant.now().plusSeconds(7200).toString());

        mockMvc.perform(patch("/api/v1/reminder/{id}", testReminder.getId())
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.description").value("Updated desc"));

        // проверяем, что реально обновилось в БД
        Reminder updated = reminderRepository.findById(testReminder.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Updated title");
        assertThat(updated.getDescription()).isEqualTo("Updated desc");
    }

    @Test
    void shouldReturnBadRequest_whenPayloadInvalid() throws Exception {
        // например, пустой title (нарушает @Valid)
        String invalidPayload = """
            {
              "title": "",
              "description": "desc",
              "remind": "%s"
            }
            """.formatted(Instant.now().plusSeconds(7200).toString());

        mockMvc.perform(patch("/api/v1/reminder/{id}", testReminder.getId())
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------
    @Test
    void shouldReturnReminder_whenExistsForUser() throws Exception {
        mockMvc.perform(get("/api/v1/reminder/{id}", testReminder.getId())
                .header("Authorization", "Bearer fake-token")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testReminder.getId()))
                .andExpect(jsonPath("$.title").value(testReminder.getTitle()))
                .andExpect(jsonPath("$.description").value(testReminder.getDescription()));

        // Дополнительно проверим, что reminder реально принадлежит testUser
        Reminder reminderFromDb = reminderRepository.findById(testReminder.getId()).orElseThrow();
        assertThat(reminderFromDb.getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void shouldReturnNotFound_whenReminderDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/reminder/{id}", 9999L)
                .header("Authorization", "Bearer fake-token")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFound_whenReminderBelongsToAnotherUser() throws Exception {
        // создаём другого пользователя
        User otherUser = userRepository.save(User.builder()
                .email("other@example.com")
                .provider(OAuthProvider.GOOGLE)
                .providerId("google-" + UUID.randomUUID())
                .role(Role.USER)
                .build());

        Reminder otherReminder = reminderRepository.save(Reminder.builder()
                .title("Other reminder")
                .description("Other desc")
                .remind(Instant.now().plusSeconds(7200))
                .user(otherUser)
                .build());

        // Тот же JWT (для testUser), но reminder принадлежит другому user
        mockMvc.perform(get("/api/v1/reminder/{id}", otherReminder.getId())
                .header("Authorization", "Bearer fake-token")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
