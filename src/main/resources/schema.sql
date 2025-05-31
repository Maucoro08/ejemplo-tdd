CREATE TABLE IF NOT EXISTS transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    -- Otros campos de transacción si fueran necesarios
    amount DECIMAL(19, 2),
    currency VARCHAR(3)
);

CREATE TABLE IF NOT EXISTS transaction_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    previous_status VARCHAR(50) NOT NULL,
    new_status VARCHAR(50) NOT NULL,
    change_date TIMESTAMP NOT NULL,
    user_performing_action VARCHAR(100) NOT NULL,
    FOREIGN KEY (transaction_id) REFERENCES transaction(id)
);

-- Datos iniciales para pruebas (opcional)
-- DELETE FROM transaction_history;
-- DELETE FROM transaction;
-- INSERT INTO transaction (id, status, amount, currency) VALUES (1, 'PENDING', 100.00, 'USD');
-- INSERT INTO transaction (id, status, amount, currency) VALUES (2, 'COMPLETED', 200.00, 'EUR');
-- INSERT INTO transaction (id, status, amount, currency) VALUES (3, 'PENDING', 300.00, 'USD');
-- INSERT INTO transaction (id, status, amount, currency) VALUES (4, 'CANCELLED', 400.00, 'GBP');