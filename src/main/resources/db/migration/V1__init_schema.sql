-- 1. Create Orders Table with Optimistic Locking and State Constraints
CREATE TABLE orders(
    order_id     BIGINT NOT NULL AUTO_INCREMENT,
    customer_id  BIGINT NOT NULL,
    total_amount DECIMAL(19, 4) NOT NULL,
    currency     CHAR(3) NOT NULL,
    order_state  VARCHAR(20) NOT NULL,
    version      INT NOT NULL DEFAULT 0, -- Optimistic Locking
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (order_id),

    -- Performance: Composite index for filtering by customer and state
    INDEX idx_orders_customer_state (customer_id, order_state),

    -- Performance: Supporting "Recent Orders" queries for specific customers
    INDEX idx_orders_customer_created_desc (customer_id, created_at DESC),

    -- Data Integrity: Strict State Machine enforcement
    CONSTRAINT chk_order_state CHECK (order_state IN ('CREATED', 'PAID', 'CANCELLED', 'SHIPPED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Create Payments Table with Idempotency and Foreign Key Constraints
CREATE TABLE payments (
    payment_id      BIGINT NOT NULL AUTO_INCREMENT,
    order_id        BIGINT NOT NULL,
    amount          DECIMAL(19, 4) NOT NULL,
    currency        CHAR(3) NOT NULL,
    payment_state   VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    version         INT NOT NULL DEFAULT 0, -- Optimistic Locking
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (payment_id),

    -- Critical: Prevent duplicate payment processing
    UNIQUE INDEX uk_payments_idempotency (idempotency_key),

    -- Performance: Quick lookup for all payments related to an order
    INDEX idx_payments_order (order_id),

    -- Referential Integrity: Ensure payment belongs to a valid order
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders (order_id),

    -- Data Integrity: Strict Payment State Machine
    CONSTRAINT chk_payment_state CHECK (payment_state IN ('PENDING', 'COMPLETED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Order Audit Table: Tracking State Transitions and Observability
CREATE TABLE order_audit (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    order_id      BIGINT NOT NULL,
    from_state    VARCHAR(50) NOT NULL,
    to_state      VARCHAR(50) NOT NULL,
    order_action  VARCHAR(50) NOT NULL,
    changed_by    VARCHAR(50) NOT NULL, -- System or User ID
    trace_id      VARCHAR(64) NOT NULL,
    error_message VARCHAR(500) DEFAULT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    -- Performance: Global timeline of all changes (most recent first)
    INDEX idx_order_audit_created_desc (created_at DESC),

    -- Performance: Specific history for a single order
    INDEX idx_order_audit_order_created_desc (order_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Payment Audit Table: High-Fidelity Transaction Logging
CREATE TABLE payment_audit (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    payment_id     BIGINT NOT NULL,
    from_state     VARCHAR(50) NOT NULL,
    to_state       VARCHAR(50) NOT NULL,
    payment_action VARCHAR(50) NOT NULL,
    changed_by     VARCHAR(50) NOT NULL,
    trace_id       VARCHAR(64) NOT NULL,
    error_message  VARCHAR(500) DEFAULT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    -- Performance: Global audit timeline
    INDEX idx_payment_audit_created_desc (created_at DESC),

    -- Performance: History for a specific payment lifecycle
    INDEX idx_payment_audit_payment_created_desc (payment_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;