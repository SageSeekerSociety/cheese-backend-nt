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
            avatar_id INTEGER NOT NULL,
            id INTEGER NOT NULL,
            user_id INTEGER NOT NULL UNIQUE,
            created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
            deleted_at TIMESTAMP(6) WITH TIME ZONE,
            updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
            intro text NOT NULL,
            nickname text NOT NULL,
            CONSTRAINT IDX_51cb79b5555effaf7d69ba1cff PRIMARY KEY(id)
        );

CREATE
    SEQUENCE attachment_id_seq
START WITH
    1 INCREMENT BY 1;

CREATE
    SEQUENCE space_admin_relation_seq
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
    SEQUENCE team_user_relation_seq
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
        attachment(
            id INTEGER NOT NULL,
            TYPE AttachmentType NOT NULL CHECK(
                TYPE BETWEEN 0 AND 3
            ),
            meta jsonb NOT NULL,
            url text NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        avatar(
            id INTEGER DEFAULT nextval('avatar_id_seq') NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        SPACE(
            avatar_id INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            description VARCHAR(255) NOT NULL,
            name VARCHAR(255) NOT NULL,
            PRIMARY KEY(id),
            UNIQUE(name)
        );

CREATE
    TABLE
        space_admin_relation(
            ROLE SMALLINT NOT NULL CHECK(
                ROLE BETWEEN 0 AND 1
            ),
            "user_id" INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            space_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            PRIMARY KEY(id),
            UNIQUE(
                space_id,
                user_id
            )
        );

CREATE
    TABLE
        task(
            "creator_id" INTEGER NOT NULL,
            deadline DATE NOT NULL,
            editable BOOLEAN NOT NULL,
            resubmittable BOOLEAN NOT NULL,
            submitter_type SMALLINT NOT NULL CHECK(
                submitter_type BETWEEN 0 AND 1
            ),
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            space_id BIGINT,
            team_id BIGINT,
            updated_at TIMESTAMP(6) NOT NULL,
            description VARCHAR(255) NOT NULL,
            name VARCHAR(255) NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        task_submission_schema(
            INDEX INTEGER NOT NULL,
            TYPE SMALLINT NOT NULL CHECK(
                TYPE BETWEEN 0 AND 1
            ),
            task_id BIGINT NOT NULL,
            description VARCHAR(255) NOT NULL
        );

CREATE
    TABLE
        task_membership(
            member_id INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            task_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        task_submission(
            content_attachment_id INTEGER,
            INDEX INTEGER NOT NULL,
            version INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            membership_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            content_text VARCHAR(255),
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        team(
            avatar_id INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            description VARCHAR(255) NOT NULL,
            name VARCHAR(255) NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        team_user_relation(
            ROLE SMALLINT NOT NULL CHECK(
                ROLE BETWEEN 0 AND 2
            ),
            "user_id" INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            team_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    INDEX IDXfmekoev1y1edqr9achyy8jp3b ON
    space_admin_relation(space_id);

CREATE
    INDEX IDX9kayuwile36o13jipo63b10sb ON
    space_admin_relation(user_id);

ALTER TABLE
    IF EXISTS public.user_profile ADD CONSTRAINT FKo5dcemd97atrmjapi9x4s1j32 FOREIGN KEY(avatar_id) REFERENCES avatar;

ALTER TABLE
    IF EXISTS public.user_profile ADD CONSTRAINT FKqcd5nmg7d7ement27tt9sf3bi FOREIGN KEY(user_id) REFERENCES public."user";

ALTER TABLE
    IF EXISTS SPACE ADD CONSTRAINT FK77tn26hq1ml6ri82fp970we8n FOREIGN KEY(avatar_id) REFERENCES avatar;

ALTER TABLE
    IF EXISTS space_admin_relation ADD CONSTRAINT FKhkpyunhmsubl1gvahyk9e9lff FOREIGN KEY(space_id) REFERENCES SPACE;

ALTER TABLE
    IF EXISTS space_admin_relation ADD CONSTRAINT FKd3x1m946orup61b4f4wu6h2xn FOREIGN KEY("user_id") REFERENCES public."user";

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

ALTER TABLE
    IF EXISTS task_submission ADD CONSTRAINT FKsxb3wfk0vxfadbe2pam2dxr0f FOREIGN KEY(content_attachment_id) REFERENCES attachment;

ALTER TABLE
    IF EXISTS task_submission ADD CONSTRAINT FK7p8ct6x90ypn5ubixkg9a5cf3 FOREIGN KEY(membership_id) REFERENCES task_membership;

ALTER TABLE
    IF EXISTS team ADD CONSTRAINT FKjv1k745e89swu3gj896pxcq3y FOREIGN KEY(avatar_id) REFERENCES avatar;

ALTER TABLE
    IF EXISTS team_user_relation ADD CONSTRAINT FKtf9y6q1stv6vpqtlclj1okjxs FOREIGN KEY(team_id) REFERENCES team;

ALTER TABLE
    IF EXISTS team_user_relation ADD CONSTRAINT FK8rg61fyticaiphplc6wb3o68p FOREIGN KEY("user_id") REFERENCES public."user";
