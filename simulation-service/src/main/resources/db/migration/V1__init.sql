-- db_simulations · simulation-service (B05–B09)

CREATE TABLE question_replica (
    question_id     UUID         PRIMARY KEY,
    code            VARCHAR(20)  NULL,
    context         TEXT         NOT NULL,
    statement       TEXT         NOT NULL,
    correct_option  CHAR(1)      NOT NULL,
    justification   TEXT         NULL,
    competence      VARCHAR(100) NOT NULL,
    topic           VARCHAR(100) NOT NULL,
    subtopic        VARCHAR(100) NULL,
    difficulty      VARCHAR(10)  NOT NULL,
    available       BOOLEAN      NOT NULL,
    source_version  BIGINT       NOT NULL,
    published_at    TIMESTAMPTZ  NULL,
    synced_at       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_replica_available ON question_replica (available, competence, topic, difficulty);

CREATE TABLE question_replica_options (
    question_id UUID    NOT NULL REFERENCES question_replica (question_id) ON DELETE CASCADE,
    label       CHAR(1) NOT NULL,
    text        TEXT    NOT NULL,
    PRIMARY KEY (question_id, label)
);

CREATE TABLE simulations (
    id                 UUID         PRIMARY KEY,
    student_id         UUID         NOT NULL,
    student_login      VARCHAR(50)  NULL,
    strategy           VARCHAR(20)  NOT NULL,
    filter_competence  VARCHAR(100) NULL,
    filter_topic       VARCHAR(100) NULL,
    filter_difficulty  VARCHAR(10)  NULL,
    question_count     INT          NOT NULL,
    time_limit_minutes INT          NOT NULL,
    status             VARCHAR(15)  NOT NULL,
    started_at         TIMESTAMPTZ  NOT NULL,
    expires_at         TIMESTAMPTZ  NOT NULL,
    finished_at        TIMESTAMPTZ  NULL,
    correct_count      INT          NOT NULL DEFAULT 0,
    global_score       NUMERIC(5,2) NOT NULL DEFAULT 0,
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_sim_status CHECK (status IN ('IN_PROGRESS','FINISHED','EXPIRED')),
    CONSTRAINT ck_sim_strategy CHECK (strategy IN ('COMPETENCE','TOPIC','DIFFICULTY','MIXED'))
);
CREATE INDEX ix_sim_student_status ON simulations (student_id, status);
CREATE INDEX ix_sim_overdue ON simulations (expires_at) WHERE status = 'IN_PROGRESS';
-- Un solo simulacro en curso por estudiante, garantizado también por la base de datos
CREATE UNIQUE INDEX uk_sim_one_active ON simulations (student_id) WHERE status = 'IN_PROGRESS';

CREATE TABLE simulation_items (
    id              UUID         PRIMARY KEY,
    simulation_id   UUID         NOT NULL REFERENCES simulations (id) ON DELETE CASCADE,
    question_id     UUID         NOT NULL REFERENCES question_replica (question_id),
    position        INT          NOT NULL,
    competence      VARCHAR(100) NOT NULL,
    topic           VARCHAR(100) NOT NULL,
    difficulty      VARCHAR(10)  NOT NULL,
    correct_option  CHAR(1)      NOT NULL,
    selected_option CHAR(1)      NULL,
    is_correct      BOOLEAN      NULL,
    answered_at     TIMESTAMPTZ  NULL,
    CONSTRAINT uk_item_position UNIQUE (simulation_id, position),
    CONSTRAINT uk_item_question UNIQUE (simulation_id, question_id)
);

CREATE TABLE competence_scores (
    simulation_id   UUID         NOT NULL REFERENCES simulations (id) ON DELETE CASCADE,
    competence      VARCHAR(100) NOT NULL,
    total_questions INT          NOT NULL,
    correct_answers INT          NOT NULL,
    score           NUMERIC(5,2) NOT NULL,
    PRIMARY KEY (simulation_id, competence)
);

CREATE TABLE processed_events (
    event_id     UUID        PRIMARY KEY,
    event_type   VARCHAR(50) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

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
