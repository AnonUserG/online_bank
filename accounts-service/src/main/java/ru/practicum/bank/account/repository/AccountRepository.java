package ru.practicum.bank.account.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.bank.account.model.AccountEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

    @EntityGraph(attributePaths = "bankAccount")
    Optional<AccountEntity> findByLogin(String login);

    boolean existsByLogin(String login);

    @Override
    @EntityGraph(attributePaths = "bankAccount")
    List<AccountEntity> findAll();
}

