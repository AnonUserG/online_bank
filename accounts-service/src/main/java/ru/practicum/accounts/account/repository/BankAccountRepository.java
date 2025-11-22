package ru.practicum.accounts.account.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
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
    Optional<BankAccountEntity> findFirstByUser_LoginOrderByCreatedAtAsc(String login);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BankAccountEntity> findById(UUID id);

    List<BankAccountEntity> findAllByUser_Login(String login);

    /**
     * Kept for backward compatibility, delegates to top-by-login selection.
     */
    @Deprecated
    default Optional<BankAccountEntity> findByUserLoginForUpdate(String login) {
        return findFirstByUser_LoginOrderByCreatedAtAsc(login);
    }
}
