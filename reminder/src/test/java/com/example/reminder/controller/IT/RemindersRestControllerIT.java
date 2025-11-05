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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 *
 * @author danil
 */
@SpringBootTest
@AutoConfigureMockMvc
class RemindersRestControllerIT extends AbstractIntegrationTest {

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

        providerId = "google-" + UUID.randomUUID();

        testUser = userRepository.save(User.builder()
                .email("test@example.com")
                .provider(OAuthProvider.GOOGLE)
                .providerId(providerId)
                .role(Role.USER)
                .build());

        // создаём reminders с разными title
        reminderRepository.save(Reminder.builder()
                .title("Buy protein")
                .description("desc1")
                .remind(Instant.now().plusSeconds(3600))
                .user(testUser)
                .build());

        reminderRepository.save(Reminder.builder()
                .title("Attend meeting")
                .description("desc2")
                .remind(Instant.now().plusSeconds(7200))
                .user(testUser)
                .build());

        reminderRepository.save(Reminder.builder()
                .title("Call mom")
                .description("desc3")
                .remind(Instant.now().plusSeconds(10800))
                .user(testUser)
                .build());
        
        Jwt jwt = new Jwt(
                "fake-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of(
                        "sub", providerId,
                        "email", "test@example.com"
                )
        );
        when(jwtDecoder.decode("fake-token")).thenReturn(jwt);
    }

    // --------------------------------------------
    @Test
    void shouldCreateReminder_whenPayloadValid() throws Exception {
        String payloadJson = """
            {
              "title": "Buy protein",
              "description": "Take after gym session",
              "remind": "%s"
            }
            """.formatted(Instant.now().plusSeconds(7200).toString());

        mockMvc.perform(post("/api/v1/reminder")
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("Buy protein"))
                .andExpect(jsonPath("$.description").value("Take after gym session"));

        // проверяем что реально создалось в БД
        Reminder saved = reminderRepository.findAll().get(0);
        assertThat(saved.getTitle()).isEqualTo("Buy protein");
        assertThat(saved.getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void shouldReturnBadRequest_whenTitleIsBlank() throws Exception {
        String invalidPayload = """
            {
              "title": "",
              "description": "Invalid reminder",
              "remind": "%s"
            }
            """.formatted(Instant.now().plusSeconds(7200).toString());

        mockMvc.perform(post("/api/v1/reminder")
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequest_whenRemindIsNull() throws Exception {
        String invalidPayload = """
            {
              "title": "Title",
              "description": "Missing remind"
            }
            """;

        mockMvc.perform(post("/api/v1/reminder")
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------
    
    // findAllSortedByTitle
    
    @Test
    void shouldReturnRemindersSortedByTitleDesc() throws Exception {
        mockMvc.perform(get("/api/v1/reminder/sort/title")
                .header("Authorization", "Bearer fake-token")
                .param("page", "0")
                .param("size", "3")
                .param("sort", "title,desc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // проверяем первый элемент — должен быть "Call mom" (по title desc)
                .andExpect(jsonPath("$.content[2].title").value("Call mom"))
                .andExpect(jsonPath("$.content[1].title").value("Buy protein"))
                .andExpect(jsonPath("$.content[0].title").value("Attend meeting"))
                // проверяем метаданные страницы
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void shouldReturnPagedResultsByTitle_whenSizeIsLimited() throws Exception {
        mockMvc.perform(get("/api/v1/reminder/sort/title")
                .header("Authorization", "Bearer fake-token")
                .param("page", "0")
                .param("size", "2")
                .param("sort", "title,desc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldReturnOnlyUserRemindersByTitle_whenOtherUserHasReminders() throws Exception {
        // создаём другого пользователя и reminder
        User otherUser = userRepository.save(User.builder()
                .email("other@example.com")
                .provider(OAuthProvider.GOOGLE)
                .providerId("google-" + UUID.randomUUID())
                .role(Role.USER)
                .build());

        reminderRepository.save(Reminder.builder()
                .title("Zebra task")
                .description("desc4")
                .remind(Instant.now().plusSeconds(14400))
                .user(otherUser)
                .build());

        // текущий JWT соответствует testUser, значит reminder другого не должен попасть
        mockMvc.perform(get("/api/v1/reminder/sort/title")
                .header("Authorization", "Bearer fake-token")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "title,desc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    // ---------------------------------
    @Test
    void shouldReturnRemindersSortedByDateDesc() throws Exception {
        mockMvc.perform(get("/api/v1/reminder/sort/date")
                .header("Authorization", "Bearer fake-token")
                .param("page", "0")
                .param("size", "3")
                .param("sort", "remind,desc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // проверяем порядок: сначала самый "поздний" remind
                .andExpect(jsonPath("$.content[2].title").value("Call mom"))
                .andExpect(jsonPath("$.content[1].title").value("Attend meeting"))
                .andExpect(jsonPath("$.content[0].title").value("Buy protein"))
                // проверяем метаданные
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void shouldReturnPagedResultsByDate_whenSizeIsLimited() throws Exception {
        mockMvc.perform(get("/api/v1/reminder/sort/date")
                .header("Authorization", "Bearer fake-token")
                .param("page", "0")
                .param("size", "2")
                .param("sort", "remind,desc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldReturnOnlyUserRemindersByDate_whenOtherUserHasReminders() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .email("other@example.com")
                .provider(OAuthProvider.GOOGLE)
                .providerId("google-" + UUID.randomUUID())
                .role(Role.USER)
                .build());

        reminderRepository.save(Reminder.builder()
                .title("Other task")
                .description("desc4")
                .remind(Instant.now().plusSeconds(14400)) // +4h
                .user(otherUser)
                .build());

        mockMvc.perform(get("/api/v1/reminder/sort/date")
                .header("Authorization", "Bearer fake-token")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "remind,desc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3)); // только у testUser
    }
    
    // ------------------------------------------------
    
    // findRemindersByDate
    
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
