package ru.practicum.accounts.account.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.accounts.account.model.AccountEntity;

/**
 * Repository for user profiles.
 */
public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

    @EntityGraph(attributePaths = "bankAccount")
    Optional<AccountEntity> findByLogin(String login);

    boolean existsByLogin(String login);

    @Override
    @EntityGraph(attributePaths = "bankAccount")
    List<AccountEntity> findAll();
}

