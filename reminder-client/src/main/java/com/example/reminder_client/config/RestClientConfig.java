package com.example.reminder_client.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.client.RestClient;


@Configuration
@EnableConfigurationProperties(RemindersApiProperties.class)
public class RestClientConfig {

    @Bean
    RestClient restClient(RemindersApiProperties props) {
        return RestClient.builder()
            .baseUrl(props.baseUrl())
            //.baseUrl("http://localhost:8080")
            .requestInterceptor((request, body, execution) -> {
                var auth = SecurityContextHolder.getContext().getAuthentication();

                if (auth instanceof OAuth2AuthenticationToken oauth
                        && oauth.getPrincipal() instanceof OidcUser oidcUser
                        && oidcUser.getIdToken() != null) {
                    // ⚠️ Используем ID token как Bearer
                    String idToken = oidcUser.getIdToken().getTokenValue();
                    request.getHeaders().setBearerAuth(idToken);
                }

                return execution.execute(request, body);
            })
            .build();
    }
}

