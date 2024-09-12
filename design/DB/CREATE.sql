---------------------------------------------------------------------------
--                        DO NOT MODIFY THIS FILE                        --
--                                                                       --
-- The following content is generated by JPA according to entity classes --
---------------------------------------------------------------------------
CREATE
    TABLE
        public."user"(
            id INTEGER NOT NULL,
            created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
            deleted_at TIMESTAMP(6) WITH TIME ZONE,
            updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
            email text NOT NULL,
            hashed_password text NOT NULL,
            username text NOT NULL,
            PRIMARY KEY(id),
            CONSTRAINT IDX_78a916df40e02a9deb1c4b75ed UNIQUE(username),
            CONSTRAINT IDX_e12875dfb3b1d92d7d7c5377e2 UNIQUE(email)
        );

CREATE
    TABLE
        public.user_profile(
            id INTEGER NOT NULL,
            created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
            deleted_at TIMESTAMP(6) WITH TIME ZONE,
            updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
            intro text NOT NULL,
            nickname text NOT NULL,
            CONSTRAINT IDX_51cb79b5555effaf7d69ba1cff PRIMARY KEY(id)
        );

CREATE
    SEQUENCE space_admin_relation_entity_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE space_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE task_membership_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE task_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE task_submission_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE team_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE user_id_seq
START WITH
    1 INCREMENT BY 1;

CREATE
    SEQUENCE user_profile_id_seq
START WITH
    1 INCREMENT BY 1;

CREATE
    TABLE
        SPACE(
            avatar_id INTEGER,
            created_at TIMESTAMP(6),
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            updated_at TIMESTAMP(6),
            description VARCHAR(255),
            name VARCHAR(255),
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        space_admin_relation_entity(
            member_role SMALLINT CHECK(
                member_role BETWEEN 0 AND 1
            ),
            "user_id" INTEGER,
            created_at TIMESTAMP(6),
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            space_id BIGINT,
            updated_at TIMESTAMP(6),
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        task(
            "creator_id" INTEGER,
            deadline DATE,
            editable BOOLEAN NOT NULL,
            resubmittable BOOLEAN NOT NULL,
            created_at TIMESTAMP(6),
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            space_id BIGINT,
            submitter_type BIGINT NOT NULL,
            team_id BIGINT,
            updated_at TIMESTAMP(6),
            description VARCHAR(255),
            name VARCHAR(255),
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        task_submission_schema(
            value INTEGER,
            task_id BIGINT NOT NULL,
            KEY VARCHAR(255)
        );

CREATE
    TABLE
        task_membership(
            member_id INTEGER,
            created_at TIMESTAMP(6),
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            task_id BIGINT,
            updated_at TIMESTAMP(6),
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        task_submission(
            created_at TIMESTAMP(6),
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            updated_at TIMESTAMP(6),
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        team(
            avatar_id INTEGER,
            created_at TIMESTAMP(6),
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            updated_at TIMESTAMP(6),
            description VARCHAR(255),
            name VARCHAR(255),
            PRIMARY KEY(id)
        );

ALTER TABLE
    IF EXISTS space_admin_relation_entity ADD CONSTRAINT FKis29cde4wcpmo0qna07kw461c FOREIGN KEY(space_id) REFERENCES SPACE;

ALTER TABLE
    IF EXISTS space_admin_relation_entity ADD CONSTRAINT FKkxce7dsvbv4dvfiad43lmxuin FOREIGN KEY("user_id") REFERENCES public."user";

ALTER TABLE
    IF EXISTS task ADD CONSTRAINT FK67uenor8d9f8lq7wjv7h56n2o FOREIGN KEY("creator_id") REFERENCES public."user";

ALTER TABLE
    IF EXISTS task ADD CONSTRAINT FKe6m3e5625asfu59r4doayop1o FOREIGN KEY(space_id) REFERENCES SPACE;

ALTER TABLE
    IF EXISTS task ADD CONSTRAINT FK6r32b6vk1rpu7ww7gratmce1i FOREIGN KEY(team_id) REFERENCES team;

ALTER TABLE
    IF EXISTS task_submission_schema ADD CONSTRAINT FKeipe4rx4f493n82xgon4padr5 FOREIGN KEY(task_id) REFERENCES task;

ALTER TABLE
    IF EXISTS task_membership ADD CONSTRAINT FK6jngswgv5k77n9pj70f18a49y FOREIGN KEY(task_id) REFERENCES task;
