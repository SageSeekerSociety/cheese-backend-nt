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
    SEQUENCE space_user_rank_seq
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
    SEQUENCE task_submission_entry_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE task_submission_review_seq
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
            TYPE VARCHAR(255) NOT NULL,
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
            enable_rank BOOLEAN NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            announcements TEXT NOT NULL,
            description TEXT NOT NULL,
            intro VARCHAR(255) NOT NULL,
            name VARCHAR(255) NOT NULL,
            task_templates TEXT NOT NULL,
            PRIMARY KEY(id)
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
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        space_user_rank(
            RANK INTEGER NOT NULL,
            "user_id" INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            space_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        task(
            approved BOOLEAN NOT NULL,
            "creator_id" INTEGER NOT NULL,
            editable BOOLEAN NOT NULL,
            RANK INTEGER,
            resubmittable BOOLEAN NOT NULL,
            submitter_type SMALLINT NOT NULL CHECK(
                submitter_type BETWEEN 0 AND 1
            ),
            created_at TIMESTAMP(6) NOT NULL,
            deadline TIMESTAMP(6),
            default_deadline BIGINT,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            space_id BIGINT,
            team_id BIGINT,
            updated_at TIMESTAMP(6) NOT NULL,
            description TEXT NOT NULL,
            intro VARCHAR(255) NOT NULL,
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
            approved SMALLINT NOT NULL CHECK(
                approved BETWEEN 0 AND 2
            ),
            created_at TIMESTAMP(6) NOT NULL,
            deadline TIMESTAMP(6),
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            member_id BIGINT NOT NULL,
            task_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        task_submission(
            submitter_id INTEGER NOT NULL,
            version INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            membership_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        task_submission_entry(
            content_attachment_id INTEGER,
            INDEX INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            task_submission_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            content_text VARCHAR(255),
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        task_submission_review(
            accepted BOOLEAN NOT NULL,
            score INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            submission_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            comment VARCHAR(255) NOT NULL,
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
            description TEXT NOT NULL,
            intro VARCHAR(255) NOT NULL,
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
    INDEX IDXmllyu96n9vj606vm9w0gp3obx ON
    SPACE(name);

CREATE
    INDEX IDXfmekoev1y1edqr9achyy8jp3b ON
    space_admin_relation(space_id);

CREATE
    INDEX IDX9kayuwile36o13jipo63b10sb ON
    space_admin_relation(user_id);

CREATE
    INDEX IDX5se9sb4u9yywkp49gnim8s85n ON
    space_user_rank(space_id);

CREATE
    INDEX IDX4poyg61j8nhdhsryne7s8snmr ON
    space_user_rank(user_id);

CREATE
    INDEX IDX70x5oq6omtraaie2fttiv25rd ON
    task_membership(task_id);

CREATE
    INDEX IDXh685vv2ufp7ohjnfw6hw231tv ON
    task_membership(member_id);

CREATE
    INDEX IDXglmpyfy44ju9tr2tm5h6j8u3t ON
    task_submission(membership_id);

CREATE
    INDEX IDXlac91yl30evfn8mq1kh91jyyg ON
    task_submission_entry(task_submission_id);

CREATE
    INDEX IDX3ctl72phv5d0yluddhpkxb1y4 ON
    task_submission_review(submission_id);

CREATE
    INDEX IDXg2l9qqsoeuynt4r5ofdt1x2td ON
    team(name);

CREATE
    INDEX IDXd45hsmkordpydcytq25ahcnby ON
    team_user_relation(team_id);

CREATE
    INDEX IDXquiqutq8cnj700u982xlo8bsm ON
    team_user_relation(user_id);

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
    IF EXISTS space_user_rank ADD CONSTRAINT FKh0k0jxvhnph0eoc5gw7652hdy FOREIGN KEY(space_id) REFERENCES SPACE;

ALTER TABLE
    IF EXISTS space_user_rank ADD CONSTRAINT FKiego7kcolpikn8o93o3qdjt3p FOREIGN KEY("user_id") REFERENCES public."user";

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
    IF EXISTS task_submission ADD CONSTRAINT FK7p8ct6x90ypn5ubixkg9a5cf3 FOREIGN KEY(membership_id) REFERENCES task_membership;

ALTER TABLE
    IF EXISTS task_submission ADD CONSTRAINT FKgqx6epr8btfbjv5lneb6ayqu4 FOREIGN KEY(submitter_id) REFERENCES public."user";

ALTER TABLE
    IF EXISTS task_submission_entry ADD CONSTRAINT FK8d9mtcc0w6k35dfj8csp6hsos FOREIGN KEY(content_attachment_id) REFERENCES attachment;

ALTER TABLE
    IF EXISTS task_submission_entry ADD CONSTRAINT FK3rhnhj7id0krtih5rhveivmgc FOREIGN KEY(task_submission_id) REFERENCES task_submission;

ALTER TABLE
    IF EXISTS task_submission_review ADD CONSTRAINT FKba2fmo0mgjdlohcnpf97tvgvt FOREIGN KEY(submission_id) REFERENCES task_submission;

ALTER TABLE
    IF EXISTS team ADD CONSTRAINT FKjv1k745e89swu3gj896pxcq3y FOREIGN KEY(avatar_id) REFERENCES avatar;

ALTER TABLE
    IF EXISTS team_user_relation ADD CONSTRAINT FKtf9y6q1stv6vpqtlclj1okjxs FOREIGN KEY(team_id) REFERENCES team;

ALTER TABLE
    IF EXISTS team_user_relation ADD CONSTRAINT FK8rg61fyticaiphplc6wb3o68p FOREIGN KEY("user_id") REFERENCES public."user";
