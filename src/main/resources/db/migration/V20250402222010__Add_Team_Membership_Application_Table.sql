CREATE SEQUENCE IF NOT EXISTS team_membership_application_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE team_membership_application
(
    id              BIGINT                      NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted_at      TIMESTAMP WITHOUT TIME ZONE,
    user_id         INTEGER                     NOT NULL,
    team_id         BIGINT                      NOT NULL,
    initiator_id    INTEGER                     NOT NULL,
    type            VARCHAR(255)                NOT NULL,
    status          VARCHAR(255)                NOT NULL,
    role            VARCHAR(255)                NOT NULL,
    message         TEXT,
    processed_by_id INTEGER,
    processed_at    TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_teammembershipapplication PRIMARY KEY (id)
);

CREATE INDEX idx_team_membership_application_team_type_status ON team_membership_application (team_id, type, status);

CREATE INDEX idx_team_membership_application_team_user_status ON team_membership_application (team_id, user_id, status);

CREATE INDEX idx_team_membership_application_user_type_status ON team_membership_application (user_id, type, status);

ALTER TABLE team_membership_application
    ADD CONSTRAINT FK_TEAMMEMBERSHIPAPPLICATION_ON_INITIATOR FOREIGN KEY (initiator_id) REFERENCES public."user" (id);

ALTER TABLE team_membership_application
    ADD CONSTRAINT FK_TEAMMEMBERSHIPAPPLICATION_ON_PROCESSEDBY FOREIGN KEY (processed_by_id) REFERENCES public."user" (id);

ALTER TABLE team_membership_application
    ADD CONSTRAINT FK_TEAMMEMBERSHIPAPPLICATION_ON_TEAM FOREIGN KEY (team_id) REFERENCES team (id);

ALTER TABLE team_membership_application
    ADD CONSTRAINT FK_TEAMMEMBERSHIPAPPLICATION_ON_USER FOREIGN KEY (user_id) REFERENCES public."user" (id);