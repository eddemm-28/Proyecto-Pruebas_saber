-- db_questions · question-service (A12, A13, A17, A18)

CREATE SEQUENCE question_code_seq START 1;

CREATE TABLE questions (
    id               UUID         PRIMARY KEY,
    code             VARCHAR(20)  NOT NULL,
    title            VARCHAR(150) NOT NULL,
    question_type    VARCHAR(20)  NOT NULL,
    context          TEXT         NOT NULL,
    statement        TEXT         NOT NULL,
    correct_option   CHAR(1)      NOT NULL,
    justification    TEXT         NOT NULL,
    bibliography     TEXT         NOT NULL,
    competence       VARCHAR(100) NOT NULL,
    topic            VARCHAR(100) NOT NULL,
    subtopic         VARCHAR(100) NOT NULL,
    difficulty       VARCHAR(10)  NOT NULL,
    state            VARCHAR(20)  NOT NULL,
    author_id        UUID         NOT NULL,
    author_login     VARCHAR(50)  NOT NULL,
    plugin_id        VARCHAR(40)  NOT NULL DEFAULT 'MANUAL',
    media_url        VARCHAR(500) NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    published_at     TIMESTAMPTZ  NULL,
    archived_at      TIMESTAMPTZ  NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_questions_code UNIQUE (code),
    CONSTRAINT ck_questions_type CHECK (question_type IN ('MULTIPLE_CHOICE','CASE','MULTIMEDIA')),
    CONSTRAINT ck_questions_correct CHECK (correct_option IN ('A','B','C','D')),
    CONSTRAINT ck_questions_difficulty CHECK (difficulty IN ('FACIL','MEDIA','DIFICIL')),
    CONSTRAINT ck_questions_state CHECK (state IN ('BORRADOR','PENDIENTE_REVISION','EN_REVISION',
                                                   'APROBADA','RECHAZADA','PUBLICADA','ARCHIVADA'))
);
CREATE INDEX ix_questions_state ON questions (state);
CREATE INDEX ix_questions_author ON questions (author_id);
CREATE INDEX ix_questions_classification ON questions (competence, topic, difficulty);

CREATE TABLE question_options (
    question_id UUID    NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    label       CHAR(1) NOT NULL,
    text        TEXT    NOT NULL,
    PRIMARY KEY (question_id, label),
    CONSTRAINT ck_options_label CHECK (label IN ('A','B','C','D'))
);
CREATE UNIQUE INDEX uk_options_text ON question_options (question_id, lower(text));

CREATE TABLE question_change_history (
    id               BIGSERIAL    PRIMARY KEY,
    question_id      UUID         NOT NULL REFERENCES questions (id),
    changed_by       UUID         NOT NULL,
    changed_by_login VARCHAR(50)  NOT NULL,
    field_name       VARCHAR(40)  NOT NULL,
    old_value        TEXT         NULL,
    new_value        TEXT         NULL,
    changed_at       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_change_history_question ON question_change_history (question_id, changed_at DESC);

CREATE TABLE question_state_audit (
    id               BIGSERIAL    PRIMARY KEY,
    question_id      UUID         NOT NULL REFERENCES questions (id),
    from_state       VARCHAR(20)  NULL,
    to_state         VARCHAR(20)  NOT NULL,
    changed_by       UUID         NOT NULL,
    changed_by_login VARCHAR(50)  NOT NULL,
    comment          VARCHAR(1000) NULL,
    changed_at       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_state_audit_question ON question_state_audit (question_id, changed_at DESC);

CREATE TABLE outbox_events (
    id            UUID         PRIMARY KEY,
    aggregate_id  UUID         NOT NULL,
    event_type    VARCHAR(50)  NOT NULL,
    routing_key   VARCHAR(100) NOT NULL,
    payload       JSONB        NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    published_at  TIMESTAMPTZ  NULL,
    attempts      INT          NOT NULL DEFAULT 0,
    last_error    VARCHAR(500) NULL
);
CREATE INDEX ix_outbox_pending ON outbox_events (created_at) WHERE published_at IS NULL;
