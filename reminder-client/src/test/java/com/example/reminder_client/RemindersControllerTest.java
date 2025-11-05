/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder_client;

import com.example.reminder_client.config.TestConfig;
import com.example.reminder_client.controller.RemindersController;
import com.example.reminder_client.controller.RemindersController;
import com.example.reminder_client.model.ReminderDto;
import com.example.reminder_client.service.RestClientReminderRestClient;
import com.example.reminder_client.service.dto.ApiErrorResponse;
import com.example.reminder_client.service.dto.PagedResponse;
import com.example.reminder_client.service.dto.Result;
import com.example.reminder_client.service.payload.NewReminderPayload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

/**
 *
 * @author danil
 */
@WebMvcTest(RemindersController.class)
@Import(TestConfig.class)
public class RemindersControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    RestClientReminderRestClient restClient;

    @MockBean
    OAuth2AuthorizedClientService authorizedClientService;

    private ClientRegistration google() {
        return ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .scope("openid", "profile", "email")
                .build();
    }

    private DefaultOidcUser oidcUserNamed(String name) {
        OidcIdToken idToken = new OidcIdToken(
                "DUMMY_ID_TOKEN",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("sub", name) // nameAttributeKey="sub" => authentication.getName()==name
        );
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, "sub");
    }

    // ==========================
    // getRemindersSortedByTitle
    @Test
    void getRemindersSortedByTitle_authenticated_happyPath() throws Exception {
        // -------- arrange
        int page = 2;
        int size = 5;

        String registrationId = "google";
        String principalName = "alice";
        String expectedIdToken = "ID_TOKEN_ABC";

        // ClientRegistration с тем же registrationId, что в SecurityConfig
        ClientRegistration clientReg = ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-id")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .scope("openid", "profile", "email")
                .build();

        // Access Token для authorized client
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "ACCESS_TOKEN_123",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Set.of("openid", "profile")
        );
        OAuth2AuthorizedClient authorizedClient
                = new OAuth2AuthorizedClient(clientReg, principalName, accessToken);

        // Мокаем сервис: ожидаем вызов с (registrationId, principalName)
        when(authorizedClientService.loadAuthorizedClient(registrationId, principalName))
                .thenReturn(authorizedClient);

        // OIDC principal (getName() = "alice")
        OidcIdToken idToken = new OidcIdToken(
                expectedIdToken,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("sub", principalName, "email", "alice@example.com")
        );
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                idToken,
                "sub" // name attribute -> getName() == "alice"
        );

        // Ответ от REST-клиента
        @SuppressWarnings("unchecked")
        PagedResponse<ReminderDto> pageResponse = mock(PagedResponse.class);
        List<ReminderDto> content = List.of(
                new ReminderDto(1L, "A", "descA", OffsetDateTime.now(ZoneId.systemDefault()), 1L),
                new ReminderDto(2L, "B", "descB", OffsetDateTime.now(ZoneId.systemDefault()), 1L)
        );
        when(pageResponse.content()).thenReturn(content);
        when(pageResponse.number()).thenReturn(page);
        when(pageResponse.totalPages()).thenReturn(10);
        when(pageResponse.size()).thenReturn(size);

        when(restClient.findAllSortedByTitle(page, size)).thenReturn(pageResponse);

        // -------- act + assert
        mockMvc.perform(get("/client/reminder/main/bytitle")
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .with(oidcLogin()
                        .clientRegistration(clientReg)
                        .oidcUser(oidcUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                // Данные страницы
                .andExpect(model().attribute("reminders", content))
                .andExpect(model().attribute("currentPage", page))
                .andExpect(model().attribute("totalPages", 10))
                .andExpect(model().attribute("size", size))
                .andExpect(model().attribute("sort", "bytitle"))
                // Аутентификация и токены в модели
                .andExpect(model().attributeExists("auth"))
                .andExpect(model().attribute("idToken", expectedIdToken))
                .andExpect(model().attribute("accessToken", accessToken.getTokenValue()));

        // -------- verifies
        verify(authorizedClientService).loadAuthorizedClient(registrationId, principalName);
        verify(restClient, times(1)).findAllSortedByTitle(page, size);
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void getRemindersSortedByTitle_whenAnonymous_permitAll_rendersMain_withoutData_andNoBackendCalls() throws Exception {
        // без аутентификации
        mockMvc.perform(get("/client/reminder/main/bytitle")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                // ничего из «аутентифицированной» ветки не добавлено
                .andExpect(model().attributeDoesNotExist(
                        "reminders", "currentPage", "totalPages", "size", "sort",
                        "auth", "idToken", "accessToken"
                ));

        // никаких обращений к бэкенду и за токенами
        verifyNoInteractions(restClient, authorizedClientService);
    }

    @Test
    void authenticated_butAuthorizedClientIsNull_returnsMain_withoutData_andNoBackendCalls() throws Exception {
        var reg = google();
        String principalName = "alice";
        var user = oidcUserNamed(principalName);

        // authorized client не найден
        when(authorizedClientService.loadAuthorizedClient(reg.getRegistrationId(), principalName)).thenReturn(null);

        mockMvc.perform(get("/client/reminder/main/bytitle")
                .param("page", "1").param("size", "10")
                .with(oidcLogin().clientRegistration(reg).oidcUser(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeDoesNotExist(
                        "reminders", "currentPage", "totalPages", "size", "sort",
                        "auth", "idToken", "accessToken"
                ));

        verifyNoInteractions(restClient);
        verify(authorizedClientService).loadAuthorizedClient(eq("google"), eq(principalName));
        verifyNoMoreInteractions(authorizedClientService);
    }

    @Test
    void authenticated_authorizedClientWithoutAccessToken_returnsMain_withoutData_andNoBackendCalls() throws Exception {
        var reg = google();
        String principalName = "bob";
        var user = oidcUserNamed(principalName);

        // есть authorized client, но без access token
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        when(client.getAccessToken()).thenReturn(null);
        when(authorizedClientService.loadAuthorizedClient(reg.getRegistrationId(), principalName)).thenReturn(client);

        mockMvc.perform(get("/client/reminder/main/bytitle")
                .param("page", "0").param("size", "3")
                .with(oidcLogin().clientRegistration(reg).oidcUser(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeDoesNotExist(
                        "reminders", "currentPage", "totalPages", "size", "sort",
                        "auth", "idToken", "accessToken"
                ));

        verifyNoInteractions(restClient);
        verify(authorizedClientService).loadAuthorizedClient(eq("google"), eq(principalName));
        verifyNoMoreInteractions(authorizedClientService);
    }

    @Test
    void getRemindersSortedByTitle_pagination_paramsArePassed_andModelUsesBackendPageMeta() throws Exception {
        // --- arrange: не-дефолтные параметры запроса
        int reqPage = 7;
        int reqSize = 20;

        // аутентификация OIDC
        String registrationId = "google";
        String principalName = "alice";
        var clientReg = ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-id")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .scope("openid", "profile", "email")
                .build();

        var accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "ACCESS_TOKEN_123",
                Instant.now(), Instant.now().plusSeconds(3600));
        var authorizedClient = new OAuth2AuthorizedClient(clientReg, principalName, accessToken);
        when(authorizedClientService.loadAuthorizedClient(registrationId, principalName))
                .thenReturn(authorizedClient);

        var idToken = new OidcIdToken(
                "ID_TOKEN_ABC", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("sub", principalName));
        var oidcUser = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, "sub");

        // ответ бэкенда (намеренно отличающиеся от запроса метаданные страницы)
        @SuppressWarnings("unchecked")
        PagedResponse<ReminderDto> pageResponse = mock(PagedResponse.class);
        List<ReminderDto> content = List.of(
                new ReminderDto(1L, "A", "dA", OffsetDateTime.now(), 1L),
                new ReminderDto(2L, "B", "dB", OffsetDateTime.now(), 1L)
        );
        when(pageResponse.content()).thenReturn(content);
        when(pageResponse.number()).thenReturn(42);   // <- currentPage берём из ответа
        when(pageResponse.totalPages()).thenReturn(99);
        when(pageResponse.size()).thenReturn(13);     // <- size в модели тоже из ответа
        when(restClient.findAllSortedByTitle(reqPage, reqSize)).thenReturn(pageResponse);

        // --- act + assert
        mockMvc.perform(get("/client/reminder/main/bytitle")
                .param("page", String.valueOf(reqPage))
                .param("size", String.valueOf(reqSize))
                .with(oidcLogin().clientRegistration(clientReg).oidcUser(oidcUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                // данные и мета-данные ИМЕННО из ответа бэкенда
                .andExpect(model().attribute("reminders", content))
                .andExpect(model().attribute("currentPage", 42))
                .andExpect(model().attribute("totalPages", 99))
                .andExpect(model().attribute("size", 13))
                .andExpect(model().attribute("sort", "bytitle"))
                // токены/аутентификация тоже проброшены
                .andExpect(model().attributeExists("auth"))
                .andExpect(model().attribute("idToken", "ID_TOKEN_ABC"))
                .andExpect(model().attribute("accessToken", "ACCESS_TOKEN_123"));

        // проверяем ПРОКИДЫВАНИЕ параметров в клиент
        verify(restClient, times(1)).findAllSortedByTitle(reqPage, reqSize);
        verifyNoMoreInteractions(restClient);

        // и что взяли authorizedClient по registrationId + name из authentication
        verify(authorizedClientService).loadAuthorizedClient(registrationId, principalName);
    }

    // ===============================
    // getRemindersSortedByDate
    // ==================================
    // createNewReminder
    @Test
    void createNewReminder_whenValidForm_andBackendSuccess_redirects_andBuildsCorrectPayload() throws Exception {
        // given: данные формы
        String title = "Buy protein";
        String description = "desc";
        LocalDateTime remindLdt = LocalDateTime.of(2031, 3, 14, 15, 9); // удобная дата :)
        ZoneId zone = ZoneId.systemDefault();
        Instant expectedInstant = remindLdt.atZone(zone).toInstant();

        // backend вернёт success
        var dto = new ReminderDto(1L, title, description, OffsetDateTime.now(zone), 42L);
        when(restClient.createReminder(org.mockito.ArgumentMatchers.any(NewReminderPayload.class)))
                .thenReturn(Result.success(dto));

        // when + then: выполняем POST с корректными параметрами
        mockMvc.perform(post("/client/reminder/create")
                .with(oauth2Login()) // если у тебя CSRF включён — оставь обе строки
                .with(csrf())
                .param("title", title)
                .param("description", description)
                .param("remind", "2031-03-14T15:09"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/reminder/main/bytitle"));

        // проверяем собранный payload
        ArgumentCaptor<NewReminderPayload> captor = ArgumentCaptor.forClass(NewReminderPayload.class);
        verify(restClient, times(1)).createReminder(captor.capture());
        NewReminderPayload payload = captor.getValue();

        assertThat(payload.title()).isEqualTo(title);
        assertThat(payload.description()).isEqualTo(description);
        assertThat(payload.remind()).isEqualTo(expectedInstant); // ключевая проверка конвертации времени

        // других взаимодействий с клиентом/сервисами быть не должно
        verifyNoMoreInteractions(restClient);
        verifyNoInteractions(authorizedClientService);
    }

    @Test
    void createNewReminder_whenRemindIsNull_returnsFormWithErrors_andDoesNotCallBackend() throws Exception {
        // given
        String title = "Buy protein";
        String description = "desc";

        // when + then
        mockMvc.perform(post("/client/reminder/create")
                .with(oauth2Login())
                .with(csrf())
                .param("title", title)
                .param("description", description))
                .andExpect(status().isOk())
                .andExpect(view().name("new-reminder-page"))
                .andExpect(model().attributeExists("errors", "errorMessage", "title", "description"))
                .andExpect(model().attributeDoesNotExist("remind")) // ← вместо attributeExists для remind
                .andExpect(model().attribute("errorMessage", "Invalid input data"))
                .andExpect(model().attribute("errors",
                        org.hamcrest.Matchers.hasEntry(
                                org.hamcrest.Matchers.equalTo("remind"),
                                org.hamcrest.Matchers.contains("Remind is required")
                        )))
                .andExpect(model().attribute("title", title))
                .andExpect(model().attribute("description", description));

        verify(restClient, never()).createReminder(any());
        verifyNoMoreInteractions(restClient);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void createNewReminder_whenBackendReturnsFailure_returnsFormWithBackendErrors_andEchoesForm() throws Exception {
        String title = "Buy protein";
        String description = "desc";
        LocalDateTime remindLdt = LocalDateTime.of(2031, 3, 14, 15, 9);

        // бизнес-ошибка бэкенда
        @SuppressWarnings("unchecked")
        ApiErrorResponse<?> apiError = mock(ApiErrorResponse.class);
        String backendMessage = "Validation failed";
        Map<String, List<String>> details = Map.of(
                "title", List.of("Too short"),
                "description", List.of("Must be at least 10 chars")
        );

        when(apiError.message()).thenReturn(backendMessage);
        // ключевая правка: возвращаем как raw Map из-за Map<String, ?>
        when(apiError.details()).thenReturn((Map) details);

        when(restClient.createReminder(org.mockito.ArgumentMatchers.any(NewReminderPayload.class)))
                .thenReturn(Result.failure(apiError));

        mockMvc.perform(post("/client/reminder/create")
                .with(oauth2Login())
                .with(csrf())
                .param("title", title)
                .param("description", description)
                .param("remind", "2031-03-14T15:09"))
                .andExpect(status().isOk())
                .andExpect(view().name("new-reminder-page"))
                .andExpect(model().attribute("errorMessage", backendMessage))
                .andExpect(model().attribute("errors", details))
                .andExpect(model().attribute("title", title))
                .andExpect(model().attribute("description", description))
                .andExpect(model().attribute("remind", remindLdt));

        verify(restClient).createReminder(org.mockito.ArgumentMatchers.any(NewReminderPayload.class));
        verifyNoMoreInteractions(restClient);
        verifyNoInteractions(authorizedClientService);
    }

    @Test
    void createNewReminder_timeConversion_usesSystemDefaultZone_exactInstant() throws Exception {
        // временно зафиксируем системную таймзону без DST, чтобы тест был детерминированным
        var originalTz = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow")); // без переходов
            ZoneId zone = ZoneId.systemDefault();

            String title = "Buy protein";
            String description = "desc";
            LocalDateTime remindLdt = LocalDateTime.of(2031, 3, 14, 15, 9);
            Instant expectedInstant = remindLdt.atZone(zone).toInstant();

            when(restClient.createReminder(org.mockito.ArgumentMatchers.any(NewReminderPayload.class)))
                    .thenReturn(Result.success(new ReminderDto(1L, title, description, OffsetDateTime.now(zone), 42L)));

            mockMvc.perform(post("/client/reminder/create")
                    .with(oauth2Login())
                    .with(csrf())
                    .param("title", title)
                    .param("description", description)
                    .param("remind", "2031-03-14T15:09"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/client/reminder/main/bytitle"));

            // проверяем, что Instant совпал с ожидаемым
            var captor = ArgumentCaptor.forClass(NewReminderPayload.class);
            verify(restClient).createReminder(captor.capture());
            assertThat(captor.getValue().remind()).isEqualTo(expectedInstant);

            verifyNoMoreInteractions(restClient);
        } finally {
            TimeZone.setDefault(originalTz);
        }
    }

    @Test
    void createNewReminder_timeConversion_dstOverlap_picksEarlierOffset() throws Exception {
        var originalTz = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin")); // зона с DST
            ZoneId zone = ZoneId.systemDefault();

            String title = "DST case";
            String description = "fall back overlap";

            // Осенний возврат часов: 2021-10-31 в Берлине 02:30 происходит дважды
            LocalDateTime remindLdt = LocalDateTime.of(2021, 10, 31, 2, 30);

            // Получим список валидных оффсетов для этого локального времени (их 2) и возьмём ранний (индекс 0)
            var offsets = zone.getRules().getValidOffsets(remindLdt);
            assertThat(offsets).hasSizeGreaterThanOrEqualTo(2);
            var earlierOffset = offsets.get(0); // ранний оффсет (обычно летний, +02:00)
            Instant expectedInstant = remindLdt.atOffset(earlierOffset).toInstant();

            when(restClient.createReminder(org.mockito.ArgumentMatchers.any(NewReminderPayload.class)))
                    .thenReturn(Result.success(new ReminderDto(10L, title, description, OffsetDateTime.now(zone), 7L)));

            mockMvc.perform(post("/client/reminder/create")
                    .with(oauth2Login())
                    .with(csrf())
                    .param("title", title)
                    .param("description", description)
                    .param("remind", "2021-10-31T02:30"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/client/reminder/main/bytitle"));

            var captor = ArgumentCaptor.forClass(NewReminderPayload.class);
            verify(restClient).createReminder(captor.capture());
            assertThat(captor.getValue().remind()).isEqualTo(expectedInstant);

            verifyNoMoreInteractions(restClient);
        } finally {
            TimeZone.setDefault(originalTz);
        }
    }

    // 1) AuthN + CSRF -> redirect (success)
    @Test
    void createNewReminder_whenAuthenticatedWithCsrf_thenRedirects() throws Exception {
        when(restClient.createReminder(org.mockito.ArgumentMatchers.any(NewReminderPayload.class)))
                .thenReturn(Result.success(new ReminderDto(1L, "t", "d", OffsetDateTime.now(), 1L)));

        mockMvc.perform(post("/client/reminder/create")
                .with(oauth2Login())
                .with(csrf())
                .param("title", "t")
                .param("description", "d")
                .param("remind", "2031-03-14T15:09"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/reminder/main/bytitle"));

        verify(restClient, times(1)).createReminder(org.mockito.ArgumentMatchers.any(NewReminderPayload.class));
        verifyNoMoreInteractions(restClient);
    }

// 2) Anonymous + CSRF -> redirect to login, backend not called
    @Test
    void createNewReminder_whenAnonymousWithCsrf_thenRedirectsToLogin_andDoesNotCallBackend() throws Exception {
        mockMvc.perform(post("/client/reminder/create")
                .with(csrf())
                .param("title", "t")
                .param("description", "d")
                .param("remind", "2031-03-14T15:09"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/**"));

        verify(restClient, never()).createReminder(org.mockito.ArgumentMatchers.any());
        verifyNoMoreInteractions(restClient);
    }

// 3) AuthN без CSRF -> 403, backend not called
    @Test
    void createNewReminder_whenAuthenticatedWithoutCsrf_thenForbidden_andDoesNotCallBackend() throws Exception {
        mockMvc.perform(post("/client/reminder/create")
                .with(oauth2Login())
                .param("title", "t")
                .param("description", "d")
                .param("remind", "2031-03-14T15:09"))
                .andExpect(status().isForbidden());

        verify(restClient, never()).createReminder(org.mockito.ArgumentMatchers.any());
        verifyNoMoreInteractions(restClient);
    }

}
