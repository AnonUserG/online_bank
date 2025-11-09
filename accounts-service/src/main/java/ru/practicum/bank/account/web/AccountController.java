package ru.practicum.bank.account.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.bank.account.service.AccountService;
import ru.practicum.bank.account.web.dto.AccountDto;
import ru.practicum.bank.account.web.dto.ChangePasswordRequest;
import ru.practicum.bank.account.web.dto.RegisterAccountRequest;
import ru.practicum.bank.account.web.dto.UpdateAccountRequest;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/users")
    public List<AccountDto> listUsers() {
        return accountService.getAll();
    }

    @GetMapping("/users/{login}")
    public AccountDto userProfile(@PathVariable String login) {
        return accountService.getProfile(login);
    }

    @PatchMapping("/users/{login}")
    public ResponseEntity<List<String>> updateProfile(@PathVariable String login,
                                                      @RequestBody @Valid UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateProfile(login, request));
    }

    @PostMapping("/users/{login}/password")
    public ResponseEntity<List<String>> changePassword(@PathVariable String login,
                                                       @RequestBody @Valid ChangePasswordRequest request) {
        return ResponseEntity.ok(accountService.changePassword(login, request));
    }

    @PostMapping("/register")
    public ResponseEntity<List<String>> register(@RequestBody @Valid RegisterAccountRequest request) {
        return ResponseEntity.ok(accountService.register(request));
    }
}

