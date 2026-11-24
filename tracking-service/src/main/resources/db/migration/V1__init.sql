-- db_tracking · tracking-service (B10–B13)

CREATE TABLE simulation_results (
    simulation_id    UUID         PRIMARY KEY,
    student_id       UUID         NOT NULL,
    student_login    VARCHAR(50)  NULL,
    final_status     VARCHAR(15)  NOT NULL,
    strategy         VARCHAR(20)  NULL,
    total_questions  INT          NOT NULL,
    correct_answers  INT          NOT NULL,
    global_score     NUMERIC(5,2) NOT NULL,
    duration_seconds INT          NOT NULL,
    started_at       TIMESTAMPTZ  NULL,
    finished_at      TIMESTAMPTZ  NOT NULL,
    received_at      TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_results_student_date ON simulation_results (student_id, finished_at DESC);
CREATE INDEX ix_results_date ON simulation_results (finished_at);

CREATE TABLE result_competence_scores (
    simulation_id   UUID         NOT NULL REFERENCES simulation_results (simulation_id) ON DELETE CASCADE,
    competence      VARCHAR(100) NOT NULL,
    total_questions INT          NOT NULL,
    correct_answers INT          NOT NULL,
    score           NUMERIC(5,2) NOT NULL,
    PRIMARY KEY (simulation_id, competence)
);

CREATE TABLE result_answers (
    simulation_id UUID         NOT NULL REFERENCES simulation_results (simulation_id) ON DELETE CASCADE,
    question_id   UUID         NOT NULL,
    competence    VARCHAR(100) NOT NULL,
    topic         VARCHAR(100) NULL,
    difficulty    VARCHAR(10)  NULL,
    answered      BOOLEAN      NOT NULL,
    is_correct    BOOLEAN      NOT NULL,
    PRIMARY KEY (simulation_id, question_id)
);
CREATE INDEX ix_answers_topic ON result_answers (competence, topic);

CREATE TABLE student_competence_stats (
    student_id        UUID         NOT NULL,
    competence        VARCHAR(100) NOT NULL,
    student_login     VARCHAR(50)  NULL,
    simulations_count INT          NOT NULL,
    total_questions   INT          NOT NULL,
    correct_answers   INT          NOT NULL,
    average_score     NUMERIC(5,2) NOT NULL,
    last_score        NUMERIC(5,2) NOT NULL,
    classification    VARCHAR(15)  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (student_id, competence),
    CONSTRAINT ck_stats_class CHECK (classification IN ('FORTALEZA','EN_DESARROLLO','DEBILIDAD'))
);

CREATE TABLE processed_events (
    event_id     UUID        PRIMARY KEY,
    event_type   VARCHAR(50) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
