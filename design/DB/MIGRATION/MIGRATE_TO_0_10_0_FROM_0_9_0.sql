-- PostgreSQL Migration Script for Cheese Backend NT (0.9.0 -> 0.10.0)
-- Backup Before Executing This in Production Database

CREATE
    SEQUENCE ai_conversation_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE ai_message_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE knowledge_label_entity_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE knowledge_seq
START WITH
    1 INCREMENT BY 50;

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
    SEQUENCE task_ai_advice_context_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE user_ai_quota_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE user_seu_consumption_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    TABLE
        ai_conversation(
            context_id BIGINT,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            owner_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            conversation_id VARCHAR(255) NOT NULL UNIQUE,
            model_type TEXT DEFAULT 'standard' NOT NULL,
            module_type VARCHAR(255) NOT NULL,
            title VARCHAR(255),
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        ai_message(
            seu_consumed NUMERIC(
                10,
                4
            ),
            tokens_used INTEGER,
            conversation_id BIGINT NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            parent_id BIGINT,
            reasoning_time_ms BIGINT,
            updated_at TIMESTAMP(6) NOT NULL,
            content TEXT NOT NULL,
            model_type TEXT DEFAULT 'standard' NOT NULL,
            reasoning_content TEXT,
            ROLE VARCHAR(255) NOT NULL,
            metadata JSONB,
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
        notification(
            READ BOOLEAN NOT NULL,
            TYPE SMALLINT NOT NULL CHECK(
                TYPE BETWEEN 0 AND 4
            ),
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            discussion_id BIGINT,
            id BIGINT NOT NULL,
            knowledge_id BIGINT,
            project_id BIGINT,
            receiver_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            text VARCHAR(255),
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
        task_ai_advice(
            created_at TIMESTAMP(6) NOT NULL,
            id BIGINT GENERATED BY DEFAULT AS IDENTITY,
            task_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            model_hash VARCHAR(64) NOT NULL,
            knowledge_fields TEXT,
            learning_paths TEXT,
            methodology TEXT,
            raw_response TEXT,
            status VARCHAR(255) NOT NULL CHECK(
                status IN(
                    'PENDING',
                    'PROCESSING',
                    'COMPLETED',
                    'FAILED'
                )
            ),
            team_tips TEXT,
            topic_summary TEXT,
            PRIMARY KEY(id),
            UNIQUE(
                task_id,
                model_hash
            )
        );

CREATE
    TABLE
        task_ai_advice_context(
            section_index INTEGER,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            task_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            SECTION VARCHAR(255),
            PRIMARY KEY(id),
            CONSTRAINT uk_task_section_index UNIQUE(
                task_id,
                SECTION,
                section_index
            )
        );

CREATE
    TABLE
        user_ai_quota(
            daily_seu_quota NUMERIC(
                10,
                4
            ),
            remaining_seu NUMERIC(
                10,
                4
            ),
            total_seu_consumed NUMERIC(
                10,
                4
            ),
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            last_reset_time TIMESTAMP(6),
            updated_at TIMESTAMP(6) NOT NULL,
            user_id BIGINT,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        user_seu_consumption(
            is_cached BOOLEAN,
            seu_consumed NUMERIC(
                10,
                4
            ),
            tokens_used INTEGER,
            cache_expire_at TIMESTAMP(6),
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            user_id BIGINT,
            cache_key VARCHAR(255),
            request_id VARCHAR(255),
            resource_type VARCHAR(255) CHECK(
                resource_type IN(
                    'LIGHTWEIGHT',
                    'STANDARD',
                    'ADVANCED'
                )
            ),
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

ALTER TABLE
    IF EXISTS ai_message ADD CONSTRAINT FKsaxwtwysovynl73cqwffe1fbn FOREIGN KEY(conversation_id) REFERENCES ai_conversation;

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