/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder.controller.IT;

import com.example.reminder.controller.AbstractIntegrationTest;
import com.example.reminder.model.OAuthProvider;
import com.example.reminder.model.Reminder;
import com.example.reminder.model.Role;
import com.example.reminder.model.User;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 *
 * @author danil
 */
@SpringBootTest
@AutoConfigureMockMvc
class RemindersRestControllerSearchByDateIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    private User testUser;
    private String providerId;

    @BeforeEach
    void setUp() {
        reminderRepository.deleteAll();
        userRepository.deleteAll();

        String providerId = "google-" + UUID.randomUUID();

        testUser = userRepository.save(User.builder()
                .email("test@example.com")
                .provider(OAuthProvider.GOOGLE)
                .providerId(providerId)
                .role(Role.USER)
                .build());

        // фиксированная дата для стабильности
        LocalDate baseDate = LocalDate.of(2025, 8, 28);

        reminderRepository.save(Reminder.builder()
                .title("Buy protein")
                .description("desc1")
                .remind(baseDate.atTime(10, 0).toInstant(ZoneOffset.UTC)) // 10:00
                .user(testUser)
                .build());

        reminderRepository.save(Reminder.builder()
                .title("Attend meeting")
                .description("desc2")
                .remind(baseDate.atTime(15, 0).toInstant(ZoneOffset.UTC)) // 15:00
                .user(testUser)
                .build());

        reminderRepository.save(Reminder.builder()
                .title("Call mom")
                .description("desc3")
                .remind(baseDate.atTime(20, 0).toInstant(ZoneOffset.UTC)) // 20:00
                .user(testUser)
                .build());

        Jwt jwt = new Jwt(
                "fake-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("sub", providerId, "email", "test@example.com")
        );
        when(jwtDecoder.decode("fake-token")).thenReturn(jwt);
    }

    @Test
    void shouldReturnRemindersForGivenDate() throws Exception {
        mockMvc.perform(get("/api/v1/reminder/search-by-date")
                        .header("Authorization", "Bearer fake-token")
                        .param("date", "2025-08-28")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "remind,asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].title").value("Buy protein"))
                .andExpect(jsonPath("$.content[1].title").value("Attend meeting"))
                .andExpect(jsonPath("$.content[2].title").value("Call mom"))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void shouldReturnEmptyResult_whenNoRemindersOnDate() throws Exception {
        mockMvc.perform(get("/api/v1/reminder/search-by-date")
                        .header("Authorization", "Bearer fake-token")
                        .param("date", "2025-08-29") // соседний день
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "remind,asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldReturnPagedResults_whenSizeIsLimited() throws Exception {
        mockMvc.perform(get("/api/v1/reminder/search-by-date")
                        .header("Authorization", "Bearer fake-token")
                        .param("date", "2025-08-28")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "remind,asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldReturnOnlyUserReminders_whenOtherUserHasReminders() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .email("other@example.com")
                .provider(OAuthProvider.GOOGLE)
                .providerId("google-" + UUID.randomUUID())
                .role(Role.USER)
                .build());

        LocalDate baseDate = LocalDate.of(2025, 8, 28);
        reminderRepository.save(Reminder.builder()
                .title("Other reminder")
                .description("descX")
                .remind(baseDate.atTime(12, 0).toInstant(ZoneOffset.UTC))
                .user(otherUser)
                .build());

        mockMvc.perform(get("/api/v1/reminder/search-by-date")
                        .header("Authorization", "Bearer fake-token")
                        .param("date", "2025-08-28")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "remind,asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3)); // только у testUser
    }
    
}