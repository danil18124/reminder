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
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 *
 * @author danil
 */
@SpringBootTest
@AutoConfigureMockMvc
class RemindersRestControllerFilterByDateIT extends AbstractIntegrationTest {

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
    // фиксированная точка для стабильных тестов
    private static Instant baseTime;

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

        baseTime = Instant.parse("2025-08-20T00:00:00Z");

        reminderRepository.save(Reminder.builder()
                .title("Buy protein")
                .description("desc1")
                .remind(baseTime.plus(1, ChronoUnit.HOURS)) // 2025-08-20T01:00
                .user(testUser)
                .build());

        reminderRepository.save(Reminder.builder()
                .title("Attend meeting")
                .description("desc2")
                .remind(baseTime.plus(2, ChronoUnit.HOURS)) // 2025-08-20T02:00
                .user(testUser)
                .build());

        reminderRepository.save(Reminder.builder()
                .title("Call mom")
                .description("desc3")
                .remind(baseTime.plus(3, ChronoUnit.HOURS)) // 2025-08-20T03:00
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
    void shouldReturnRemindersWithinDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/reminder/filter/date")
                        .header("Authorization", "Bearer fake-token")
                        .param("start", "2025-08-20T00:00:00Z")
                        .param("end", "2025-08-20T23:59:59Z")
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
    void shouldReturnSubsetWithinNarrowDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/reminder/filter/date")
                        .header("Authorization", "Bearer fake-token")
                        .param("start", "2025-08-20T01:30:00Z")
                        .param("end", "2025-08-20T03:00:00Z")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "remind,asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Attend meeting"))
                .andExpect(jsonPath("$.content[1].title").value("Call mom"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void shouldReturnEmptyResult_whenNoRemindersInRange() throws Exception {
        mockMvc.perform(get("/api/v1/reminder/filter/date")
                        .header("Authorization", "Bearer fake-token")
                        .param("start", "2025-08-21T00:00:00Z")
                        .param("end", "2025-08-21T23:59:59Z")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldRespectPaginationAndSorting() throws Exception {
        mockMvc.perform(get("/api/v1/reminder/filter/date")
                        .header("Authorization", "Bearer fake-token")
                        .param("start", "2025-08-20T00:00:00Z")
                        .param("end", "2025-08-20T23:59:59Z")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "remind,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Call mom")) // самый поздний
                .andExpect(jsonPath("$.content[1].title").value("Attend meeting"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldReturnOnlyUserReminders_whenOtherUserHasRemindersInRange() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .email("other@example.com")
                .provider(OAuthProvider.GOOGLE)
                .providerId("google-" + UUID.randomUUID())
                .role(Role.USER)
                .build());

        reminderRepository.save(Reminder.builder()
                .title("Other reminder")
                .description("descX")
                .remind(baseTime.plus(2, ChronoUnit.HOURS)) // в тот же день
                .user(otherUser)
                .build());

        mockMvc.perform(get("/api/v1/reminder/filter/date")
                        .header("Authorization", "Bearer fake-token")
                        .param("start", "2025-08-20T00:00:00Z")
                        .param("end", "2025-08-20T23:59:59Z")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3)); // только testUser
    }

}