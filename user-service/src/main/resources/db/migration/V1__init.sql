-- db_users · user-service (A08, A09, A11)

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    login         VARCHAR(50)  NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    status        VARCHAR(10)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_users_login UNIQUE (login),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMINISTRADOR','AUTOR','REVISOR','DOCENTE','ESTUDIANTE')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVO','INACTIVO'))
);
CREATE INDEX ix_users_role_status ON users (role, status);

CREATE TABLE login_audit (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         UUID         NULL REFERENCES users (id),
    login_attempted VARCHAR(100) NOT NULL,
    success         BOOLEAN      NOT NULL,
    failure_reason  VARCHAR(30)  NULL,
    ip_address      VARCHAR(64)  NULL,
    client_agent    VARCHAR(255) NULL,
    occurred_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_login_audit_reason CHECK (failure_reason IS NULL
        OR failure_reason IN ('BAD_CREDENTIALS','USER_INACTIVE','USER_NOT_FOUND'))
);
CREATE INDEX ix_login_audit_login_date ON login_audit (login_attempted, occurred_at DESC);
CREATE INDEX ix_login_audit_date ON login_audit (occurred_at DESC);

CREATE TABLE user_admin_audit (
    id             BIGSERIAL   PRIMARY KEY,
    target_user_id UUID        NOT NULL REFERENCES users (id),
    actor_user_id  UUID        NOT NULL REFERENCES users (id),
    action         VARCHAR(20) NOT NULL,
    old_value      VARCHAR(50) NULL,
    new_value      VARCHAR(50) NULL,
    occurred_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_admin_audit_action CHECK (action IN ('ROLE_CHANGED','ACTIVATED','INACTIVATED'))
);
CREATE INDEX ix_admin_audit_target ON user_admin_audit (target_user_id, occurred_at DESC);
