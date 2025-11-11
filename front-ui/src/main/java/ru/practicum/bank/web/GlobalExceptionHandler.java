package ru.practicum.bank.web;

import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    private static final String SESSION_EXPIRED_MESSAGE = "Сессия устарела, выполните вход повторно.";

    @ExceptionHandler({ClientAuthorizationException.class, OAuth2AuthorizationException.class})
    public String handleAuthorizationExceptions(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("globalError", SESSION_EXPIRED_MESSAGE);
        return "redirect:/logout";
    }
}
