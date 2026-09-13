-- Customer table, its id sequence and integrity constraints.
-- The 7-digit id rule is enforced both by the sequence bounds and a CHECK
-- constraint, so no writer can violate it either way.

CREATE SEQUENCE customer_id_seq
    START WITH 1000000
    INCREMENT BY 1
    MINVALUE 1000000
    MAXVALUE 9999999
    NO CYCLE;

CREATE TABLE customers
(
    id           BIGINT                      NOT NULL,
    name         VARCHAR(150)                NOT NULL,
    legal_id     VARCHAR(50)                 NOT NULL,
    type         VARCHAR(20)                 NOT NULL,
    email        VARCHAR(150),
    phone_number VARCHAR(16),

    -- Address is @Embeddable: columns sit inline, not in a join table.
    street       VARCHAR(150)                NOT NULL,
    city         VARCHAR(100)                NOT NULL,
    postal_code  VARCHAR(20),
    country      VARCHAR(2)                  NOT NULL,

    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_legal_id UNIQUE (legal_id),
    CONSTRAINT ck_customers_id_seven_digits CHECK (id BETWEEN 1000000 AND 9999999),
    CONSTRAINT ck_customers_type CHECK (type IN ('RETAIL', 'CORPORATE', 'INVESTMENT'))
);
