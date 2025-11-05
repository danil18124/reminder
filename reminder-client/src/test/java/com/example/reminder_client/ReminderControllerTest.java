/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder_client;

import com.example.reminder_client.controller.ReminderController;
import com.example.reminder_client.model.ReminderDto;
import com.example.reminder_client.service.RestClientReminderRestClient;
import com.example.reminder_client.service.dto.ApiErrorResponse;
import com.example.reminder_client.service.dto.Result;
import com.example.reminder_client.service.payload.UpdateReminderPayload;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InOrder;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 *
 * @author danil
 */
@WebMvcTest(ReminderController.class)
class ReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestClientReminderRestClient restClient;

    @MockBean
    private OAuth2AuthorizedClientService authorizedClientService;

    private TimeZone originalTz;

    @BeforeEach
    void rememberTz() {
        originalTz = TimeZone.getDefault();
    }

    @AfterEach
    void restoreTz() {
        TimeZone.setDefault(originalTz);
    }

    // ==============================================
    // getReminderEditPage
    @Test
    void getReminderEditPage_whenAuthenticated_returnsEditViewWithReminder() throws Exception {
        // given
        long id = 123L;
        var dto = new ReminderDto(
                id,
                "Existing title",
                "Existing desc",
                OffsetDateTime.now(ZoneId.systemDefault()),
                1L
        );
        when(restClient.getReminderById(id)).thenReturn(dto);

        // when + then
        mockMvc.perform(get("/client/reminder/{reminderId}/edit", id)
                .with(csrf())
                .with(oauth2Login())) // имитируем залогиненного пользователя
                .andExpect(status().isOk())
                .andExpect(view().name("reminder-edit-page"))
                .andExpect(model().attributeExists("reminder"))
                .andExpect(model().attribute("reminder", dto));

        verify(restClient, times(1)).getReminderById(id);
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void getReminderEditPage_whenUnauthenticated_redirectsToLogin_andDoesNotCallBackend() throws Exception {
        long id = 456L;

        mockMvc.perform(get("/client/reminder/{reminderId}/edit", id))
                .andExpect(status().is3xxRedirection())
                // в приложениях с OAuth2 обычно редирект идёт на /oauth2/authorization/{registrationId}
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/**"));

        verify(restClient, never()).getReminderById(anyLong());
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void getReminderEditPage_whenBackendReturns404_mapsTo404_and4xx5xxView() throws Exception {
        long id = 999L;

        // Сымитируем 404 от REST-сервиса (тело включает message/details под твой парсер)
        String body = """
                {"message":"Reminder not found","details":{}}
                """;
        var ex = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found",
                HttpHeaders.EMPTY,
                body.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );
        when(restClient.getReminderById(id)).thenThrow(ex);

        mockMvc.perform(get("/client/reminder/{reminderId}/edit", id)
                .with(oauth2Login()))
                .andExpect(status().isNotFound())
                // В advice для RestClientResponseException мы возвращаем errors/4xx5xx:
                .andExpect(model().attributeExists("errorMessage", "status", "path", "timestamp"))
                .andExpect(model().attribute("errorMessage", "Reminder not found"));

        verify(restClient).getReminderById(id);
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void getReminderEditPage_whenBackendThrowsRuntimeException_returns500View() throws Exception {
        long id = 42L;

        when(restClient.getReminderById(id)).thenThrow(new RuntimeException("DB connection failed"));

        mockMvc.perform(get("/client/reminder/{reminderId}/edit", id)
                .with(csrf())
                .with(oauth2Login()))
                .andExpect(status().isInternalServerError())
                .andExpect(model().attributeExists("errorMessage", "status", "path", "timestamp"))
                // если добавлял correlationId в advice — можно проверить и его:
                .andExpect(model().attributeExists("correlationId"));

        verify(restClient, times(1)).getReminderById(id);
        verifyNoMoreInteractions(restClient);
    }

    // ==============================================
    
    // updateReminder
    
    @Test
    void updateReminder_whenValidFormAndBackendSuccess_thenRedirectsAndBuildsCorrectPayload() throws Exception {
        // given
        long reminderId = 42L;
        String title = "Buy protein";
        String description = "desc";
        LocalDateTime remindLdt = LocalDateTime.of(2030, 1, 10, 14, 30); // 2030-01-10T14:30

        // именно так же, как делает контроллер: LDT -> Zoned -> Instant
        ZoneId systemZone = ZoneId.systemDefault();
        Instant expectedInstant = remindLdt.atZone(systemZone).toInstant();

        // Твой DTO использует OffsetDateTime
        OffsetDateTime dtoRemind = OffsetDateTime.ofInstant(expectedInstant, systemZone);

        when(restClient.updateReminder(eq(reminderId), any()))
                .thenReturn(Result.success(new ReminderDto(reminderId, title, description, dtoRemind, 1L)));

        // when
        var resultActions = mockMvc.perform(
                post("/client/reminder/{reminderId}/edit", reminderId)
                        .with(csrf())
                        .with(oauth2Login())
                        .param("title", title)
                        .param("description", description)
                        .param("remind", "2030-01-10T14:30") // соответствует полю формы LocalDateTime
        );

        // then: точный редирект
        resultActions
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/reminder/main/bytitle"));

        // Проверяем, что payload собран корректно (особенно Instant!)
        ArgumentCaptor<UpdateReminderPayload> payloadCaptor = ArgumentCaptor.forClass(UpdateReminderPayload.class);
        verify(restClient).updateReminder(eq(reminderId), payloadCaptor.capture());

        UpdateReminderPayload actual = payloadCaptor.getValue();
        assertThat(actual.title()).isEqualTo(title);
        assertThat(actual.description()).isEqualTo(description);
        assertThat(actual.remind()).isEqualTo(expectedInstant);

        // На счастливом пути не трогаем handleValidationError -> не зовём getReminderById
        verify(restClient, never()).getReminderById(any());

        // никаких лишних вызовов
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void updateReminder_convertsLocalDateTimeToInstant_inUTC() throws Exception {
        // ——— фиксируем системную таймзону в UTC, чтобы тест был детерминированным
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        long id = 42L;
        String title = "Buy protein";
        String description = "desc";
        // безопасная дата (нет DST вопросов в UTC)
        LocalDateTime ldt = LocalDateTime.of(2030, 1, 10, 14, 30);

        ZoneId zone = ZoneId.systemDefault(); // теперь это UTC
        Instant expectedInstant = ldt.atZone(zone).toInstant();
        OffsetDateTime dtoRemind = OffsetDateTime.ofInstant(expectedInstant, zone);

        when(restClient.updateReminder(eq(id), any(UpdateReminderPayload.class)))
                .thenReturn(Result.success(new ReminderDto(id, title, description, dtoRemind, 1L)));

        mockMvc.perform(post("/client/reminder/{reminderId}/edit", id)
                .with(oauth2Login())
                .with(csrf())
                .param("title", title)
                .param("description", description)
                .param("remind", "2030-01-10T14:30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/reminder/main/bytitle"));

        // ——— проверяем, что Instant в payload совпал с ожидаемым
        ArgumentCaptor<UpdateReminderPayload> captor = ArgumentCaptor.forClass(UpdateReminderPayload.class);
        verify(restClient).updateReminder(eq(id), captor.capture());
        UpdateReminderPayload payload = captor.getValue();

        assertThat(payload.title()).isEqualTo(title);
        assertThat(payload.description()).isEqualTo(description);
        assertThat(payload.remind()).isEqualTo(expectedInstant);

        verify(restClient, never()).getReminderById(any());
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void updateReminder_convertsLocalDateTimeToInstant_inDstZone_safeLocalTime() throws Exception {
        // ——— реальная зона с сезонным переводом, берём валидное время (01:30)
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));

        long id = 7L;
        String title = "Call mom";
        String description = "desc2";
        // дата рядом с весенним переходом в Берлине, но валидное локальное время (существует)
        LocalDateTime ldt = LocalDateTime.of(2030, 3, 31, 1, 30);

        ZoneId zone = ZoneId.systemDefault(); // Europe/Berlin
        Instant expectedInstant = ldt.atZone(zone).toInstant();
        OffsetDateTime dtoRemind = OffsetDateTime.ofInstant(expectedInstant, zone);

        when(restClient.updateReminder(eq(id), any(UpdateReminderPayload.class)))
                .thenReturn(Result.success(new ReminderDto(id, title, description, dtoRemind, 1L)));

        mockMvc.perform(post("/client/reminder/{reminderId}/edit", id)
                .with(oauth2Login())
                .with(csrf())
                .param("title", title)
                .param("description", description)
                .param("remind", "2030-03-31T01:30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/reminder/main/bytitle"));

        ArgumentCaptor<UpdateReminderPayload> captor = ArgumentCaptor.forClass(UpdateReminderPayload.class);
        verify(restClient).updateReminder(eq(id), captor.capture());
        UpdateReminderPayload payload = captor.getValue();

        assertThat(payload.remind()).isEqualTo(expectedInstant);
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void updateReminder_whenRemindIsNull_returnsEditView_withErrorsAndEchoes_andDoesNotCallUpdate() throws Exception {
        // given
        long id = 101L;
        String title = "Buy protein";
        String description = "desc";

        // контроллер в handleValidationError дергает getReminderById — подготовим DTO
        var dto = new ReminderDto(
                id,
                "Existing title",
                "Existing desc",
                OffsetDateTime.now(ZoneId.systemDefault()),
                1L
        );
        when(restClient.getReminderById(id)).thenReturn(dto);

        // when + then
        mockMvc.perform(post("/client/reminder/{reminderId}/edit", id)
                .with(oauth2Login())
                .with(csrf())
                .param("title", title)
                .param("description", description)
        // ПАРАМЕТР remind намеренно отсутствует -> form.remind() == null
        )
                .andExpect(status().isOk())
                .andExpect(view().name("reminder-edit-page"))
                // базовые атрибуты модели присутствуют
                .andExpect(model().attributeExists("reminder", "errors", "errorMessage", "title", "description"))
                // reminder из бекенда
                .andExpect(model().attribute("reminder", dto))
                // точное сообщение об ошибке
                .andExpect(model().attribute("errorMessage", "Invalid input data"))
                // карта ошибок содержит remind -> ["Remind is required"]
                .andExpect(model().attribute("errors",
                        hasEntry(equalTo("remind"), hasItem("Remind is required"))
                ))
                // эхо-поля
                .andExpect(model().attribute("title", title))
                .andExpect(model().attribute("description", description))
                .andExpect(model().attribute("remind", nullValue()));

        // критично: updateReminder НЕ должен вызываться
        verify(restClient, never()).updateReminder(anyLong(), ArgumentMatchers.any());

        // а getReminderById — должен, и ровно один раз
        verify(restClient, times(1)).getReminderById(id);

        verifyNoMoreInteractions(restClient);
    }

    @Test
    void updateReminder_whenBackendReturnsFailure_returnsEditView_withNormalizedErrors_echoes_andProperOrder() throws Exception {
        // given: данные формы
        long id = 55L;
        String title = "Buy protein";
        String description = "desc";
        LocalDateTime remindLdt = LocalDateTime.of(2030, 2, 5, 9, 10);

        ZoneId zone = ZoneId.systemDefault();
        Instant expectedInstant = remindLdt.atZone(zone).toInstant();

        // смоделируем "сырые" details с разными типами значений
        Map<String, Object> rawDetails = Map.of(
                "title", "Too short", // строка -> должен стать List.of("Too short")
                "description", List.of("Must be at least 10 chars", "No abusive words") // уже список
        );

        // мок ApiErrorResponse<?> под твой тип
        @SuppressWarnings("unchecked")
        ApiErrorResponse<?> apiError = mock(ApiErrorResponse.class);
        when(apiError.message()).thenReturn("Validation failed");
        doAnswer(inv -> rawDetails).when(apiError).details();

        // restClient сначала возвращает failure(...)
        when(restClient.updateReminder(eq(id), any(UpdateReminderPayload.class)))
                .thenReturn(Result.failure(apiError));

        // handleValidationError подтягивает reminder
        var dto = new ReminderDto(
                id, "Existing title", "Existing desc",
                OffsetDateTime.now(zone), 1L
        );
        when(restClient.getReminderById(id)).thenReturn(dto);

        // when + then
        mockMvc.perform(post("/client/reminder/{reminderId}/edit", id)
                .with(oauth2Login())
                .with(csrf())
                .param("title", title)
                .param("description", description)
                .param("remind", "2030-02-05T09:10"))
                .andExpect(status().isOk())
                .andExpect(view().name("reminder-edit-page"))
                // базовые атрибуты (эхо-поля + ошибка + reminder)
                .andExpect(model().attributeExists("reminder", "errors", "errorMessage", "title", "description", "remind"))
                .andExpect(model().attribute("reminder", dto))
                .andExpect(model().attribute("errorMessage", "Validation failed"))
                // Нормализация ошибок в Map<String, List<String>>
                .andExpect(model().attribute("errors", allOf(
                        hasEntry(equalTo("title"), org.hamcrest.Matchers.contains("Too short")),
                        hasEntry(equalTo("description"), org.hamcrest.Matchers.contains("Must be at least 10 chars", "No abusive words"))
                )))
                // эхо-значения формы
                .andExpect(model().attribute("title", title))
                .andExpect(model().attribute("description", description))
                .andExpect(model().attribute("remind", remindLdt));

        // проверяем payload и конвертацию времени
        ArgumentCaptor<UpdateReminderPayload> captor = ArgumentCaptor.forClass(UpdateReminderPayload.class);
        verify(restClient).updateReminder(eq(id), captor.capture());
        UpdateReminderPayload payload = captor.getValue();
        assertThat(payload.title()).isEqualTo(title);
        assertThat(payload.description()).isEqualTo(description);
        assertThat(payload.remind()).isEqualTo(expectedInstant);

        // порядок вызовов: update → getById
        InOrder inOrder = inOrder(restClient);
        inOrder.verify(restClient).updateReminder(eq(id), any(UpdateReminderPayload.class));
        inOrder.verify(restClient).getReminderById(id);

        verifyNoMoreInteractions(restClient);
    }

    // =================================
    @Test
    void deleteReminder_whenAuthenticatedWithCsrf_thenRedirectsAndCallsBackendOnce() throws Exception {
        long id = 123L;

        mockMvc.perform(post("/client/reminder/{reminderId}/delete", id)
                .with(oauth2Login())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/reminder/main/bytitle"));

        verify(restClient, times(1)).deleteReminder(id);
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void deleteReminder_whenUnauthenticated_thenRedirectsToLogin_andDoesNotCallBackend() throws Exception {
        long id = 456L;

        mockMvc.perform(post("/client/reminder/{reminderId}/delete", id)
                .with(csrf())) // даже с csrf без логина — на логин
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/**"));

        verify(restClient, never()).deleteReminder(anyLong());
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void deleteReminder_whenAuthenticatedWithoutCsrf_thenForbidden_andDoesNotCallBackend() throws Exception {
        long id = 789L;

        mockMvc.perform(post("/client/reminder/{reminderId}/delete", id)
                .with(oauth2Login())) // нет csrf
                .andExpect(status().isForbidden());

        verify(restClient, never()).deleteReminder(anyLong());
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void deleteReminder_whenBackendReturns404_mapsTo404_and4xx5xxView() throws Exception {
        long id = 999L;

        // 404 от REST-сервиса с телом, которое умеет парсить твой advice
        String body = """
            {"message":"Reminder not found","details":{}}
            """;
        var notFound = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found",
                HttpHeaders.EMPTY,
                body.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );
        doThrow(notFound).when(restClient).deleteReminder(id);

        mockMvc.perform(post("/client/reminder/{reminderId}/delete", id)
                .with(oauth2Login())
                .with(csrf())) // убери, если CSRF отключён
                .andExpect(status().isNotFound())
                .andExpect(view().name("errors/4xx5xx"))
                .andExpect(model().attributeExists("errorMessage", "status", "path", "timestamp"))
                .andExpect(model().attribute("errorMessage", "Reminder not found"));

        verify(restClient, times(1)).deleteReminder(id);
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void deleteReminder_whenBackendThrowsRuntimeException_mapsTo500_and5xxView() throws Exception {
        long id = 42L;

        doThrow(new RuntimeException("DB connection failed"))
        .when(restClient).deleteReminder(id);

        mockMvc.perform(post("/client/reminder/{reminderId}/delete", id)
                .with(oauth2Login())
                .with(csrf())) // убери, если CSRF отключён
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("errors/5xx"))
                .andExpect(model().attributeExists("errorMessage", "status", "path", "timestamp"))
                // если в advice добавлял correlationId — можно проверять и его:
                .andExpect(model().attributeExists("correlationId"));

        verify(restClient, times(1)).deleteReminder(id);
        verifyNoMoreInteractions(restClient);
    }
}
