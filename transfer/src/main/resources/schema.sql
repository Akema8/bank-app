CREATE TABLE IF NOT EXISTS transfer_transaction
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_login  VARCHAR(255)   NOT NULL,
    to_login    VARCHAR(255)   NOT NULL,
    amount      DECIMAL(19, 2) NOT NULL,
    status      VARCHAR(16)    NOT NULL,
    error       VARCHAR(512),
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
