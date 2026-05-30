CREATE TABLE IF NOT EXISTS account
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    login     VARCHAR(255)   NOT NULL UNIQUE,
    name      VARCHAR(255)   NOT NULL,
    birthdate DATE           NOT NULL,
    balance   DECIMAL(19, 2) NOT NULL DEFAULT 0.00
);