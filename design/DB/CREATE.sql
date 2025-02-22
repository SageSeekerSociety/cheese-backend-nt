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
            hashed_password text,
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
    SEQUENCE knowledge_label_entity_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE knowledge_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE material_bundle_id_seq
START WITH
    1 INCREMENT BY 1;

CREATE
    SEQUENCE material_id_seq
START WITH
    1 INCREMENT BY 1;

CREATE
    SEQUENCE notification_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE project_discussion_reaction_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE project_discussion_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE project_external_collaborator_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE project_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE space_admin_relation_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE space_classification_topics_relation_seq
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
    SEQUENCE task_topics_relation_seq
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
    SEQUENCE topic_id_seq
START WITH
    1 INCREMENT BY 1;

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
        knowledge(
            "created_by_id" INTEGER NOT NULL,
            material_id INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            content jsonb NOT NULL,
            description text NOT NULL,
            name VARCHAR(255) NOT NULL,
            TYPE VARCHAR(255) NOT NULL CHECK(
                TYPE IN(
                    'DOCUMENT',
                    'LINK',
                    'TEXT',
                    'IMAGE'
                )
            ),
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        knowledge_label_entity(
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            knowledge_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            label VARCHAR(50) NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        material(
            download_count INTEGER DEFAULT 0 NOT NULL,
            expires INTEGER,
            id INTEGER NOT NULL,
            uploader_id INTEGER NOT NULL,
            created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
            TYPE VARCHAR(255) NOT NULL,
            meta jsonb NOT NULL,
            name text NOT NULL,
            url text NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        material_bundle(
            comments_count INTEGER DEFAULT 0 NOT NULL,
            creator_id INTEGER NOT NULL,
            id INTEGER NOT NULL,
            my_rating FLOAT(53),
            rating FLOAT(53) DEFAULT 0 NOT NULL,
            rating_count INTEGER DEFAULT 0 NOT NULL,
            created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
            updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            content text NOT NULL,
            title text NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        materialbundles_relation(
            bundle_id INTEGER NOT NULL,
            material_id INTEGER NOT NULL,
            PRIMARY KEY(
                bundle_id,
                material_id
            )
        );

CREATE
    TABLE
        notification(
            READ BOOLEAN NOT NULL,
            receiver_id INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            content VARCHAR(255) NOT NULL,
            TYPE VARCHAR(255) NOT NULL CHECK(
                TYPE IN(
                    'MENTION',
                    'REPLY',
                    'REACTION',
                    'PROJECT_INVITE',
                    'DEADLINE_REMIND'
                )
            ),
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        project(
            "leader_id" INTEGER NOT NULL,
            color_code VARCHAR(7) NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            end_date TIMESTAMP(6) NOT NULL,
            external_task_id BIGINT,
            id BIGINT NOT NULL,
            parent_id BIGINT,
            start_date TIMESTAMP(6) NOT NULL,
            team_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            content text NOT NULL,
            description text NOT NULL,
            github_repo VARCHAR(255),
            name VARCHAR(255) NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        project_knowledge_project(
            knowledge_id BIGINT NOT NULL,
            project_ids BIGINT
        );

CREATE
    TABLE
        project_discussion(
            "sender_id" INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            parent_id BIGINT,
            project_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            content jsonb NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        project_discussion_reaction(
            "user_id" INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            project_discussion_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            emoji VARCHAR(255) NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        project_external_collaborator(
            "user_id" INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            project_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
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
        space_classification_topics_relation(
            topic_id INTEGER NOT NULL,
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
            approved SMALLINT NOT NULL CHECK(
                approved BETWEEN 0 AND 2
            ),
            "creator_id" INTEGER NOT NULL,
            editable BOOLEAN NOT NULL,
            participant_limit INTEGER,
            RANK INTEGER,
            resubmittable BOOLEAN NOT NULL,
            submitter_type SMALLINT NOT NULL CHECK(
                submitter_type BETWEEN 0 AND 1
            ),
            created_at TIMESTAMP(6) NOT NULL,
            deadline TIMESTAMP(6),
            default_deadline BIGINT NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            space_id BIGINT,
            team_id BIGINT,
            updated_at TIMESTAMP(6) NOT NULL,
            description TEXT NOT NULL,
            intro VARCHAR(255) NOT NULL,
            name VARCHAR(255) NOT NULL,
            reject_reason VARCHAR(255) NOT NULL,
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
            apply_reason VARCHAR(255) NOT NULL,
            class_name VARCHAR(255) NOT NULL,
            email VARCHAR(255) NOT NULL,
            grade VARCHAR(255) NOT NULL,
            major VARCHAR(255) NOT NULL,
            personal_advantage VARCHAR(255) NOT NULL,
            phone VARCHAR(255) NOT NULL,
            real_name VARCHAR(255) NOT NULL,
            remark VARCHAR(255) NOT NULL,
            student_id VARCHAR(255) NOT NULL,
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
        task_topics_relation(
            topic_id INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            task_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
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
    TABLE
        topic(
            created_by_id INTEGER NOT NULL,
            id INTEGER NOT NULL,
            created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
            deleted_at TIMESTAMP(6) WITH TIME ZONE,
            name text NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    INDEX IDXaj3gr2rwnv7uamdf03p78372m ON
    knowledge(name);

CREATE
    INDEX IDXpscwbyxoud3wy81qoy7a01f80 ON
    knowledge_label_entity(knowledge_id);

CREATE
    INDEX IDXdwigtt9w7i3sbwkq9s1t5ihh0 ON
    knowledge_label_entity(label);

CREATE
    INDEX IDX3k75vvu7mevyvvb5may5lj8k7 ON
    project(name);

CREATE
    INDEX IDXrl2bt06yn51j9nbk2d0tjituy ON
    project(team_id);

CREATE
    INDEX IDXif1pq04iwqmv1xrsf892htked ON
    project(leader_id);

CREATE
    INDEX IDXkycbyj306lg659w6g2ceuqpnu ON
    project(parent_id);

CREATE
    INDEX IDXqsx9ogphlxqm4g7funlasxwit ON
    project_discussion(project_id);

CREATE
    INDEX IDX31oe4tu1rdota0bkwd2e78oju ON
    project_discussion(sender_id);

CREATE
    INDEX IDXgbv14riv3876fsd5bir8fik52 ON
    project_discussion(parent_id);

CREATE
    INDEX IDXafswtrwv5hq0ml0gulagy7b3b ON
    project_discussion_reaction(project_discussion_id);

CREATE
    INDEX IDX43kw6o6kjlj006asay8wxedn3 ON
    project_discussion_reaction(user_id);

CREATE
    INDEX IDXs4ljf8ihivq2aim4m44e6c3if ON
    project_external_collaborator(project_id);

CREATE
    INDEX IDXl0ohd41bxpy9ln4urlj2v3kcj ON
    project_external_collaborator(user_id);

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
    INDEX IDXo1hn4jgjd2kpue9yt0ptyj2jp ON
    space_classification_topics_relation(space_id);

CREATE
    INDEX IDXby2rktc4t1x2cvvlps3gx5pg3 ON
    space_classification_topics_relation(topic_id);

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
    INDEX IDXka5lit0ursfthx3207eojpvn7 ON
    task_topics_relation(task_id);

CREATE
    INDEX IDXop6rbg45aknhvuqaakuonifao ON
    task_topics_relation(topic_id);

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
    IF EXISTS knowledge ADD CONSTRAINT FKacal20h046lgv8bl4bkl2di1f FOREIGN KEY("created_by_id") REFERENCES public."user";

ALTER TABLE
    IF EXISTS knowledge ADD CONSTRAINT FKhniy1bsjjeoxbpfnlwydlgxtk FOREIGN KEY(material_id) REFERENCES material;

ALTER TABLE
    IF EXISTS knowledge_label_entity ADD CONSTRAINT FKn9rdc2m01070dfpopfiexs5n9 FOREIGN KEY(knowledge_id) REFERENCES knowledge;

ALTER TABLE
    IF EXISTS material ADD CONSTRAINT FK21nqwvdonsvsnp7r3d9uo17bo FOREIGN KEY(uploader_id) REFERENCES public."user" ON
    DELETE
        RESTRICT;

ALTER TABLE
    IF EXISTS material_bundle ADD CONSTRAINT FKl3r75ka0qydpitvbtayq7grsi FOREIGN KEY(creator_id) REFERENCES public."user" ON
    DELETE
        RESTRICT;

ALTER TABLE
    IF EXISTS materialbundles_relation ADD CONSTRAINT FK5fkpr8538ghw2wfjten9hergi FOREIGN KEY(bundle_id) REFERENCES material_bundle ON
    DELETE
        CASCADE;

ALTER TABLE
    IF EXISTS materialbundles_relation ADD CONSTRAINT FK62blmnwrqevg0whwwv5231b5g FOREIGN KEY(material_id) REFERENCES material ON
    DELETE
        RESTRICT;

ALTER TABLE
    IF EXISTS notification ADD CONSTRAINT FKs951ba5cqr6ibbu6w295b3ljg FOREIGN KEY(receiver_id) REFERENCES public."user";

ALTER TABLE
    IF EXISTS project ADD CONSTRAINT FKo1hhpy5548w2mkqptfoejhn9l FOREIGN KEY("leader_id") REFERENCES public."user";

ALTER TABLE
    IF EXISTS project ADD CONSTRAINT FKt0just6g3205u402vn88i0fhy FOREIGN KEY(parent_id) REFERENCES project;

ALTER TABLE
    IF EXISTS project ADD CONSTRAINT FK99hcloicqmg95ty11qht49n8x FOREIGN KEY(team_id) REFERENCES team;

ALTER TABLE
    IF EXISTS project_knowledge_project ADD CONSTRAINT FK118urn3jpv1bbqou4vgt7plbf FOREIGN KEY(knowledge_id) REFERENCES knowledge;

ALTER TABLE
    IF EXISTS project_discussion ADD CONSTRAINT FKtqjqyqmvkqtgm0ehxhokyou00 FOREIGN KEY(parent_id) REFERENCES project_discussion;

ALTER TABLE
    IF EXISTS project_discussion ADD CONSTRAINT FK38osivo96n9pn7tktnookf6hc FOREIGN KEY(project_id) REFERENCES project;

ALTER TABLE
    IF EXISTS project_discussion ADD CONSTRAINT FKacbplng6k4nd13jimdq7smr7d FOREIGN KEY("sender_id") REFERENCES public."user";

ALTER TABLE
    IF EXISTS project_discussion_reaction ADD CONSTRAINT FKgwp92ko43m29ulofqhm9l4ecx FOREIGN KEY(project_discussion_id) REFERENCES project_discussion;

ALTER TABLE
    IF EXISTS project_discussion_reaction ADD CONSTRAINT FKcmgufje6xw5v10l5xkl7c7tg9 FOREIGN KEY("user_id") REFERENCES public."user";

ALTER TABLE
    IF EXISTS project_external_collaborator ADD CONSTRAINT FKg743uet6baba96l4nvt09si6b FOREIGN KEY(project_id) REFERENCES project;

ALTER TABLE
    IF EXISTS project_external_collaborator ADD CONSTRAINT FKont3tarqujw2xphhpsnddpbo7 FOREIGN KEY("user_id") REFERENCES public."user";

ALTER TABLE
    IF EXISTS SPACE ADD CONSTRAINT FK77tn26hq1ml6ri82fp970we8n FOREIGN KEY(avatar_id) REFERENCES avatar;

ALTER TABLE
    IF EXISTS space_admin_relation ADD CONSTRAINT FKhkpyunhmsubl1gvahyk9e9lff FOREIGN KEY(space_id) REFERENCES SPACE;

ALTER TABLE
    IF EXISTS space_admin_relation ADD CONSTRAINT FKd3x1m946orup61b4f4wu6h2xn FOREIGN KEY("user_id") REFERENCES public."user";

ALTER TABLE
    IF EXISTS space_classification_topics_relation ADD CONSTRAINT FKtfux67slxlrbd7975e7066vq FOREIGN KEY(space_id) REFERENCES SPACE;

ALTER TABLE
    IF EXISTS space_classification_topics_relation ADD CONSTRAINT FKpvs648o6f6bdvjsa27dd2pdp1 FOREIGN KEY(topic_id) REFERENCES topic;

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
    IF EXISTS task_topics_relation ADD CONSTRAINT FKjbxaijxjd045fy4ry2pxenrir FOREIGN KEY(task_id) REFERENCES task;

ALTER TABLE
    IF EXISTS task_topics_relation ADD CONSTRAINT FKd1esf4rvrn7eedttnfqs5dfw1 FOREIGN KEY(topic_id) REFERENCES topic;

ALTER TABLE
    IF EXISTS team ADD CONSTRAINT FKjv1k745e89swu3gj896pxcq3y FOREIGN KEY(avatar_id) REFERENCES avatar;

ALTER TABLE
    IF EXISTS team_user_relation ADD CONSTRAINT FKtf9y6q1stv6vpqtlclj1okjxs FOREIGN KEY(team_id) REFERENCES team;

ALTER TABLE
    IF EXISTS team_user_relation ADD CONSTRAINT FK8rg61fyticaiphplc6wb3o68p FOREIGN KEY("user_id") REFERENCES public."user";

ALTER TABLE
    IF EXISTS topic ADD CONSTRAINT FKjy82itq5pcd3u2f8nvtrms0bn FOREIGN KEY(created_by_id) REFERENCES public."user";
