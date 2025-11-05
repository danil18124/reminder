package com.example.reminder_client.controller;

import com.example.reminder_client.controller.payload.UpdateReminderForm;

import java.time.Instant;

import java.time.ZoneId;
import java.util.List;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.reminder_client.model.ReminderDto;
import com.example.reminder_client.service.RestClientReminderRestClient;
import com.example.reminder_client.service.dto.Result;
import com.example.reminder_client.service.payload.UpdateReminderPayload;

import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequestMapping("/client/reminder/{reminderId}")
@Slf4j
public class ReminderController {

    private final RestClientReminderRestClient restClient;
    private final OAuth2AuthorizedClientService authorizedClientService;


    @PostMapping("/edit")
    public String updateReminder(@PathVariable("reminderId") Long id, @ModelAttribute UpdateReminderForm form, Model model) {

        if (form.remind() == null) {
            return handleValidationError(id, form, model,
                    Map.of("remind", List.of("Remind is required")),
                    "Invalid input data",
                    "reminder-edit-page");
        }

        Instant remindInstant = form.remind()
                .atZone(ZoneId.systemDefault())
                .toInstant();

        UpdateReminderPayload payload = new UpdateReminderPayload(form.title(), form.description(), remindInstant);

        Result<ReminderDto> result = restClient.updateReminder(id, payload);

        if (result.isSuccess()) {
            return "redirect:/client/reminder/main/bytitle";
        } else {
            return handleValidationError(id, form, model,
                    result.error().details(),
                    result.error().message(),
                    "reminder-edit-page");
        }

    }

    private String handleValidationError(Long id,
            UpdateReminderForm form,
            Model model,
            Map<String, ?> rawErrors,
            String errorMessage,
            String viewName) {

        ReminderDto reminder = restClient.getReminderById(id);
        model.addAttribute("reminder", reminder);

        // приведение к Map<String, List<String>>
        Map<String, List<String>> errors = rawErrors.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() instanceof List
                        ? (List<String>) e.getValue()
                        : List.of(String.valueOf(e.getValue()))
                ));

        model.addAttribute("errors", errors);
        model.addAttribute("errorMessage", errorMessage);

        // сохраняем введённые пользователем данные
        model.addAttribute("title", form.title());
        model.addAttribute("description", form.description());
        model.addAttribute("remind", form.remind());

        return viewName;
    }

    @GetMapping("/edit")
    public String getReminderEditPage(@PathVariable("reminderId") Long id, Model model) {
        ReminderDto reminder = this.restClient.getReminderById(id);
        model.addAttribute("reminder", reminder);
        return "reminder-edit-page";
    }
    
    @PostMapping("/delete")
    public String deleteReminder(@PathVariable("reminderId") Long id) {
        this.restClient.deleteReminder(id);
        return "redirect:/client/reminder/main/bytitle";
    }

}
