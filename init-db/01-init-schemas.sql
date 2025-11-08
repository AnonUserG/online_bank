-- Create schemas for each service
CREATE SCHEMA IF NOT EXISTS accounts;
CREATE SCHEMA IF NOT EXISTS cash;
CREATE SCHEMA IF NOT EXISTS transfer;
CREATE SCHEMA IF NOT EXISTS exchange;
CREATE SCHEMA IF NOT EXISTS blocker;
CREATE SCHEMA IF NOT EXISTS notifications;

-- Create accounts schema tables
CREATE TABLE IF NOT EXISTS accounts.users (
                                              id UUID PRIMARY KEY,
                                              username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

CREATE TABLE IF NOT EXISTS accounts.bank_accounts (
                                                      id UUID PRIMARY KEY,
                                                      user_id UUID NOT NULL REFERENCES accounts.users(id),
    account_number VARCHAR(20) NOT NULL UNIQUE,
    currency VARCHAR(3) NOT NULL,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

-- Create exchange schema tables
CREATE TABLE IF NOT EXISTS exchange.currency_rates (
                                                       id BIGSERIAL PRIMARY KEY,
                                                       base_currency VARCHAR(3) NOT NULL,
    target_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(19,6) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             UNIQUE(base_currency, target_currency)
    );

-- Create transfer schema tables
CREATE TABLE IF NOT EXISTS transfer.transactions (
                                                     id UUID PRIMARY KEY,
                                                     from_account_id UUID NOT NULL,
                                                     to_account_id UUID NOT NULL,
                                                     amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

-- Create cash schema tables
CREATE TABLE IF NOT EXISTS cash.operations (
                                               id UUID PRIMARY KEY,
                                               account_id UUID NOT NULL,
                                               operation_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

-- Create blocker schema tables
CREATE TABLE IF NOT EXISTS blocker.suspicious_operations (
                                                             id UUID PRIMARY KEY,
                                                             operation_id UUID NOT NULL,
                                                             operation_type VARCHAR(50) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

-- Create notifications schema tables
CREATE TABLE IF NOT EXISTS notifications.messages (
                                                      id UUID PRIMARY KEY,
                                                      user_id UUID NOT NULL,
                                                      type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE
                          );

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_bank_accounts_user_id ON accounts.bank_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_from_account ON transfer.transactions(from_account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_to_account ON transfer.transactions(to_account_id);
CREATE INDEX IF NOT EXISTS idx_operations_account ON cash.operations(account_id);
CREATE INDEX IF NOT EXISTS idx_messages_user ON notifications.messages(user_id);

-- Add some initial data for currency rates
INSERT INTO exchange.currency_rates (base_currency, target_currency, rate) VALUES
                                                                               ('RUB', 'RUB', 1.0),
                                                                               ('RUB', 'USD', 0.011),
                                                                               ('RUB', 'CNY', 0.079),
                                                                               ('USD', 'RUB', 90.91),
                                                                               ('USD', 'CNY', 7.18),
                                                                               ('CNY', 'RUB', 12.66),
                                                                               ('CNY', 'USD', 0.139)
    ON CONFLICT (base_currency, target_currency) DO NOTHING;