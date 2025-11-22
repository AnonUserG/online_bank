-- init-db/01-init-schemas.sql

-- ==========================================
-- 0) Чистый старт (dev): дропаем схемы
-- ==========================================
DROP SCHEMA IF EXISTS accounts      CASCADE;
DROP SCHEMA IF EXISTS cash          CASCADE;
DROP SCHEMA IF EXISTS transfer      CASCADE;
DROP SCHEMA IF EXISTS exchange      CASCADE;
DROP SCHEMA IF EXISTS blocker       CASCADE;
DROP SCHEMA IF EXISTS notifications CASCADE;

-- UUID генератор для gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==========================================
-- 1) Схемы
-- ==========================================
CREATE SCHEMA IF NOT EXISTS accounts;
CREATE SCHEMA IF NOT EXISTS cash;
CREATE SCHEMA IF NOT EXISTS transfer;
CREATE SCHEMA IF NOT EXISTS exchange;
CREATE SCHEMA IF NOT EXISTS blocker;
CREATE SCHEMA IF NOT EXISTS notifications;

-- ==========================================
-- 2) ACCOUNTS
-- ==========================================
-- Важно: UUID без DEFAULT, т.к. генерирует приложение (Hibernate uuid2)
CREATE TABLE accounts.users (
                                id         UUID PRIMARY KEY,
                                login      VARCHAR(50)  NOT NULL UNIQUE,      -- для UI и поиска
                                name       VARCHAR(255) NOT NULL,             -- "Фамилия Имя" для UI
                                email      VARCHAR(100),
                                birthdate  DATE         NOT NULL,
                                kc_id      VARCHAR(255),                      -- sub/id в Keycloak (опц.)
                                active     BOOLEAN      NOT NULL DEFAULT TRUE,
                                created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE accounts.bank_accounts (
                                        id              UUID PRIMARY KEY,
                                        user_id         UUID        NOT NULL,
                                        account_number  VARCHAR(20) NOT NULL UNIQUE,
                                        currency        VARCHAR(3)  NOT NULL DEFAULT 'RUB',
                                        balance         NUMERIC(19,4) NOT NULL DEFAULT 0,
                                        status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE|BLOCKED|CLOSED
                                        version         BIGINT      NOT NULL DEFAULT 0,        -- оптимистическая блокировка
                                        created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                        updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                        CONSTRAINT fk_bank_accounts_user FOREIGN KEY (user_id)
                                        REFERENCES accounts.users(id) ON DELETE CASCADE,
                                        CONSTRAINT uq_bank_accounts_user_currency UNIQUE (user_id, currency),
                                        CONSTRAINT chk_balance_nonneg CHECK (balance >= 0),
                                        CONSTRAINT chk_currency3 CHECK (char_length(currency) = 3)
);

CREATE INDEX idx_bank_accounts_user_id ON accounts.bank_accounts(user_id);

-- ==========================================
-- 3) EXCHANGE
-- ==========================================
CREATE TABLE exchange.currency_rates (
                                         id               BIGSERIAL PRIMARY KEY,
                                         base_currency    VARCHAR(3)   NOT NULL,
                                         target_currency  VARCHAR(3)   NOT NULL,
                                         rate             NUMERIC(19,6) NOT NULL,
                                         created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                         CONSTRAINT uq_currency_pair      UNIQUE (base_currency, target_currency),
                                         CONSTRAINT chk_base_cur3         CHECK (char_length(base_currency) = 3),
                                         CONSTRAINT chk_target_cur3       CHECK (char_length(target_currency) = 3),
                                         CONSTRAINT chk_rate_positive     CHECK (rate > 0)
);

-- ==========================================
-- 4) TRANSFER
-- ==========================================
CREATE TABLE transfer.transactions (
                                       id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       from_account_id  UUID        NOT NULL,  -- без FK на другой сервис
                                       to_account_id    UUID        NOT NULL,
                                       amount           NUMERIC(19,4) NOT NULL,
                                       currency         VARCHAR(3)  NOT NULL,
                                       status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING|DONE|FAILED
                                       idempotency_key  VARCHAR(100) NOT NULL,
                                       created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       CONSTRAINT chk_transfer_amount    CHECK (amount > 0),
                                       CONSTRAINT chk_transfer_currency3 CHECK (char_length(currency) = 3),
                                       CONSTRAINT uq_transfer_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_transactions_from_account ON transfer.transactions(from_account_id);
CREATE INDEX idx_transactions_to_account   ON transfer.transactions(to_account_id);

-- ==========================================
-- 5) CASH
-- ==========================================
CREATE TABLE cash.operations (
                                 id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 account_id       UUID        NOT NULL,
                                 operation_type   VARCHAR(20) NOT NULL,  -- DEPOSIT|WITHDRAW
                                 amount           NUMERIC(19,4) NOT NULL,
                                 currency         VARCHAR(3)  NOT NULL,
                                 status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING|DONE|FAILED
                                 idempotency_key  VARCHAR(100) NOT NULL,
                                 created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 CONSTRAINT chk_cash_amount     CHECK (amount > 0),
                                 CONSTRAINT chk_cash_currency3  CHECK (char_length(currency) = 3),
                                 CONSTRAINT uq_cash_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_operations_account ON cash.operations(account_id);

-- ==========================================
-- 6) BLOCKER
-- ==========================================
CREATE TABLE blocker.suspicious_operations (
                                               id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                               operation_id    UUID        NOT NULL,        -- ID из cash/transfer
                                               operation_type  VARCHAR(50) NOT NULL,        -- CASH|TRANSFER|...
                                               reason          VARCHAR(255) NOT NULL,
                                               status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',  -- OPEN|REVIEWED|BLOCKED|RELEASED
                                               created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ==========================================
-- 7) NOTIFICATIONS
-- ==========================================
CREATE TABLE notifications.messages (
                                        id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        user_id     UUID        NOT NULL,         -- без FK на accounts (граница микросервиса)
                                        type        VARCHAR(50) NOT NULL,         -- CASH_IN|CASH_OUT|TRANSFER_IN|TRANSFER_OUT|...
                                        title       VARCHAR(255) NOT NULL,
                                        content     TEXT        NOT NULL,
                                        status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING|SENT|FAILED
                                        created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                        sent_at     TIMESTAMPTZ
);

CREATE INDEX idx_messages_user ON notifications.messages(user_id);

-- ==========================================
-- 8) Начальные курсы валют
-- ==========================================
INSERT INTO exchange.currency_rates (base_currency, target_currency, rate) VALUES
                                                                               ('RUB','RUB',1.000000),
                                                                               ('RUB','USD',0.011000),
                                                                               ('RUB','CNY',0.079000),
                                                                               ('USD','RUB',90.910000),
                                                                               ('USD','CNY',7.180000),
                                                                               ('CNY','RUB',12.660000),
                                                                               ('CNY','USD',0.139000)
ON CONFLICT (base_currency, target_currency) DO NOTHING;

-- ==========================================
-- 9) демо-пользователи alice/bob (Keycloak)
-- ==========================================
WITH upsert_bob AS (
    INSERT INTO accounts.users (id, login, name, email, birthdate, kc_id, active, created_at, updated_at)
    VALUES (gen_random_uuid(), 'bob', 'Bob Example', 'bob@example.com', DATE '1990-01-10', NULL, TRUE, now(), now())
    ON CONFLICT (login) DO UPDATE
        SET name = EXCLUDED.name,
            email = EXCLUDED.email,
            birthdate = EXCLUDED.birthdate,
            active = TRUE,
            updated_at = now()
    RETURNING id
)
INSERT INTO accounts.bank_accounts (id, user_id, account_number, currency, balance, status, version, created_at, updated_at)
SELECT gen_random_uuid(), id, '40800000000000000001', 'RUB', 1500.00, 'ACTIVE', 0, now(), now()
FROM upsert_bob
ON CONFLICT (user_id, currency) DO UPDATE
    SET balance = EXCLUDED.balance,
        status = EXCLUDED.status,
        updated_at = now();

WITH upsert_alice AS (
    INSERT INTO accounts.users (id, login, name, email, birthdate, kc_id, active, created_at, updated_at)
    VALUES (gen_random_uuid(), 'alice', 'Alice Example', 'alice@example.com', DATE '1992-05-15', NULL, TRUE, now(), now())
    ON CONFLICT (login) DO UPDATE
        SET name = EXCLUDED.name,
            email = EXCLUDED.email,
            birthdate = EXCLUDED.birthdate,
            active = TRUE,
            updated_at = now()
    RETURNING id
)
INSERT INTO accounts.bank_accounts (id, user_id, account_number, currency, balance, status, version, created_at, updated_at)
SELECT gen_random_uuid(), id, '40800000000000000002', 'RUB', 750.00, 'ACTIVE', 0, now(), now()
FROM upsert_alice
ON CONFLICT (user_id, currency) DO UPDATE
    SET balance = EXCLUDED.balance,
        status = EXCLUDED.status,
        updated_at = now();

-- дополнительные счета в другой валюте
INSERT INTO accounts.bank_accounts (id, user_id, account_number, currency, balance, status, version, created_at, updated_at)
SELECT gen_random_uuid(), id, '40800000000000000003', 'USD', 300.00, 'ACTIVE', 0, now(), now()
FROM accounts.users WHERE login = 'bob'
ON CONFLICT (user_id, currency) DO NOTHING;

INSERT INTO accounts.bank_accounts (id, user_id, account_number, currency, balance, status, version, created_at, updated_at)
SELECT gen_random_uuid(), id, '40800000000000000004', 'CNY', 500.00, 'ACTIVE', 0, now(), now()
FROM accounts.users WHERE login = 'alice'
ON CONFLICT (user_id, currency) DO NOTHING;
