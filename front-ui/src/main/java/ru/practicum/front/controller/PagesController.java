package ru.practicum.front.controller;

import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.practicum.front.service.GatewayApiClient;
import ru.practicum.front.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Controller
public class PagesController {

    private final GatewayApiClient api;

    public PagesController(GatewayApiClient api) {
        this.api = api;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/main";
    }

    @GetMapping("/main")
    public String mainPage(Model model,
                           @AuthenticationPrincipal OidcUser oidcUser,
                           @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                           @ModelAttribute("passwordErrors") List<String> passwordErrors,
                           @ModelAttribute("userAccountErrors") List<String> userAccountErrors,
                           @ModelAttribute("cashErrors") List<String> cashErrors,
                           @ModelAttribute("transferOtherErrors") List<String> transferOtherErrors) {

        // Авторизованный пользователь обязателен
        String bearer = client.getAccessToken().getTokenValue();
        String login = Optional.ofNullable(oidcUser.getPreferredUsername())
                .orElse(oidcUser.getName()); // fallback

        // Профиль пользователя (login, name, birthdate)
        Map<String, Object> profile = api.getUserProfile(login, bearer);
        String name = Objects.toString(profile.getOrDefault("name", ""), "");
        LocalDate birthdate = Optional.ofNullable(profile.get("birthdate"))
                .map(Object::toString).map(LocalDate::parse).orElse(null);

        // Список всех пользователей для селекта перевода
        List<Map<String, Object>> usersRaw = api.getAllUsers(bearer);
        // Оставим как есть — шаблон ожидает user.getLogin() / user.getName()
        // Для простоты положим Map'ы напрямую
        model.addAttribute("users", usersRaw);

        model.addAttribute("login", login);
        model.addAttribute("name", name);
        model.addAttribute("birthdate", birthdate);

        // Ошибки (null, если не выполнялись операции)
        model.addAttribute("passwordErrors", (passwordErrors == null || passwordErrors.isEmpty()) ? null : passwordErrors);
        model.addAttribute("userAccountErrors", (userAccountErrors == null || userAccountErrors.isEmpty()) ? null : userAccountErrors);
        model.addAttribute("cashErrors", (cashErrors == null || cashErrors.isEmpty()) ? null : cashErrors);
        model.addAttribute("transferOtherErrors", (transferOtherErrors == null || transferOtherErrors.isEmpty()) ? null : transferOtherErrors);

        return "main";
    }

    // =================== Смена пароля ===================

    @PostMapping("/user/{login}/editPassword")
    public String editPassword(@PathVariable("login") String pathLogin,
                               @RequestParam("password") String password,
                               @RequestParam("confirm_password") String confirm,
                               @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                               RedirectAttributes ra,
                               @AuthenticationPrincipal OidcUser oidcUser) {

        String login = oidcUser.getPreferredUsername();
        if (!Objects.equals(login, pathLogin)) {
            ra.addFlashAttribute("passwordErrors", List.of("Нельзя менять пароль другого пользователя"));
            return "redirect:/main";
        }

        List<String> errors = ValidationUtils.validatePasswordChange(password, confirm);
        if (!errors.isEmpty()) {
            ra.addFlashAttribute("passwordErrors", errors);
            return "redirect:/main";
        }

        String bearer = client.getAccessToken().getTokenValue();
        List<String> backendErrors = safeList(api.changePassword(login, password, bearer));
        ra.addFlashAttribute("passwordErrors", backendErrors.isEmpty() ? null : backendErrors);
        return "redirect:/main";
    }

    // =================== Редактирование профиля ===================

    @PostMapping("/user/{login}/editUserAccount")
    public String editUserAccount(@PathVariable("login") String pathLogin,
                                  @RequestParam("name") @NotBlank String name,
                                  @RequestParam(value = "birthdate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthdate,
                                  @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                                  RedirectAttributes ra,
                                  @AuthenticationPrincipal OidcUser oidcUser) {

        String login = oidcUser.getPreferredUsername();
        if (!Objects.equals(login, pathLogin)) {
            ra.addFlashAttribute("userAccountErrors", List.of("Нельзя изменять чужой профиль"));
            return "redirect:/main";
        }

        List<String> errors = ValidationUtils.validateProfile(name, birthdate);
        if (!errors.isEmpty()) {
            ra.addFlashAttribute("userAccountErrors", errors);
            return "redirect:/main";
        }

        String bearer = client.getAccessToken().getTokenValue();
        List<String> backendErrors = safeList(api.updateProfile(login, name, birthdate.toString(), bearer));
        ra.addFlashAttribute("userAccountErrors", backendErrors.isEmpty() ? null : backendErrors);
        return "redirect:/main";
    }

    // =================== Внесение/снятие денег ===================

    @PostMapping("/user/{login}/cash")
    public String cash(@PathVariable("login") String pathLogin,
                       @RequestParam("value") String value,
                       @RequestParam("action") String action,
                       @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                       RedirectAttributes ra,
                       @AuthenticationPrincipal OidcUser oidcUser) {

        String login = oidcUser.getPreferredUsername();
        if (!Objects.equals(login, pathLogin)) {
            ra.addFlashAttribute("cashErrors", List.of("Нельзя выполнять операции по чужому счёту"));
            return "redirect:/main";
        }

        String bearer = client.getAccessToken().getTokenValue();
        List<String> backendErrors = safeList(api.cash(login, action, value, bearer));
        ra.addFlashAttribute("cashErrors", backendErrors.isEmpty() ? null : backendErrors);
        return "redirect:/main";
    }

    // =================== Перевод ===================

    @PostMapping("/user/{login}/transfer")
    public String transfer(@PathVariable("login") String fromLogin,
                           @RequestParam("value") String value,
                           @RequestParam("to_login") String toLogin,
                           @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                           RedirectAttributes ra,
                           @AuthenticationPrincipal OidcUser oidcUser) {

        String login = oidcUser.getPreferredUsername();
        if (!Objects.equals(login, fromLogin)) {
            ra.addFlashAttribute("transferOtherErrors", List.of("Нельзя переводить с чужого счёта"));
            return "redirect:/main";
        }

        List<String> validationErrors = ValidationUtils.validateTransfer(toLogin, value);
        if (!validationErrors.isEmpty()) {
            ra.addFlashAttribute("transferOtherErrors", validationErrors);
            return "redirect:/main";
        }

        String bearer = client.getAccessToken().getTokenValue();
        String normalizedValue = new BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP).toPlainString();
        List<String> backendErrors = safeList(api.transfer(fromLogin, toLogin, normalizedValue, bearer));
        ra.addFlashAttribute("transferOtherErrors", backendErrors.isEmpty() ? null : backendErrors);
        return "redirect:/main";
    }

    // =================== Регистрация ===================

    @GetMapping("/signup")
    public String signupForm() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam("login") String login,
                         @RequestParam("password") String password,
                         @RequestParam("confirm_password") String confirm,
                         @RequestParam("name") String name,
                         @RequestParam("birthdate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthdate,
                         RedirectAttributes ra,
                         Model model) {

        var errors = ValidationUtils.validateSignup(login, password, confirm, name, birthdate);
        if (!errors.isEmpty()) {
            model.addAttribute("login", login);
            model.addAttribute("name", name);
            model.addAttribute("birthdate", birthdate);
            model.addAttribute("errors", errors);
            return "signup";
        }

        // Регистрация: открытый эндпоинт Accounts через gateway
        List<String> backendErrors = safeList(api.register(login, password, name, birthdate.toString()));
        if (!backendErrors.isEmpty()) {
            model.addAttribute("login", login);
            model.addAttribute("name", name);
            model.addAttribute("birthdate", birthdate);
            model.addAttribute("errors", backendErrors);
            return "signup";
        }

        // По ТЗ — автоавторизация. Практический компромисс: редиректим на начало OIDC-логина.
        return "redirect:/oauth2/authorization/keycloak";
    }

    // ----------------- helpers -----------------
    @ModelAttribute("passwordErrors")
    public List<String> passwordErrors() { return null; }
    @ModelAttribute("userAccountErrors")
    public List<String> userAccountErrors() { return null; }
    @ModelAttribute("cashErrors")
    public List<String> cashErrors() { return null; }
    @ModelAttribute("transferOtherErrors")
    public List<String> transferOtherErrors() { return null; }

    @SuppressWarnings("unchecked")
    private static List<String> safeList(Object maybeList) {
        if (maybeList == null) return List.of();
        if (maybeList instanceof List<?> l) {
            // Преобразуем элементы в строки для шаблона
            return l.stream().map(String::valueOf).toList();
        }
        if (maybeList instanceof Map<?,?> m && m.containsKey("errors")) {
            Object val = m.get("errors");
            if (val instanceof List<?> l2) return l2.stream().map(String::valueOf).toList();
            return List.of(String.valueOf(val));
        }
        if (maybeList instanceof String s) return List.of(s);
        return List.of(String.valueOf(maybeList));
    }
}
