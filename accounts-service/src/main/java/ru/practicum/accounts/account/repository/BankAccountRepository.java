package ru.practicum.accounts.account.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.accounts.account.model.BankAccountEntity;

/**
 * Repository for bank accounts with locking helpers.
 */
public interface BankAccountRepository extends JpaRepository<BankAccountEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BankAccountEntity b join fetch b.user u where u.login = :login")
    Optional<BankAccountEntity> findByUserLoginForUpdate(@Param("login") String login);
}

