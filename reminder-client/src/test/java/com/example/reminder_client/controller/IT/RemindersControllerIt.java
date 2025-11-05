/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder_client.controller.IT;

import com.example.reminder_client.controller.payload.NewReminderForm;
import com.example.reminder_client.model.ReminderDto;
import com.example.reminder_client.service.RestClientReminderRestClient;
import com.example.reminder_client.service.dto.PagedResponse;
import com.example.reminder_client.service.dto.Result;
import com.example.reminder_client.service.payload.NewReminderPayload;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import org.springframework.web.client.RestClientResponseException;

/**
 *
 * @author danil
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class RemindersControllerIt {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestClientReminderRestClient restClient;

    @MockBean
    private OAuth2AuthorizedClientService authorizedClientService;

    // ================
    /**
     * Замокать выдачу access token из OAuth2AuthorizedClientService
     */
    private void mockAccessToken(String registrationId, String principalName, String tokenValue) {
        var now = Instant.now();
        var accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenValue,
                now,
                now.plusSeconds(3600)
        );
        var client = mock(OAuth2AuthorizedClient.class);
        when(client.getAccessToken()).thenReturn(accessToken);

        when(authorizedClientService.loadAuthorizedClient(eq(registrationId), eq(principalName)))
                .thenReturn(client);
    }

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

    /**
     * Построить PagedResponse мок-объектом: content/number/size/totalPages
     */
    private <T> PagedResponse<T> pageResponse(List<T> content, int number, int size, int totalPages) {
        @SuppressWarnings("unchecked")
        PagedResponse<T> page = mock(PagedResponse.class, RETURNS_DEEP_STUBS);
        when(page.content()).thenReturn(content);
        when(page.number()).thenReturn(number);
        when(page.size()).thenReturn(size);
        when(page.totalPages()).thenReturn(totalPages);
        return page;
    }

    // ================
    @Test
    void shouldRenderMain_withDefaults_whenAuthenticatedAndHasAccessToken() throws Exception {
        // --- given ---
        var reminders = List.of(
                new ReminderDto(1L, "Buy protein", "desc", null, 1L),
                new ReminderDto(2L, "Call mom", "desc", null, 1L)
        );

        var pageResponse = pageResponse(reminders, 0, 3, 3);
        when(restClient.findAllSortedByTitle(0, 3)).thenReturn(pageResponse);

        var registrationId = "google";
        var principalName = "user123";
        var idTokenValue = "id-token-123";
        var authentication = oidcAuth(registrationId, principalName, idTokenValue);

        // выдача access token через сервис авторизованного клиента
        mockAccessToken(registrationId, principalName, "mock-access-token");

        // --- when / then ---
        mockMvc.perform(get("/client/reminder/main/bytitle")
                .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("reminders", hasSize(2)))
                .andExpect(model().attribute("currentPage", equalTo(0)))
                .andExpect(model().attribute("totalPages", equalTo(3)))
                .andExpect(model().attribute("size", equalTo(3)))
                .andExpect(model().attribute("sort", equalTo("bytitle")))
                .andExpect(model().attributeExists("auth"))
                .andExpect(model().attribute("idToken", equalTo(idTokenValue)))
                .andExpect(model().attribute("accessToken", equalTo("mock-access-token")));

        verify(restClient, times(1)).findAllSortedByTitle(0, 3);
        verifyNoMoreInteractions(restClient);

        verify(authorizedClientService, times(1))
                .loadAuthorizedClient(eq(registrationId), eq(principalName));
        verifyNoMoreInteractions(authorizedClientService);
    }

    @Test
    void shouldRenderMain_withCustomPagination_whenAuthenticatedAndHasAccessToken() throws Exception {
        // --- given ---
        var reminders = List.of(
                new ReminderDto(10L, "Alpha", "desc", null, 1L),
                new ReminderDto(11L, "Beta", "desc", null, 1L)
        );
        // страница №2, размер 5, всего 4 страницы
        var pageResponse = pageResponse(reminders, 2, 5, 4);
        when(restClient.findAllSortedByTitle(2, 5)).thenReturn(pageResponse);

        var registrationId = "google";
        var principalName = "user123";
        var idTokenValue = "id-token-123";
        var authentication = oidcAuth(registrationId, principalName, idTokenValue);

        mockAccessToken(registrationId, principalName, "mock-access-token");

        // --- when / then ---
        mockMvc.perform(get("/client/reminder/main/bytitle")
                .param("page", "2")
                .param("size", "5")
                .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                // проверяем переопределение пагинации
                .andExpect(model().attribute("size", equalTo(5)))
                .andExpect(model().attribute("currentPage", equalTo(2)))
                .andExpect(model().attribute("totalPages", equalTo(4)))
                // остальные инварианты
                .andExpect(model().attribute("reminders", hasSize(2)))
                .andExpect(model().attribute("sort", equalTo("bytitle")))
                .andExpect(model().attributeExists("auth"))
                .andExpect(model().attribute("idToken", equalTo(idTokenValue)))
                .andExpect(model().attribute("accessToken", equalTo("mock-access-token")));

        verify(restClient, times(1)).findAllSortedByTitle(2, 5);
        verifyNoMoreInteractions(restClient);

        verify(authorizedClientService, times(1))
                .loadAuthorizedClient(eq(registrationId), eq(principalName));
        verifyNoMoreInteractions(authorizedClientService);
    }

    @Test
    void shouldRenderMain_whenUnauthenticated_thenNoDataAndNoClientCalls() throws Exception {
        // --- when / then ---
        mockMvc.perform(get("/client/reminder/main/bytitle")) // authentication == null
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                // в модели НЕТ бизнес-атрибутов и токенов
                .andExpect(model().attributeDoesNotExist(
                        "reminders", "currentPage", "totalPages", "size", "sort",
                        "idToken", "accessToken", "auth"
                ));

        // restClient и authorizedClientService не вызывались
        verifyNoInteractions(restClient);
        verifyNoInteractions(authorizedClientService);
    }

    @Test
    void shouldRenderMain_whenAuthenticatedButNoAuthorizedClient_thenNoDataAndNoClientCalls() throws Exception {
        // --- given ---
        var registrationId = "google";
        var principalName = "user123";
        var idTokenValue = "id-token-123";
        var auth = oidcAuth(registrationId, principalName, idTokenValue);

        // сервис возвращает null вместо клиента с токеном
        when(authorizedClientService.loadAuthorizedClient(eq(registrationId), eq(principalName)))
                .thenReturn(null);

        // --- when / then ---
        mockMvc.perform(get("/client/reminder/main/bytitle")
                .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                // В МОДЕЛИ НЕТ бизнес-атрибутов и токенов
                .andExpect(model().attributeDoesNotExist(
                        "reminders", "currentPage", "totalPages", "size", "sort",
                        "idToken", "accessToken", "auth"
                ));

        // RestClient не вызывался
        verifyNoInteractions(restClient);

        // authorizedClientService дернули один раз с корректными ключами
        verify(authorizedClientService, times(1))
                .loadAuthorizedClient(eq(registrationId), eq(principalName));
        verifyNoMoreInteractions(authorizedClientService);
    }

    @Test
    void shouldRenderAdviceErrorPage_whenRestClientResponseExceptionThrown() throws Exception {
        // --- given ---
        var registrationId = "google";
        var principalName = "user123";
        var idTokenValue = "id-token-123";
        var auth = oidcAuth(registrationId, principalName, idTokenValue);

        // авторизованный клиент с access token
        mockAccessToken(registrationId, principalName, "mock-access-token");

        // RestClient кидает 502 Bad Gateway с телом-JSON (под парсер ApiError)
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = """
        {"message":"Oops","details":{"title":["too short"]}}
        """.getBytes(StandardCharsets.UTF_8);

        when(restClient.findAllSortedByTitle(0, 3)).thenThrow(
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
        mockMvc.perform(get("/client/reminder/main/bytitle")
                .with(authentication(auth)))
                // статус из исключения (502)
                .andExpect(status().isBadGateway())
                // view от Advice
                .andExpect(view().name("errors/4xx5xx"))
                // бизнес-атрибутов и токенов быть не должно
                .andExpect(model().attributeDoesNotExist(
                        "reminders", "currentPage", "totalPages", "size", "sort",
                        "idToken", "accessToken", "auth"
                ))
                // атрибуты ошибки из Advice
                .andExpect(model().attribute("errorMessage", equalTo("Oops")))
                .andExpect(model().attribute("errors", allOf(
                        instanceOf(Map.class),
                        hasEntry(equalTo("title"), hasItem("too short"))
                )));

        // вызовы
        verify(authorizedClientService, times(1))
                .loadAuthorizedClient(eq(registrationId), eq(principalName));
        verifyNoMoreInteractions(authorizedClientService);

        verify(restClient, times(1)).findAllSortedByTitle(0, 3);
        verifyNoMoreInteractions(restClient);
    }

    // =======================================================
    // createNewReminder
    @Test
    void shouldRedirectToList_whenCreateReminderSucceeds() throws Exception {
        // --- given ---
        String title = "Buy protein";
        String description = "desc";
        String remindStr = "2025-11-05T10:30:00"; // строка с секундами

        @SuppressWarnings("unchecked")
        Result<ReminderDto> ok = mock(Result.class);
        when(ok.isSuccess()).thenReturn(true);
        when(restClient.createReminder(any())).thenReturn(ok);

        var auth = oidcAuth("google", "user123", "id-token-123");

        // ⬇️ ожидаемый Instant считается из LocalDateTime (а не LocalDate!)
        LocalDateTime ldt = LocalDateTime.parse(remindStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        Instant expectedInstant = ldt.atZone(ZoneId.systemDefault()).toInstant();

        // --- when / then ---
        mockMvc.perform(post("/client/reminder/create")
                .with(authentication(auth))
                .with(csrf())
                // ⬇️ главное отличие: передаём ГОТОВЫЙ объект формы — биндинг не участвует
                .flashAttr("newReminderForm", new NewReminderForm(title, description, ldt)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/reminder/main/bytitle"))
                .andExpect(flash().attributeCount(0));

        // Проверяем payload
        ArgumentCaptor<NewReminderPayload> captor = ArgumentCaptor.forClass(NewReminderPayload.class);
        verify(restClient, times(1)).createReminder(captor.capture());
        verifyNoMoreInteractions(restClient);

        NewReminderPayload sent = captor.getValue();
        assertThat(sent.title()).isEqualTo(title);
        assertThat(sent.description()).isEqualTo(description);
        assertThat(sent.remind()).isEqualTo(expectedInstant);
    }

    @Test
    void shouldReturnFormWithErrors_whenRemindIsNull() throws Exception {
        // --- given ---
        String title = "Buy protein";
        String description = "desc";

        var auth = oidcAuth("google", "user123", "id-token-123");

        // --- when / then ---
        mockMvc.perform(post("/client/reminder/create")
                .with(authentication(auth))
                .with(csrf())
                // передаём готовую форму с remind = null
                .flashAttr("newReminderForm", new NewReminderForm(title, description, null)))
                .andExpect(status().isOk())
                .andExpect(view().name("new-reminder-page"))
                // ошибки и сообщение
                .andExpect(model().attribute("errorMessage", equalTo("Invalid input data")))
                .andExpect(model().attribute("errors", allOf(
                        instanceOf(Map.class),
                        hasEntry(equalTo("remind"), hasItem("Remind is required"))
                )))
                // echo-поля
                .andExpect(model().attribute("title", equalTo(title)))
                .andExpect(model().attribute("description", equalTo(description)))
                .andExpect(model().attribute("remind", nullValue()));

        // REST-клиент не должен вызываться
        verifyNoInteractions(restClient);
        // и сервис авторизованного клиента тоже (этот метод его не трогает)
        verifyNoInteractions(authorizedClientService);
    }

    @Test
    void shouldRedirectToLogin_whenUnauthenticated_onCreate() throws Exception {
        // --- when / then ---
        mockMvc.perform(post("/client/reminder/create")
                .with(csrf()) // избегаем 403 из-за CSRF, чтобы проверить именно security-редирект
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.TEXT_HTML)
                .characterEncoding("UTF-8")
                .param("title", "Buy protein")
                .param("description", "desc")
                .param("remind", "2025-11-05T10:30"))
                .andExpect(status().is3xxRedirection())
                // В зависимости от конфигурации это может быть /login (formLogin)
                // или /oauth2/authorization/{registrationId} (OAuth2).
                .andExpect(header().string("Location",
                        anyOf(containsString("/login"), containsString("/oauth2/authorization"))));

        // Никаких вызовов в сторону REST и клиента авторизации быть не должно
        verifyNoInteractions(restClient);
        verifyNoInteractions(authorizedClientService);
    }

    @Test
    void shouldRenderAdviceErrorPage_whenCreateThrowsRestClientResponseException() throws Exception {
        // --- given ---
        String title = "Buy protein";
        String description = "desc";
        LocalDateTime remind = LocalDateTime.of(2025, 11, 5, 10, 30);

        var auth = oidcAuth("google", "user123", "id-token-123");

        // Исключение от RestClient: 502 + JSON-тело, которое парсит твой Advice
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        byte[] body = """
        {"message":"Oops","details":{"title":["too short"]}}
        """.getBytes(StandardCharsets.UTF_8);

        when(restClient.createReminder(any())).thenThrow(
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
        mockMvc.perform(post("/client/reminder/create")
                .with(authentication(auth))
                .with(csrf())
                // Минуем строковый биндинг — кладём готовую форму
                .flashAttr("newReminderForm", new NewReminderForm(title, description, remind)))
                .andExpect(status().isBadGateway())
                .andExpect(view().name("errors/4xx5xx"))
                // В модели НЕТ echo-полей формы и бизнес-атрибутов
                .andExpect(model().attributeDoesNotExist("title", "description", "remind"))
                // В модели ЕСТЬ данные ошибки из Advice
                .andExpect(model().attribute("errorMessage", equalTo("Oops")))
                .andExpect(model().attribute("errors", allOf(
                        instanceOf(Map.class),
                        hasEntry(equalTo("title"), hasItem("too short"))
                )));

        verify(restClient, times(1)).createReminder(any(NewReminderPayload.class));
        verifyNoMoreInteractions(restClient);
        // Метод create не использует authorizedClientService
        verifyNoInteractions(authorizedClientService);
    }

}
