/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder.controller.IT;

import com.example.reminder.model.OAuthProvider;
import com.example.reminder.model.Reminder;
import com.example.reminder.model.Role;
import com.example.reminder.model.User;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.UserRepository;
import java.time.Instant;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 * @author danil
 */

@SpringBootTest
@AutoConfigureMockMvc
public class RemindersRestControllerFindByTitleIT {
    
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


         reminderRepository.save(Reminder.builder()
                .title("Test reminder 1")
                .description("desc1")
                .remind(Instant.now().plusSeconds(3600))
                .user(testUser)
                .build());

        reminderRepository.save(Reminder.builder()
                .title("Another reminder")
                .description("desc2")
                .remind(Instant.now().plusSeconds(7200))
                .user(testUser)
                .build());

        reminderRepository.save(Reminder.builder()
                .title("Test reminder 2")
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
    
        @Test
    void shouldReturnRemindersFilteredByTitle() throws Exception {
        mockMvc.perform(get("/api/v1/reminder")
                        .header("Authorization", "Bearer fake-token")
                        .param("title", "Test")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "remind,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Test reminder 2"))
                .andExpect(jsonPath("$.content[1].title").value("Test reminder 1"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void shouldReturnBadRequest_whenTitleParamIsBlank() throws Exception {
        mockMvc.perform(get("/api/v1/reminder")
                        .header("Authorization", "Bearer fake-token")
                        .param("title", "")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotReturnOtherUserReminders() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .email("other@example.com")
                .provider(OAuthProvider.GOOGLE)
                .providerId("google-" + UUID.randomUUID())
                .role(Role.USER)
                .build());

        reminderRepository.save(Reminder.builder()
                .title("Call mom")
                .description("desc4")
                .remind(Instant.now().plusSeconds(14400))
                .user(otherUser)
                .build());

        mockMvc.perform(get("/api/v1/reminder")
                        .header("Authorization", "Bearer fake-token")
                        .param("title", "Test")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "remind,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2)); // только у testUser
    }
}
