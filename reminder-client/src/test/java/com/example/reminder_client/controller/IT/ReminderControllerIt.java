/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder_client.controller.IT;

import com.example.reminder_client.controller.payload.UpdateReminderForm;
import com.example.reminder_client.model.ReminderDto;
import com.example.reminder_client.service.RestClientReminderRestClient;

import com.example.reminder_client.service.dto.Result;
import com.example.reminder_client.service.payload.UpdateReminderPayload;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;

import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.web.client.RestClientResponseException;

/**
 *
 * @author danil
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ReminderControllerIt {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestClientReminderRestClient restClient;

    @MockBean
    private OAuth2AuthorizedClientService authorizedClientService;

    /**
     * Построить OIDC-аутентификацию с нужными registrationId
     * (clientRegistrationId) и principalName
     */
    private Authentication oidcAuth(String registrationId, String principalName, String idTokenValue) {
        var now = Instant.now();
        var idToken = new OidcIdToken(
                idTokenValue,
                now,
                now.plusSeconds(3600),
                Map.of("sub", principalName, "email", "test@example.com")
        );
        var user = new DefaultOidcUser(AuthorityUtils.createAuthorityList("ROLE_USER"), idToken);
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), registrationId);
    }

    @Test
    void shouldRedirectToList_whenUpdateReminderSucceeds() throws Exception {
        // --- given ---
        long reminderId = 42L;
        String title = "Buy protein";
        String description = "desc";
        LocalDateTime remind = LocalDateTime.of(2025, 11, 5, 10, 30);

        // успешный результат от REST-клиента
        @SuppressWarnings("unchecked")
        Result<ReminderDto> ok = mock(Result.class);
        when(ok.isSuccess()).thenReturn(true);
        when(restClient.updateReminder(eq(reminderId), any(UpdateReminderPayload.class))).thenReturn(ok);

        // аутентификация (если маршрут защищён)
        var auth = oidcAuth("google", "user123", "id-token-123");

        // ожидаемый Instant по семантике контроллера
        Instant expectedInstant = remind.atZone(ZoneId.systemDefault()).toInstant();

        // --- when / then ---
        mockMvc.perform(post("/client/reminder/{reminderId}/edit", reminderId)
                .with(authentication(auth))
                .with(csrf())
                // минуем строковый биндинг — отдаём готовую форму
                .flashAttr("updateReminderForm", new UpdateReminderForm(title, description, remind)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/reminder/main/bytitle"));

        // проверяем, что update вызван 1 раз с корректным payload
        ArgumentCaptor<UpdateReminderPayload> payloadCaptor = ArgumentCaptor.forClass(UpdateReminderPayload.class);
        verify(restClient, times(1)).updateReminder(eq(reminderId), payloadCaptor.capture());
        verifyNoMoreInteractions(restClient);

        UpdateReminderPayload sent = payloadCaptor.getValue();
        assertThat(sent.title()).isEqualTo(title);
        assertThat(sent.description()).isEqualTo(description);
        assertThat(sent.remind()).isEqualTo(expectedInstant);

        // на успешном пути getReminderById не нужен
        verify(restClient, never()).getReminderById(anyLong());
    }

    @Test
    void shouldReturnEditPageWithErrors_whenRemindIsNull_onUpdate() throws Exception {
        // --- given ---
        long reminderId = 42L;
        String title = "Buy protein";
        String description = "desc";

        // аутентификация (если маршрут защищён)
        var auth = oidcAuth("google", "user123", "id-token-123");

        // контроллер в ветке ошибки обязательно подтягивает оригинальный reminder
        var existingReminder = new ReminderDto(reminderId, "Existing title", "Existing desc", null, 1L);
        when(restClient.getReminderById(reminderId)).thenReturn(existingReminder);

        // --- when / then ---
        mockMvc.perform(post("/client/reminder/{reminderId}/edit", reminderId)
                .with(authentication(auth))
                .with(csrf())
                // минуем строковый биндинг — передаём готовую форму с remind = null
                .flashAttr("updateReminderForm", new UpdateReminderForm(title, description, null)))
                .andExpect(status().isOk())
                .andExpect(view().name("reminder-edit-page"))
                // в модели есть DTO reminder, подтянутый из REST
                .andExpect(model().attributeExists("reminder"))
                // ошибки и сообщение
                .andExpect(model().attribute("errorMessage", equalTo("Invalid input data")))
                .andExpect(model().attribute("errors", allOf(
                        instanceOf(Map.class),
                        hasEntry(equalTo("remind"), hasItem("Remind is required"))
                )))
                // echo-поля из формы
                .andExpect(model().attribute("title", equalTo(title)))
                .andExpect(model().attribute("description", equalTo(description)))
                .andExpect(model().attribute("remind", nullValue()));

        // проверки взаимодействий
        verify(restClient, times(1)).getReminderById(eq(reminderId));
        verify(restClient, never()).updateReminder(anyLong(), any(UpdateReminderPayload.class));
        verifyNoMoreInteractions(restClient);

        // этот метод не использует authorizedClientService
        verifyNoInteractions(authorizedClientService);
    }

    @Test
    void shouldRenderAdviceErrorPage_whenUpdateThrowsRestClientResponseException() throws Exception {
        // --- given ---
        long reminderId = 42L;
        String title = "Buy protein";
        String description = "desc";
        LocalDateTime remind = LocalDateTime.of(2025, 11, 5, 10, 30);

        var auth = oidcAuth("google", "user123", "id-token-123");

        // Исключение от REST-клиента: 502 + JSON, которое парсит @ControllerAdvice
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        byte[] body = """
        {"message":"Oops","details":{"title":["too short"]}}
        """.getBytes(StandardCharsets.UTF_8);

        when(restClient.updateReminder(eq(reminderId), any(UpdateReminderPayload.class))).thenThrow(
                new RestClientResponseException(
                        "Bad Gateway",
                        HttpStatus.BAD_GATEWAY.value(),
                        "Bad Gateway",
                        headers,
                        body,
                        StandardCharsets.UTF_8
                )
        );

        // --- when / then ---
        mockMvc.perform(post("/client/reminder/{reminderId}/edit", reminderId)
                .with(authentication(auth))
                .with(csrf())
                // Минуем строковый биндинг — передаём готовую форму
                .flashAttr("updateReminderForm", new UpdateReminderForm(title, description, remind)))
                .andExpect(status().isBadGateway())
                .andExpect(view().name("errors/4xx5xx"))
                // В модели НЕТ echo-полей и reminder
                .andExpect(model().attributeDoesNotExist("reminder", "title", "description", "remind"))
                // В модели ЕСТЬ данные ошибки из Advice
                .andExpect(model().attribute("errorMessage", equalTo("Oops")))
                .andExpect(model().attribute("errors", allOf(
                        instanceOf(Map.class),
                        hasEntry(equalTo("title"), hasItem("too short"))
                )));

        // Взаимодействия
        verify(restClient, times(1)).updateReminder(eq(reminderId), any(UpdateReminderPayload.class));
        verify(restClient, never()).getReminderById(anyLong());
        verifyNoMoreInteractions(restClient);

        // Этот метод контроллера не использует authorizedClientService
        verifyNoInteractions(authorizedClientService);
    }

    @Test
    void shouldRedirectToLogin_whenUnauthenticated_onUpdate() throws Exception {
        // --- when / then ---
        mockMvc.perform(post("/client/reminder/{reminderId}/edit", 42L)
                .with(csrf()) // избегаем 403 из-за CSRF, хотим увидеть security-редирект
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.TEXT_HTML)
                .param("title", "Buy protein")
                .param("description", "desc")
                .param("remind", "2025-11-05T10:30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        anyOf(containsString("/login"),
                                containsString("/oauth2/authorization"))));

        // Ни update, ни подгрузка reminder не должны вызываться
        verify(restClient, never()).updateReminder(anyLong(), any(UpdateReminderPayload.class));
        verify(restClient, never()).getReminderById(anyLong());
        verifyNoMoreInteractions(restClient);

        // В этом методе контроллер не использует AuthorizedClientService — но на всякий случай:
        verifyNoInteractions(authorizedClientService);
    }

    @Test
    void shouldRenderAdviceErrorPage_whenGetReminderByIdThrows_inHandleValidationError() throws Exception {
        // --- given ---
        long reminderId = 42L;
        String title = "Buy protein";
        String description = "desc";

        // аутентификация (если маршрут защищён)
        var auth = oidcAuth("google", "user123", "id-token-123");

        // Сценарий: локальная валидация срабатывает (remind = null) -> handleValidationError(...)
        // Внутри handleValidationError вызывается getReminderById(id), который бросает 404
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        byte[] body = """
        {"message":"Not found","details":{"id":["not found"]}}
        """.getBytes(StandardCharsets.UTF_8);

        when(restClient.getReminderById(eq(reminderId))).thenThrow(
                new RestClientResponseException(
                        "Not Found",
                        HttpStatus.NOT_FOUND.value(),
                        "Not Found",
                        headers,
                        body,
                        StandardCharsets.UTF_8
                )
        );

        // --- when / then ---
        mockMvc.perform(post("/client/reminder/{reminderId}/edit", reminderId)
                .with(authentication(auth))
                .with(csrf())
                // Проваливаемся в локальную валидацию: remind = null
                .flashAttr("updateReminderForm", new UpdateReminderForm(title, description, null)))
                .andExpect(status().isNotFound())
                .andExpect(view().name("errors/4xx5xx"))
                // В модели НЕТ echo-полей и reminder (Advice отрисовывает чистую ошибку)
                .andExpect(model().attributeDoesNotExist("reminder", "title", "description", "remind"))
                // В модели ЕСТЬ нормализованные поля ошибки
                .andExpect(model().attribute("errorMessage", equalTo("Not found")))
                .andExpect(model().attribute("errors", allOf(
                        instanceOf(Map.class),
                        hasEntry(equalTo("id"), hasItem("not found"))
                )));

        // Взаимодействия: getReminderById дернули (упало), update не вызывался
        verify(restClient, times(1)).getReminderById(eq(reminderId));
        verify(restClient, never()).updateReminder(anyLong(), any(UpdateReminderPayload.class));
        verifyNoMoreInteractions(restClient);

        verifyNoInteractions(authorizedClientService);
    }

    // ----- 
    // 1) УСПЕХ: дергаем только updateReminder, getReminderById — ни разу
    @Test
    void calls_update_only_on_success() throws Exception {
        long id = 42L;
        var auth = oidcAuth("google", "user123", "id-token-123");
        var remind = LocalDateTime.of(2025, 11, 5, 10, 30);

        @SuppressWarnings("unchecked")
        Result<ReminderDto> ok = mock(Result.class);
        when(ok.isSuccess()).thenReturn(true);
        when(restClient.updateReminder(eq(id), any(UpdateReminderPayload.class))).thenReturn(ok);

        mockMvc.perform(post("/client/reminder/{reminderId}/edit", id)
                .with(authentication(auth)).with(csrf())
                .flashAttr("updateReminderForm", new UpdateReminderForm("t", "d", remind)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/reminder/main/bytitle"));

        verify(restClient, times(1)).updateReminder(eq(id), any(UpdateReminderPayload.class));
        verify(restClient, never()).getReminderById(anyLong());
        verifyNoMoreInteractions(restClient);
    }

// 2) ЛОКАЛЬНАЯ ВАЛИДАЦИЯ: update не дергается, getReminderById — ровно 1 раз
    @Test
    void calls_getById_only_on_local_validation_error() throws Exception {
        long id = 42L;
        var auth = oidcAuth("google", "user123", "id-token-123");

        when(restClient.getReminderById(id))
                .thenReturn(new ReminderDto(id, "Existing title", "Existing desc", null, 1L));

        mockMvc.perform(post("/client/reminder/{reminderId}/edit", id)
                .with(authentication(auth)).with(csrf())
                // remind = null → локальная проверка сработает и уйдём в handleValidationError(...)
                .flashAttr("updateReminderForm", new UpdateReminderForm("t", "d", null)))
                .andExpect(status().isOk())
                .andExpect(view().name("reminder-edit-page"));

        verify(restClient, never()).updateReminder(anyLong(), any(UpdateReminderPayload.class));
        verify(restClient, times(1)).getReminderById(eq(id));
        verifyNoMoreInteractions(restClient);
    }



}
