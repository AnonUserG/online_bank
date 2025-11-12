package ru.practicum.accounts.account.web;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.accounts.account.service.AccountService;
import ru.practicum.accounts.account.web.dto.AccountDetailsDto;
import ru.practicum.accounts.account.web.dto.BalanceAdjustmentRequest;

@RestController
@RequestMapping("/api/accounts/internal/users")
public class AccountInternalController {

    private final AccountService accountService;

    public AccountInternalController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{login}")
    public AccountDetailsDto getDetails(@PathVariable String login) {
        return accountService.getAccountDetails(login);
    }

    @PostMapping("/{login}/balance")
    public AccountDetailsDto adjustBalance(@PathVariable String login,
                                           @RequestBody @Valid BalanceAdjustmentRequest request) {
        return accountService.adjustBalance(login, request);
    }
}
