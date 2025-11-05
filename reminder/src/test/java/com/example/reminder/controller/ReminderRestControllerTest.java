/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder.controller;

import com.example.reminder.controller.payload.UpdateReminderPayload;
import com.example.reminder.model.OAuthProvider;
import com.example.reminder.model.dto.ReminderDto;
import com.example.reminder.service.ReminderService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.MockMvc;

/**
 *
 * @author danil
 */
@WebMvcTest(ReminderRestController.class)
public class ReminderRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReminderService reminderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deleteReminder_shouldReturn204_whenValidRequest() throws Exception {
        // when / then
        mockMvc.perform(delete("/api/v1/reminder/42")
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-123")
                .claim("email", "test@example.com")
                )))
                .andExpect(status().isNoContent());

        verify(reminderService).deleteReminder(42L, OAuthProvider.GOOGLE, "provider-123", "test@example.com");
    }

    @Test
    void deleteReminder_shouldReturn400_whenIdIsInvalid() throws Exception {
        // given: reminderId = "abc" не конвертируется в Long
        mockMvc.perform(delete("/api/v1/reminder/abc")
                .with(jwt()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reminderService);
    }

    @Test
    void deleteReminder_shouldPassJwtClaimsToService() throws Exception {
        // when
        mockMvc.perform(delete("/api/v1/reminder/99")
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-999")
                .claim("email", "another@example.com")
                )))
                .andExpect(status().isNoContent());

        // then
        verify(reminderService).deleteReminder(99L, OAuthProvider.GOOGLE, "provider-999", "another@example.com");
    }

    // ------------------------------------------
    @Test
    void updateReminder_shouldReturn200AndUpdatedReminder_whenValidRequest() throws Exception {
        // given
        OffsetDateTime futureDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        UpdateReminderPayload payload = new UpdateReminderPayload("Updated title", "Updated description", futureDate);

        ReminderDto updated = new ReminderDto(
                42L,
                "Updated title",
                "Updated description",
                futureDate,
                1L
        );

        when(reminderService.updateReminder(eq(42L), eq(payload), eq(OAuthProvider.GOOGLE),
                eq("provider-123"), eq("test@example.com"))).thenReturn(updated);

        // when / then
        mockMvc.perform(patch("/api/v1/reminder/42")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-123")
                .claim("email", "test@example.com")
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void updateReminder_shouldReturn400_whenPayloadInvalid() throws Exception {
        // given: пустой title и description
        UpdateReminderPayload invalidPayload = new UpdateReminderPayload("", "", OffsetDateTime.now().minusDays(1));

        mockMvc.perform(patch("/api/v1/reminder/42")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPayload))
                .with(jwt()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reminderService);
    }

    @Test
    void updateReminder_shouldPassJwtClaimsToService() throws Exception {
        // given
        OffsetDateTime futureDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        UpdateReminderPayload payload = new UpdateReminderPayload("Title", "Desc", futureDate);

        ReminderDto updated = new ReminderDto(99L, "Title", "Desc", futureDate, 1L);
        when(reminderService.updateReminder(anyLong(), any(), any(), any(), any())).thenReturn(updated);

        // when
        mockMvc.perform(patch("/api/v1/reminder/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-999")
                .claim("email", "another@example.com")
                )))
                .andExpect(status().isOk());

        // then
        verify(reminderService).updateReminder(eq(99L), eq(payload), eq(OAuthProvider.GOOGLE),
                eq("provider-999"), eq("another@example.com"));
    }

    // ----------------------------------------------
    @Test
    void getReminderById_shouldReturn200AndReminder_whenValidRequest() throws Exception {
        // given
        OffsetDateTime remind = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        ReminderDto dto = new ReminderDto(42L, "Test title", "Some description", remind, 1L);

        when(reminderService.getById(42L, OAuthProvider.GOOGLE, "provider-123", "test@example.com"))
                .thenReturn(dto);

        // when / then
        mockMvc.perform(get("/api/v1/reminder/42")
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-123")
                .claim("email", "test@example.com")
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.title").value("Test title"))
                .andExpect(jsonPath("$.description").value("Some description"));
    }

    @Test
    void getReminderById_shouldReturn400_whenIdIsInvalid() throws Exception {
        // given: reminderId = "abc" не конвертируется в Long
        mockMvc.perform(get("/api/v1/reminder/abc")
                .with(jwt()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reminderService);
    }

    @Test
    void getReminderById_shouldPassJwtClaimsToService() throws Exception {
        // given
        OffsetDateTime remind = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        ReminderDto dto = new ReminderDto(99L, "Another title", "Desc", remind, 2L);

        when(reminderService.getById(anyLong(), any(), any(), any())).thenReturn(dto);

        // when
        mockMvc.perform(get("/api/v1/reminder/99")
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-999")
                .claim("email", "another@example.com")
                )))
                .andExpect(status().isOk());

        // then
        verify(reminderService).getById(99L, OAuthProvider.GOOGLE, "provider-999", "another@example.com");
    }

}
