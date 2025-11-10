package ru.practicum.bank.account.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import ru.practicum.bank.account.model.AccountEntity;
import ru.practicum.bank.account.model.BankAccountEntity;
import ru.practicum.bank.account.repository.AccountRepository;
import ru.practicum.bank.clients.KeycloakAdminClient;
import ru.practicum.bank.clients.NotificationsClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AccountControllerIT {

    private static final AtomicLong ACCOUNT_SEQUENCE = new AtomicLong(1);

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bank")
                    .withUsername("bank")
                    .withPassword("bank");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    KeycloakAdminClient keycloakAdminClient;

    @MockBean
    NotificationsClient notificationsClient;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    void registerRejectsUnderageUser() throws Exception {
        LocalDate birthdate = LocalDate.now().minusYears(16);
        Map<String, Object> payload = Map.of(
                "login", "teenager",
                "password", "pwd12345",
                "name", "Teen User",
                "email", "teen@example.com",
                "birthdate", birthdate.toString()
        );

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Возраст должен быть не меньше 18 лет")));
    }

    @Test
    void deleteAccountFailsWhenBalancePositive() throws Exception {
        persistAccount("rich-user", BigDecimal.valueOf(500));

        mockMvc.perform(delete("/api/accounts/users/{login}", "rich-user"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Нельзя удалить аккаунт")));

        verify(keycloakAdminClient, never()).deleteUser("rich-user");
    }

    @Test
    void deleteAccountRemovesAccountWhenBalanceZero() throws Exception {
        persistAccount("empty-user", BigDecimal.ZERO);

        mockMvc.perform(delete("/api/accounts/users/{login}", "empty-user"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[]")));

        assertThat(accountRepository.findByLogin("empty-user")).isEmpty();
        verify(keycloakAdminClient).deleteUser("empty-user");
        verify(notificationsClient).sendAccountDeleted("empty-user");
    }

    private void persistAccount(String login, BigDecimal balance) {
        AccountEntity entity = new AccountEntity();
        entity.setLogin(login);
        entity.setName("Test User");
        entity.setBirthdate(LocalDate.of(1990, 1, 1));
        entity.setEmail("test@example.com");

        BankAccountEntity bank = new BankAccountEntity();
        bank.setAccountNumber(generateAccountNumber());
        bank.setCurrency("RUB");
        bank.setBalance(balance);

        entity.setBankAccount(bank);
        accountRepository.save(entity);
    }

    private String generateAccountNumber() {
        long seq = ACCOUNT_SEQUENCE.getAndIncrement();
        return String.format("4080%016d", seq);
    }
}
