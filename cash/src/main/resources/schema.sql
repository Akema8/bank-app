CREATE TABLE IF NOT EXISTS cash_transaction
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    login       VARCHAR(255)   NOT NULL,
    action      VARCHAR(16)    NOT NULL,
    amount      DECIMAL(19, 2) NOT NULL,
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
