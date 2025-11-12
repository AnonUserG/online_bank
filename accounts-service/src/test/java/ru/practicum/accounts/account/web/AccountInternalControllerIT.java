package ru.practicum.accounts.account.web;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.accounts.account.model.AccountEntity;
import ru.practicum.accounts.account.model.BankAccountEntity;
import ru.practicum.accounts.account.repository.AccountRepository;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AccountInternalControllerIT {

    private static final AtomicInteger ACCOUNT_SEQUENCE = new AtomicInteger(1);

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

    @BeforeEach
    void clean() {
        accountRepository.deleteAll();
    }

    @Test
    void getDetailsReturnsPersistedAccount() throws Exception {
        AccountEntity entity = persistAccount("bob", new BigDecimal("250.00"));

        mockMvc.perform(get("/api/accounts/internal/users/{login}", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login", is("bob")))
                .andExpect(jsonPath("$.accountNumber", equalTo(entity.getBankAccount().getAccountNumber())))
                .andExpect(jsonPath("$.balance", is(250.0)));
    }

    @Test
    void adjustBalanceDepositsFunds() throws Exception {
        persistAccount("alice", new BigDecimal("100.00"));

        mockMvc.perform(post("/api/accounts/internal/users/{login}/balance", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 75,
                                "type", "DEPOSIT"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(175.0)));
    }

    private AccountEntity persistAccount(String login, BigDecimal balance) {
        AccountEntity user = new AccountEntity();
        user.setLogin(login);
        user.setName("Test " + login);
        user.setBirthdate(LocalDate.of(1990, 1, 1));

        BankAccountEntity bank = new BankAccountEntity();
        bank.setAccountNumber(nextAccountNumber());
        bank.setCurrency("RUB");
        bank.setBalance(balance);
        user.setBankAccount(bank);
        return accountRepository.save(user);
    }

    private String nextAccountNumber() {
        return String.format("4080%016d", ACCOUNT_SEQUENCE.getAndIncrement());
    }
}
