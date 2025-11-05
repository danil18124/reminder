/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder_client.controller;

import com.example.reminder_client.controller.payload.NewReminderForm;
import com.example.reminder_client.model.ReminderDto;
import com.example.reminder_client.service.RestClientReminderRestClient;
import com.example.reminder_client.service.dto.PagedResponse;
import com.example.reminder_client.service.dto.Result;
import com.example.reminder_client.service.payload.NewReminderPayload;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author danil
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/client/reminder")
@Slf4j
public class RemindersController {
    private final RestClientReminderRestClient restClient;
    private final OAuth2AuthorizedClientService authorizedClientService;

    

    @GetMapping("/main/bytitle")
    public String getRemindersSortedByTitle(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            Model model,
            OAuth2AuthenticationToken authentication) {
        
        if (authentication != null && authentication.isAuthenticated()) {
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    authentication.getAuthorizedClientRegistrationId(),
                    authentication.getName()
            );

            if (client != null && client.getAccessToken() != null) {
                String accessToken = client.getAccessToken().getTokenValue();

                OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
                String idToken = oidcUser.getIdToken().getTokenValue();

                PagedResponse<ReminderDto> pageResponse = this.restClient.findAllSortedByTitle(page, size);

                model.addAttribute("reminders", pageResponse.content());
                model.addAttribute("currentPage", pageResponse.number());
                model.addAttribute("totalPages", pageResponse.totalPages());
                model.addAttribute("size", pageResponse.size());
                model.addAttribute("sort", "bytitle");
                model.addAttribute("auth", authentication);

                model.addAttribute("idToken", idToken);
                model.addAttribute("accessToken", accessToken);
            }
        }

        return "main";
    }
    
    @GetMapping("/main")
    public String getMainPage() {
        return "redirect:/client/reminder/main/bytitle";
    }

    @GetMapping("/main/bydate")
    public String getRemindersSortedByDate(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            Model model,
            OAuth2AuthenticationToken authentication) {

        if (authentication != null && authentication.isAuthenticated()) {
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    authentication.getAuthorizedClientRegistrationId(),
                    authentication.getName()
            );

            if (client != null && client.getAccessToken() != null) {
                String accessToken = client.getAccessToken().getTokenValue();

                PagedResponse<ReminderDto> pageResponse = this.restClient.findAllSortedByDate(page, size);

                model.addAttribute("reminders", pageResponse.content());
                model.addAttribute("currentPage", pageResponse.number());
                model.addAttribute("totalPages", pageResponse.totalPages());
                model.addAttribute("size", pageResponse.size());
                model.addAttribute("sort", "bydate");
                model.addAttribute("auth", authentication);
            }
        }

        System.out.println("MAIN");

        return "main";
    }

    @GetMapping("/create")
    public String getCreateNewReminderPage() {
        return "new-reminder-page";
    }

    @PostMapping("/create")
    public String createNewReminder(@ModelAttribute NewReminderForm form, Model model) {

        // простая серверная проверка
        if (form.remind() == null) {
            // имитируем структуру, которую уже рендеришь как errors['remind']
            Map<String, List<String>> errors = new HashMap<>();
            errors.put("remind", List.of("Remind is required"));

            model.addAttribute("errors", errors);
            model.addAttribute("errorMessage", "Invalid input data");

            // важно: вернуть ту же форму и передать обратно введённые значения
            model.addAttribute("title", form.title());
            model.addAttribute("description", form.description());
            model.addAttribute("remind", null);
            return "new-reminder-page";
        }

        Instant remindInstant = form.remind()
                .atZone(ZoneId.systemDefault())
                .toInstant();

        NewReminderPayload payload = new NewReminderPayload(form.title(), form.description(), remindInstant);

        Result<ReminderDto> result = restClient.createReminder(payload);
        if (result.isSuccess()) {
            return "redirect:/client/reminder/main/bytitle";
        } else {
            log.warn("Validation failed: {}", result.error());
            model.addAttribute("errors", result.error().details());   // ошибки от REST (валидация Bean Validation)
            model.addAttribute("errorMessage", result.error().message());
            // вернуть форму с теми же значениями
            model.addAttribute("title", form.title());
            model.addAttribute("description", form.description());
            model.addAttribute("remind", form.remind());
            return "new-reminder-page";
        }
    }
    
    @GetMapping("/main/search")
    public String searchRemindersByTitle(@RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "title,asc") String sort,
            Model model,
            Authentication authentication) {

        PagedResponse<ReminderDto> pageResponse = this.restClient.findRemindersByTitle(title, page, size, sort);

        model.addAttribute("reminders", pageResponse.content());
        model.addAttribute("currentPage", pageResponse.number());
        model.addAttribute("totalPages", pageResponse.totalPages());
        model.addAttribute("size", pageResponse.size());
        model.addAttribute("title", title);
        model.addAttribute("auth", authentication);

        return "main"; // можно сделать отдельный шаблон search.html, но можно использовать main
    }

    @GetMapping("/main/search-by-date")
    public String getRemindersByDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "remind,asc") String sort,
            Model model,
            Authentication authentication) {

        PagedResponse<ReminderDto> pageResponse = this.restClient.findRemindersByDate(date, page, size, sort);

        model.addAttribute("reminders", pageResponse.content());
        model.addAttribute("currentPage", pageResponse.number());
        model.addAttribute("totalPages", pageResponse.totalPages());
        model.addAttribute("size", pageResponse.size());
        model.addAttribute("date", date);
        model.addAttribute("auth", authentication);

        return "main";
    }

    @GetMapping("/main/searchbydaterange")
    public String searchRemindersByDateRange(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "remind,asc") String sort,
            Model model,
            Authentication authentication) {

        // Конвертируем LocalDate → Instant (UTC границы дня)
        Instant start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        PagedResponse<ReminderDto> pageResponse = this.restClient.findRemindersByDateRange(start, end, page, size, sort);

        model.addAttribute("reminders", pageResponse.content());
        model.addAttribute("currentPage", pageResponse.number());
        model.addAttribute("totalPages", pageResponse.totalPages());
        model.addAttribute("size", pageResponse.size());
        model.addAttribute("auth", authentication);

        // возвращаем обратно выбранные даты, чтобы форма показывала их
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "main";
    }
    
    
    
}
