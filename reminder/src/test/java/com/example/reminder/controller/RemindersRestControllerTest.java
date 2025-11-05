/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder.controller;

import com.example.reminder.controller.payload.NewReminderPayload;
import com.example.reminder.model.OAuthProvider;
import com.example.reminder.model.dto.ReminderDto;
import com.example.reminder.service.ReminderService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 *
 * @author danil
 */
@WebMvcTest(RemindersRestController.class)
class RemindersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReminderService reminderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createReminder_shouldReturn201AndLocation_whenValidRequest() throws Exception {
        // given

        OffsetDateTime futureDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        NewReminderPayload payload = new NewReminderPayload(
                "Test title",
                "Some description",
                futureDate
        );

        ReminderDto dto = new ReminderDto(
                123L,
                "Test title",
                "Some description",
                futureDate,
                1L
        );

        when(reminderService.createReminder(
                eq(payload),
                eq(OAuthProvider.GOOGLE),
                eq("provider-123"),
                eq("test@example.com")))
                .thenReturn(dto);

        // when / then
        mockMvc.perform(post("/api/v1/reminder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-123")
                .claim("email", "test@example.com")
                )))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/reminder/123")))
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.title").value("Test title"))
                .andExpect(jsonPath("$.description").value("Some description"));

        verify(reminderService).createReminder(eq(payload), eq(OAuthProvider.GOOGLE), eq("provider-123"), eq("test@example.com"));
    }

    @Test
    void createReminder_shouldReturn400_whenInvalidRequest() throws Exception {
        // given: title и description пустые → нарушают @NotBlank и @Size
        NewReminderPayload invalidPayload = new NewReminderPayload(
                "",
                "",
                OffsetDateTime.parse("2025-09-20T10:00:00Z")
        );

        // when / then
        mockMvc.perform(post("/api/v1/reminder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPayload))
                .with(jwt()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reminderService);
    }

    @Test
    void createReminder_usesJwtClaimsForUserIdentification() throws Exception {
        OffsetDateTime futureDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);

        NewReminderPayload payload = new NewReminderPayload(
                "Another title",
                "Another description",
                futureDate
        );

        ReminderDto dto = new ReminderDto(
                321L,
                "Another title",
                "Another description",
                futureDate,
                1L
        );

        when(reminderService.createReminder(any(), any(), any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/reminder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-999")
                .claim("email", "another@example.com")
                )))
                .andExpect(status().isCreated());

        verify(reminderService).createReminder(eq(payload), eq(OAuthProvider.GOOGLE), eq("provider-999"), eq("another@example.com"));
    }

    // ------------------------------------------------------------------
    // findAllSortedByTitle
    @Test
    void findAllSortedByTitle_shouldReturnPage_whenValidRequest() throws Exception {
        // given
        ReminderDto dto1 = new ReminderDto(1L, "Zeta", "desc1", OffsetDateTime.now().plusDays(1), 1L);
        ReminderDto dto2 = new ReminderDto(2L, "Alpha", "desc2", OffsetDateTime.now().plusDays(2), 1L);
        ReminderDto dto3 = new ReminderDto(3L, "Beta", "desc3", OffsetDateTime.now().plusDays(3), 1L);

        Page<ReminderDto> page = new PageImpl<>(
                List.of(dto1, dto2, dto3),
                PageRequest.of(0, 3, Sort.by("title").descending()),
                3
        );

        when(reminderService.findAllSortedByTitle(
                any(Pageable.class),
                eq(OAuthProvider.GOOGLE),
                eq("provider-123"),
                eq("test@example.com")
        )).thenReturn(page);

        // when / then
        mockMvc.perform(get("/api/v1/reminder/sort/title?page=0&size=3&sort=title,desc")
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-123")
                .claim("email", "test@example.com")
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Zeta"))
                .andExpect(jsonPath("$.content[1].title").value("Alpha"))
                .andExpect(jsonPath("$.content[2].title").value("Beta"))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void findAllSortedByTitle_shouldPassJwtClaimsToService() throws Exception {
        // given
        Page<ReminderDto> emptyPage = new PageImpl<>(List.of());
        when(reminderService.findAllSortedByTitle(any(), any(), any(), any())).thenReturn(emptyPage);

        // when
        mockMvc.perform(get("/api/v1/reminder/sort/title?page=0&size=3&sort=title,desc")
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-999")
                .claim("email", "another@example.com")
                )))
                .andExpect(status().isOk());

        // then
        verify(reminderService).findAllSortedByTitle(
                any(Pageable.class),
                eq(OAuthProvider.GOOGLE),
                eq("provider-999"),
                eq("another@example.com")
        );
    }

    // ----------------------------------------------------
    // findAllSortedByDate
    @Test
    void findAllSortedByDate_shouldReturnPage_whenValidRequest() throws Exception {
        // given
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        ReminderDto dto1 = new ReminderDto(1L, "Alpha", "desc1", now.plusDays(3), 1L);
        ReminderDto dto2 = new ReminderDto(2L, "Beta", "desc2", now.plusDays(2), 1L);
        ReminderDto dto3 = new ReminderDto(3L, "Gamma", "desc3", now.plusDays(1), 1L);

        Page<ReminderDto> page = new PageImpl<>(
                List.of(dto1, dto2, dto3),
                PageRequest.of(0, 3, Sort.by("remind").descending()),
                3
        );

        when(reminderService.findAllSortedByDate(
                any(Pageable.class),
                eq(OAuthProvider.GOOGLE),
                eq("provider-123"),
                eq("test@example.com")
        )).thenReturn(page);

        // when / then
        mockMvc.perform(get("/api/v1/reminder/sort/date?page=0&size=3&sort=remind,desc")
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-123")
                .claim("email", "test@example.com")
                )))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Alpha"))
                .andExpect(jsonPath("$.content[1].title").value("Beta"))
                .andExpect(jsonPath("$.content[2].title").value("Gamma"))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void findAllSortedByDate_shouldPassJwtClaimsToService() throws Exception {
        // given
        Page<ReminderDto> emptyPage = new PageImpl<>(List.of());
        when(reminderService.findAllSortedByDate(any(), any(), any(), any())).thenReturn(emptyPage);

        // when
        mockMvc.perform(get("/api/v1/reminder/sort/date?page=0&size=3&sort=remind,desc")
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-999")
                .claim("email", "another@example.com")
                )))
                .andExpect(status().isOk());

        // then
        verify(reminderService).findAllSortedByDate(
                any(Pageable.class),
                eq(OAuthProvider.GOOGLE),
                eq("provider-999"),
                eq("another@example.com")
        );
    }

    // --------------------------------------------------
    // findRemindersByTitle
    @Test
    void findRemindersByTitle_shouldReturnPage_whenValidRequest() throws Exception {
        // given
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        ReminderDto dto1 = new ReminderDto(1L, "Test A", "desc1", now.plusDays(1), 1L);
        ReminderDto dto2 = new ReminderDto(2L, "Test B", "desc2", now.plusDays(2), 1L);

        Page<ReminderDto> page = new PageImpl<>(List.of(dto1, dto2), PageRequest.of(0, 2), 2);

        when(reminderService.findRemindersByTitle(
                eq("test"),
                any(Pageable.class),
                eq(OAuthProvider.GOOGLE),
                eq("provider-123"),
                eq("test@example.com")
        )).thenReturn(page);

        // when / then
        mockMvc.perform(get("/api/v1/reminder?title=test&page=0&size=2&sort=remind,desc")
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-123")
                .claim("email", "test@example.com")
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test A"))
                .andExpect(jsonPath("$.content[1].title").value("Test B"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    void findRemindersByTitle_shouldReturn400_whenTitleIsBlank() throws Exception {
        // given: title пустой
        mockMvc.perform(get("/api/v1/reminder?title=&page=0&size=2&sort=remind,desc")
                .with(jwt()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reminderService);
    }

    @Test
    void findRemindersByTitle_shouldPassJwtClaimsToService() throws Exception {
        // given
        when(reminderService.findRemindersByTitle(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        // when
        mockMvc.perform(get("/api/v1/reminder?title=test&page=0&size=2&sort=remind,desc")
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-999")
                .claim("email", "another@example.com")
                )))
                .andExpect(status().isOk());

        // then
        verify(reminderService).findRemindersByTitle(
                eq("test"),
                any(Pageable.class),
                eq(OAuthProvider.GOOGLE),
                eq("provider-999"),
                eq("another@example.com")
        );
    }

    // ---------------------------------------------
    @Test
    void findRemindersByDate_shouldReturnPage_whenValidRequest() throws Exception {
        // given
        LocalDate date = LocalDate.of(2025, 8, 28);
        Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        ReminderDto dto1 = new ReminderDto(1L, "Test A", "desc1", now.plusHours(1), 1L);
        ReminderDto dto2 = new ReminderDto(2L, "Test B", "desc2", now.plusHours(2), 1L);

        Page<ReminderDto> page = new PageImpl<>(List.of(dto1, dto2), PageRequest.of(0, 3), 2);

        when(reminderService.findRemindersByDateRange(
                eq(startOfDay), eq(endOfDay), any(Pageable.class),
                eq(OAuthProvider.GOOGLE), eq("provider-123"), eq("test@example.com")
        )).thenReturn(page);

        // when / then
        mockMvc.perform(get("/api/v1/reminder/search-by-date?date=2025-08-28&page=0&size=3&sort=remind,asc")
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-123")
                .claim("email", "test@example.com")
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test A"))
                .andExpect(jsonPath("$.content[1].title").value("Test B"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(3));
    }

    @Test
    void findRemindersByDate_shouldReturn400_whenDateIsInvalid() throws Exception {
        // given: некорректная дата
        mockMvc.perform(get("/api/v1/reminder/search-by-date?date=invalid-date&page=0&size=3")
                .with(jwt()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reminderService);
    }

    @Test
    void findRemindersByDate_shouldPassJwtClaimsToService() throws Exception {
        // given
        when(reminderService.findRemindersByDateRange(any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        // when
        mockMvc.perform(get("/api/v1/reminder/search-by-date?date=2025-08-28&page=0&size=3&sort=remind,asc")
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-999")
                .claim("email", "another@example.com")
                )))
                .andExpect(status().isOk());

        // then
        verify(reminderService).findRemindersByDateRange(
                any(Instant.class), any(Instant.class),
                any(Pageable.class),
                eq(OAuthProvider.GOOGLE),
                eq("provider-999"),
                eq("another@example.com")
        );
    }

    // ---------------------------------------------------------
    
    // findRemindersByDateRange
    
    @Test
    void findRemindersByDateRange_shouldReturnPage_whenValidRequest() throws Exception {
        // given
        Instant start = Instant.parse("2025-08-10T00:00:00Z");
        Instant end = Instant.parse("2025-08-30T23:59:59Z");

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        ReminderDto dto1 = new ReminderDto(1L, "Test A", "desc1", now.plusDays(1), 1L);
        ReminderDto dto2 = new ReminderDto(2L, "Test B", "desc2", now.plusDays(2), 1L);

        Page<ReminderDto> page = new PageImpl<>(List.of(dto1, dto2), PageRequest.of(0, 5), 2);

        when(reminderService.findRemindersByDateRange(
                eq(start), eq(end), any(Pageable.class),
                eq(OAuthProvider.GOOGLE), eq("provider-123"), eq("test@example.com")
        )).thenReturn(page);

        // when / then
        mockMvc.perform(get("/api/v1/reminder/filter/date?start=2025-08-10T00:00:00Z&end=2025-08-30T23:59:59Z&page=0&size=5")
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-123")
                .claim("email", "test@example.com")
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test A"))
                .andExpect(jsonPath("$.content[1].title").value("Test B"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void findRemindersByDateRange_shouldReturn400_whenDatesInvalid() throws Exception {
        // given: start некорректный
        mockMvc.perform(get("/api/v1/reminder/filter/date?start=invalid&end=2025-08-30T23:59:59Z&page=0&size=5")
                .with(jwt()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reminderService);
    }

    @Test
    void findRemindersByDateRange_shouldPassJwtClaimsToService() throws Exception {
        // given
        when(reminderService.findRemindersByDateRange(any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        // when
        mockMvc.perform(get("/api/v1/reminder/filter/date?start=2025-08-10T00:00:00Z&end=2025-08-30T23:59:59Z&page=0&size=5")
                .with(jwt().jwt(jwt -> jwt
                .subject("provider-999")
                .claim("email", "another@example.com")
                )))
                .andExpect(status().isOk());

        // then
        verify(reminderService).findRemindersByDateRange(
                any(Instant.class), any(Instant.class),
                any(Pageable.class),
                eq(OAuthProvider.GOOGLE),
                eq("provider-999"),
                eq("another@example.com")
        );
    }

}
