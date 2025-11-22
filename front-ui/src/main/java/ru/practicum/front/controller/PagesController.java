package ru.practicum.front.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.practicum.front.mapper.AccountMapper;
import ru.practicum.front.service.Dto;
import ru.practicum.front.service.GatewayApiClient;
import ru.practicum.front.service.dto.AccountResponse;
import ru.practicum.front.service.dto.RateResponse;
import ru.practicum.front.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * MVC-контроллер пользовательских страниц.
 */
@Controller
@RequiredArgsConstructor
public class PagesController {

    private final GatewayApiClient api;
    private final AccountMapper accountMapper;

    @ModelAttribute("passwordErrors")
    public List<String> passwordErrors() {
        return List.of();
    }

    @ModelAttribute("userAccountErrors")
    public List<String> userAccountErrors() {
        return List.of();
    }

    @ModelAttribute("cashErrors")
    public List<String> cashErrors() {
        return List.of();
    }

    @ModelAttribute("transferOtherErrors")
    public List<String> transferOtherErrors() {
        return List.of();
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

        String bearer = client.getAccessToken().getTokenValue();
        String login = Optional.ofNullable(oidcUser.getPreferredUsername())
                .orElse(oidcUser.getName());

        AccountResponse profileResponse = api.getUserProfile(login, bearer);
        Dto.UserProfile profile = accountMapper.toUserProfile(profileResponse);

        List<Dto.UserShort> users = api.getAllUsers(bearer).stream()
                .map(accountMapper::toUserShort)
                .toList();

        List<RateResponse> rates = api.getRates(bearer);

        model.addAttribute("users", users);
        model.addAttribute("login", profile.login());
        model.addAttribute("name", profile.name());
        model.addAttribute("birthdate", profile.birthdate());
        model.addAttribute("rates", rates);
        model.addAttribute("passwordErrors", normalizeErrors(passwordErrors));
        model.addAttribute("userAccountErrors", normalizeErrors(userAccountErrors));
        model.addAttribute("cashErrors", normalizeErrors(cashErrors));
        model.addAttribute("transferOtherErrors", normalizeErrors(transferOtherErrors));
        return "main";
    }

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
        ra.addFlashAttribute("passwordErrors", normalizeErrors(backendErrors));
        return "redirect:/main";
    }

    @PostMapping("/user/{login}/editUserAccount")
    public String editUserAccount(@PathVariable("login") String pathLogin,
                                  @RequestParam("name") @NotBlank String name,
                                  @RequestParam(value = "birthdate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthdate,
                                  @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                                  RedirectAttributes ra,
                                  @AuthenticationPrincipal OidcUser oidcUser) {

        String login = oidcUser.getPreferredUsername();
        if (!Objects.equals(login, pathLogin)) {
            ra.addFlashAttribute("userAccountErrors", List.of("Нельзя редактировать чужой профиль"));
            return "redirect:/main";
        }

        List<String> errors = ValidationUtils.validateProfile(name, birthdate);
        if (!errors.isEmpty()) {
            ra.addFlashAttribute("userAccountErrors", errors);
            return "redirect:/main";
        }

        String bearer = client.getAccessToken().getTokenValue();
        List<String> backendErrors = safeList(api.updateProfile(login, name, birthdate.toString(), bearer));
        ra.addFlashAttribute("userAccountErrors", normalizeErrors(backendErrors));
        return "redirect:/main";
    }

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
        ra.addFlashAttribute("cashErrors", normalizeErrors(backendErrors));
        return "redirect:/main";
    }

    @PostMapping("/user/{login}/transfer")
    public String transfer(@PathVariable("login") String fromLogin,
                           @RequestParam("value") String value,
                           @RequestParam("to_login") String toLogin,
                           @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                           RedirectAttributes ra,
                           @AuthenticationPrincipal OidcUser oidcUser) {

        String login = oidcUser.getPreferredUsername();
        if (!Objects.equals(login, fromLogin)) {
            ra.addFlashAttribute("transferOtherErrors", List.of("Нельзя переводить деньги с чужого счёта"));
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
        ra.addFlashAttribute("transferOtherErrors", normalizeErrors(backendErrors));
        return "redirect:/main";
    }

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

        List<String> backendErrors = safeList(api.register(login, password, name, birthdate.toString()));
        if (!backendErrors.isEmpty()) {
            model.addAttribute("login", login);
            model.addAttribute("name", name);
            model.addAttribute("birthdate", birthdate);
            model.addAttribute("errors", backendErrors);
            return "signup";
        }

        ra.addFlashAttribute("globalMessage", "Регистрация успешно завершена. Авторизуйтесь снова.");
        return "redirect:/oauth2/authorization/keycloak";
    }

    private List<String> safeList(List<String> list) {
        return list == null ? List.of() : list;
    }

    private List<String> normalizeErrors(List<String> errors) {
        return (errors == null || errors.isEmpty()) ? null : errors;
    }
}
