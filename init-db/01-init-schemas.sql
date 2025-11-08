-- init-db/01-init-schemas.sql

-- UUID генерация
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Схемы per service
CREATE SCHEMA IF NOT EXISTS accounts;
CREATE SCHEMA IF NOT EXISTS cash;
CREATE SCHEMA IF NOT EXISTS transfer;
CREATE SCHEMA IF NOT EXISTS exchange;
CREATE SCHEMA IF NOT EXISTS blocker;
CREATE SCHEMA IF NOT EXISTS notifications;

--------------------------------------------------------------------------------
-- ACCOUNTS
--------------------------------------------------------------------------------
-- Пользователи (бизнес-профиль; пароль хранит Keycloak)
CREATE TABLE IF NOT EXISTS accounts.users (
                                              id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                              login       VARCHAR(50)  NOT NULL UNIQUE,         -- для UI и поиска
                                              name        VARCHAR(255) NOT NULL,                -- "Фамилия Имя" для UI
                                              email       VARCHAR(100) UNIQUE,
                                              birthdate   DATE         NOT NULL,
                                              kc_id       VARCHAR(255),                         -- sub/id пользователя в Keycloak (опционально)
                                              active      BOOLEAN      NOT NULL DEFAULT TRUE,
                                              created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                              updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Счёт пользователя (по умолчанию 1:1 с users)
CREATE TABLE IF NOT EXISTS accounts.bank_accounts (
                                                      id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                      user_id         UUID        NOT NULL,
                                                      account_number  VARCHAR(20) NOT NULL UNIQUE,
                                                      currency        VARCHAR(3)  NOT NULL,
                                                      balance         NUMERIC(19,4) NOT NULL DEFAULT 0,
                                                      status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE|BLOCKED|CLOSED (произвольный справочник)
                                                      version         BIGINT      NOT NULL DEFAULT 0,         -- оптимистическая блокировка
                                                      created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                      updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                      CONSTRAINT fk_bank_accounts_user FOREIGN KEY (user_id)
                                                          REFERENCES accounts.users(id) ON DELETE CASCADE,
                                                      CONSTRAINT uq_bank_accounts_user UNIQUE (user_id),
                                                      CONSTRAINT chk_balance_nonneg CHECK (balance >= 0),
                                                      CONSTRAINT chk_currency3 CHECK (char_length(currency) = 3)
);

CREATE INDEX IF NOT EXISTS idx_bank_accounts_user_id ON accounts.bank_accounts(user_id);

--------------------------------------------------------------------------------
-- EXCHANGE
--------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exchange.currency_rates (
                                                       id              BIGSERIAL PRIMARY KEY,
                                                       base_currency   VARCHAR(3)  NOT NULL,
                                                       target_currency VARCHAR(3)  NOT NULL,
                                                       rate            NUMERIC(19,6) NOT NULL,
                                                       created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                       CONSTRAINT uq_currency_pair UNIQUE (base_currency, target_currency),
                                                       CONSTRAINT chk_base_cur3 CHECK (char_length(base_currency) = 3),
                                                       CONSTRAINT chk_target_cur3 CHECK (char_length(target_currency) = 3),
                                                       CONSTRAINT chk_rate_positive CHECK (rate > 0)
);

--------------------------------------------------------------------------------
-- TRANSFER
--------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transfer.transactions (
                                                     id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                     from_account_id  UUID        NOT NULL,           -- без внешних ключей на другой сервис
                                                     to_account_id    UUID        NOT NULL,
                                                     amount           NUMERIC(19,4) NOT NULL,
                                                     currency         VARCHAR(3)  NOT NULL,
                                                     status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',   -- PENDING|DONE|FAILED
                                                     idempotency_key  VARCHAR(100) NOT NULL,
                                                     created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                     updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                     CONSTRAINT chk_transfer_amount CHECK (amount > 0),
                                                     CONSTRAINT chk_transfer_currency3 CHECK (char_length(currency) = 3),
                                                     CONSTRAINT uq_transfer_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_transactions_from_account ON transfer.transactions(from_account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_to_account   ON transfer.transactions(to_account_id);

--------------------------------------------------------------------------------
-- CASH
--------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cash.operations (
                                               id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                               account_id       UUID        NOT NULL,
                                               operation_type   VARCHAR(20) NOT NULL,           -- DEPOSIT|WITHDRAW
                                               amount           NUMERIC(19,4) NOT NULL,
                                               currency         VARCHAR(3)  NOT NULL,
                                               status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',   -- PENDING|DONE|FAILED
                                               idempotency_key  VARCHAR(100) NOT NULL,
                                               created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                                               CONSTRAINT chk_cash_amount CHECK (amount > 0),
                                               CONSTRAINT chk_cash_currency3 CHECK (char_length(currency) = 3),
                                               CONSTRAINT uq_cash_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_operations_account ON cash.operations(account_id);

--------------------------------------------------------------------------------
-- BLOCKER
--------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blocker.suspicious_operations (
                                                             id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                             operation_id    UUID        NOT NULL,            -- ID из cash/transfer
                                                             operation_type  VARCHAR(50) NOT NULL,            -- CASH|TRANSFER|...
                                                             reason          VARCHAR(255) NOT NULL,
                                                             status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',   -- OPEN|REVIEWED|BLOCKED|RELEASED
                                                             created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

--------------------------------------------------------------------------------
-- NOTIFICATIONS
--------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications.messages (
                                                      id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                      user_id     UUID        NOT NULL,                -- без FK на accounts (граница микросервиса)
                                                      type        VARCHAR(50) NOT NULL,                -- CASH_IN|CASH_OUT|TRANSFER_IN|TRANSFER_OUT|...
                                                      title       VARCHAR(255) NOT NULL,
                                                      content     TEXT        NOT NULL,
                                                      status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING|SENT|FAILED
                                                      created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                      sent_at     TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_messages_user ON notifications.messages(user_id);

--------------------------------------------------------------------------------
-- Начальные курсы валют
--------------------------------------------------------------------------------
INSERT INTO exchange.currency_rates (base_currency, target_currency, rate) VALUES
                                                                               ('RUB','RUB',1.000000),
                                                                               ('RUB','USD',0.011000),
                                                                               ('RUB','CNY',0.079000),
                                                                               ('USD','RUB',90.910000),
                                                                               ('USD','CNY',7.180000),
                                                                               ('CNY','RUB',12.660000),
                                                                               ('CNY','USD',0.139000)
ON CONFLICT (base_currency, target_currency) DO NOTHING;
