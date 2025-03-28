-- PostgreSQL Migration Script for Cheese Backend NT (0.10.X -> 0.11.0)
-- Backup Before Executing This in Production Database

ALTER SEQUENCE knowledge_label_entity_seq RENAME TO knowledge_label_seq;

CREATE
    SEQUENCE discussion_reaction_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE discussion_seq
START WITH
    1 INCREMENT BY 50;

DROP SEQUENCE IF EXISTS project_discussion_reaction_seq;
DROP SEQUENCE IF EXISTS project_discussion_seq;
DROP SEQUENCE IF EXISTS project_external_collaborator_seq;
DROP SEQUENCE IF EXISTS project_seq;

CREATE
    SEQUENCE project_membership_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE project_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE reaction_type_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE user_real_name_access_logs_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE user_real_name_identities_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    TABLE
        discussion(
            sender_id INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            model_id BIGINT NOT NULL,
            parent_id BIGINT,
            updated_at TIMESTAMP(6) NOT NULL,
            model_type VARCHAR(255) NOT NULL CHECK(
                model_type IN('PROJECT')
            ),
            content JSONB NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        discussion_mentioned_users(
            discussion_id BIGINT NOT NULL,
            user_id BIGINT
        );

CREATE
    TABLE
        discussion_reaction(
            user_id INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            discussion_id BIGINT NOT NULL,
            id BIGINT NOT NULL,
            reaction_type_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            PRIMARY KEY(id),
            CONSTRAINT uk_discussion_reaction_user_type UNIQUE(
                discussion_id,
                user_id,
                reaction_type_id
            )
        );

CREATE
    TABLE
        encryption_keys(
            purpose SMALLINT NOT NULL CHECK(
                purpose BETWEEN 0 AND 1
            ),
            created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            related_entity_id BIGINT,
            id VARCHAR(255) NOT NULL,
            key_value VARCHAR(255) NOT NULL,
            PRIMARY KEY(id)
        );


DROP INDEX IDXpscwbyxoud3wy81qoy7a01f80;
DROP INDEX IDXdwigtt9w7i3sbwkq9s1t5ihh0;
DROP INDEX IDX3k75vvu7mevyvvb5may5lj8k7;
DROP INDEX IDXrl2bt06yn51j9nbk2d0tjituy;
DROP INDEX IDXif1pq04iwqmv1xrsf892htked;
DROP INDEX IDXkycbyj306lg659w6g2ceuqpnu;
DROP INDEX IDXqsx9ogphlxqm4g7funlasxwit;
DROP INDEX IDX31oe4tu1rdota0bkwd2e78oju;
DROP INDEX IDXgbv14riv3876fsd5bir8fik52;
DROP INDEX IDXafswtrwv5hq0ml0gulagy7b3b;
DROP INDEX IDX43kw6o6kjlj006asay8wxedn3;
DROP INDEX IDXs4ljf8ihivq2aim4m44e6c3if;
DROP INDEX IDXl0ohd41bxpy9ln4urlj2v3kcj;
DROP TABLE IF EXISTS project_knowledge_project;
DROP TABLE IF EXISTS knowledge_label_entity;
DROP TABLE IF EXISTS knowledge;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS project_discussion_reaction;
DROP TABLE IF EXISTS project_discussion;
DROP TABLE IF EXISTS project_external_collaborator;
DROP TABLE IF EXISTS project;

CREATE
    TABLE
        knowledge(
            "created_by_id" INTEGER NOT NULL,
            material_id INTEGER,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            discussion_id BIGINT,
            id BIGINT NOT NULL,
            project_id BIGINT,
            team_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            description text NOT NULL,
            name VARCHAR(255) NOT NULL,
            source_type VARCHAR(255) NOT NULL CHECK(
                source_type IN(
                    'MANUAL',
                    'FROM_DISCUSSION'
                )
            ),
            TYPE VARCHAR(255) NOT NULL CHECK(
                TYPE IN(
                    'MATERIAL',
                    'LINK',
                    'TEXT',
                    'CODE'
                )
            ),
            content jsonb NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        knowledge_knowledge_labels(
            knowledge_id BIGINT NOT NULL,
            knowledge_labels_id BIGINT NOT NULL UNIQUE
        );

CREATE
    TABLE
        knowledge_label(
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
            receiver_id INTEGER NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            TYPE VARCHAR(255) NOT NULL CHECK(
                TYPE IN(
                    'MENTION',
                    'REPLY',
                    'REACTION',
                    'PROJECT_INVITE',
                    'DEADLINE_REMIND'
                )
            ),
            content JSONB,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        project(
            archived BOOLEAN DEFAULT FALSE NOT NULL,
            color_code VARCHAR(7) NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            end_date TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            external_task_id BIGINT,
            id BIGINT NOT NULL,
            leader_id BIGINT NOT NULL,
            parent_id BIGINT,
            start_date TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            team_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            description text NOT NULL,
            github_repo VARCHAR(255),
            name VARCHAR(255) NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        project_membership(
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            project_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            user_id BIGINT NOT NULL,
            notes text,
            ROLE VARCHAR(255) NOT NULL CHECK(
                ROLE IN(
                    'LEADER',
                    'MEMBER',
                    'EXTERNAL'
                )
            ),
            PRIMARY KEY(id),
            UNIQUE(
                project_id,
                user_id
            )
        );

CREATE
    TABLE
        reaction_type(
            display_order INTEGER NOT NULL,
            is_active BOOLEAN NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            code VARCHAR(32) NOT NULL,
            name VARCHAR(64) NOT NULL,
            description VARCHAR(255),
            PRIMARY KEY(id),
            CONSTRAINT idx_reaction_type_code UNIQUE(code)
        );

ALTER TABLE task 
ADD COLUMN require_real_name BOOLEAN DEFAULT FALSE NOT NULL;

CREATE
    TABLE
        task_membership_team_members(
            encrypted BOOLEAN DEFAULT FALSE,
            member_id BIGINT NOT NULL,
            task_membership_id BIGINT NOT NULL,
            class_name VARCHAR(255),
            grade VARCHAR(255),
            major VARCHAR(255),
            real_name VARCHAR(255),
            student_id VARCHAR(255)
        );

ALTER TABLE task_membership 
ADD COLUMN encrypted BOOLEAN DEFAULT FALSE;

ALTER TABLE task_membership 
ADD COLUMN is_team BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE task_membership ts
SET is_team = 
    CASE
        WHEN t.submitter_type = 1 THEN TRUE
        ELSE FALSE
    END
FROM task t
WHERE ts.task_id = t.id;

ALTER TABLE task_membership 
ADD COLUMN encryption_key_id VARCHAR(255);

ALTER TABLE task_membership ALTER COLUMN class_name DROP NOT NULL;
ALTER TABLE task_membership ALTER COLUMN grade DROP NOT NULL;
ALTER TABLE task_membership ALTER COLUMN major DROP NOT NULL;
ALTER TABLE task_membership ALTER COLUMN real_name DROP NOT NULL;
ALTER TABLE task_membership ALTER COLUMN student_id DROP NOT NULL;

CREATE
    TABLE
        user_real_name_access_logs(
            accessor_id BIGINT NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            module_entity_id BIGINT,
            target_id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            access_reason VARCHAR(255) NOT NULL,
            access_type VARCHAR(255) NOT NULL CHECK(
                access_type IN(
                    'VIEW',
                    'EXPORT'
                )
            ),
            ip_address VARCHAR(255) NOT NULL,
            module_type VARCHAR(255) CHECK(
                module_type IN('TASK')
            ),
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        user_real_name_identities(
            encrypted BOOLEAN DEFAULT FALSE NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            deleted_at TIMESTAMP(6),
            id BIGINT NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            user_id BIGINT NOT NULL,
            class_name VARCHAR(255) NOT NULL,
            encryption_key_id VARCHAR(255) NOT NULL,
            grade VARCHAR(255) NOT NULL,
            major VARCHAR(255) NOT NULL,
            real_name VARCHAR(255) NOT NULL,
            student_id VARCHAR(255) NOT NULL,
            PRIMARY KEY(id)
        );

CREATE
    TABLE
        user_role(
            id BIGINT GENERATED BY DEFAULT AS IDENTITY,
            user_id BIGINT NOT NULL,
            ROLE VARCHAR(255) NOT NULL CHECK(
                ROLE IN(
                    'SUPER_ADMIN',
                    'ADMIN',
                    'MODERATOR',
                    'USER'
                )
            ),
            PRIMARY KEY(id),
            UNIQUE(
                user_id,
                ROLE
            )
        );

CREATE
    INDEX IDXiuy7mmgy0obas3bfqu1j519bc ON
    discussion(
        model_type,
        model_id
    );

CREATE
    INDEX IDXkhxb42bgml7ds4m1mn25ulgpj ON
    discussion(sender_id);

CREATE
    INDEX idx_reaction_discussion_type ON
    discussion_reaction(
        discussion_id,
        reaction_type_id
    );

CREATE
    INDEX idx_reaction_discussion_user ON
    discussion_reaction(
        discussion_id,
        user_id
    );

CREATE
    INDEX IDXlk5f1tf6v3vslj8o1rfqwgcwi ON
    knowledge(team_id);

CREATE
    INDEX IDXjabwimmbvt40wl20fp0kaevp4 ON
    knowledge(source_type);

CREATE
    INDEX IDXk01yi4pk1orlwhihsorqhnwr8 ON
    knowledge(project_id);

CREATE
    INDEX IDXp6qd6pe6q2uaa78bypcy99k2k ON
    knowledge(discussion_id);

CREATE
    INDEX IDXiyfipycyf7afycfn9f8gkhcvf ON
    knowledge_label(knowledge_id);

CREATE
    INDEX IDX2lm01yrx3fukjj9a89v0hpr2 ON
    knowledge_label(label);

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
    INDEX IDX95fxueh91d9uobhmly6smcdtw ON
    project_membership(project_id);

CREATE
    INDEX IDXc0ggmiab5m1chu88hitikohha ON
    project_membership(user_id);

CREATE
    INDEX IDXobr8vksep0or878byk2kohpty ON
    project_membership(ROLE);

CREATE
    INDEX IDXapcc8lxk2xnug8377fatvbn04 ON
    user_role(user_id);

ALTER TABLE
    IF EXISTS discussion ADD CONSTRAINT FKgs1dxhfnb68rh344y4f34fwqa FOREIGN KEY(parent_id) REFERENCES discussion;

ALTER TABLE
    IF EXISTS discussion ADD CONSTRAINT FK7wmfmeiq6a9y5ygdi1obp01r FOREIGN KEY(sender_id) REFERENCES public."user";

ALTER TABLE
    IF EXISTS discussion_mentioned_users ADD CONSTRAINT FKg7pxkxyxyxq2g2qttjeyph6ra FOREIGN KEY(discussion_id) REFERENCES discussion;

ALTER TABLE
    IF EXISTS discussion_reaction ADD CONSTRAINT FKawpinudxl9749buwtradij9p6 FOREIGN KEY(discussion_id) REFERENCES discussion;

ALTER TABLE
    IF EXISTS discussion_reaction ADD CONSTRAINT FK9r8w78vm7sdf6iqkcl0pc9jqp FOREIGN KEY(reaction_type_id) REFERENCES reaction_type;

ALTER TABLE
    IF EXISTS discussion_reaction ADD CONSTRAINT FKjh9fs8xa2fuupaw2josqcjbfv FOREIGN KEY(user_id) REFERENCES public."user";

ALTER TABLE
    IF EXISTS knowledge ADD CONSTRAINT FKacal20h046lgv8bl4bkl2di1f FOREIGN KEY("created_by_id") REFERENCES public."user";

ALTER TABLE
    IF EXISTS knowledge ADD CONSTRAINT FKhniy1bsjjeoxbpfnlwydlgxtk FOREIGN KEY(material_id) REFERENCES material;

ALTER TABLE
    IF EXISTS knowledge ADD CONSTRAINT FK1df46ccdrd9w7i5bf1urij4j2 FOREIGN KEY(discussion_id) REFERENCES discussion;

ALTER TABLE
    IF EXISTS knowledge ADD CONSTRAINT FKs854y5ajesie1jja4thskulsc FOREIGN KEY(team_id) REFERENCES team;

ALTER TABLE
    IF EXISTS knowledge_knowledge_labels ADD CONSTRAINT FKd9dp2f3c65b8d2gdc9oeql6ja FOREIGN KEY(knowledge_labels_id) REFERENCES knowledge_label;

ALTER TABLE
    IF EXISTS knowledge_knowledge_labels ADD CONSTRAINT FKbdxbbpetfqn7y3cuq50tgcuoq FOREIGN KEY(knowledge_id) REFERENCES knowledge;

ALTER TABLE
    IF EXISTS knowledge_label ADD CONSTRAINT FK43k38w4y2j691mb6nhyijhq96 FOREIGN KEY(knowledge_id) REFERENCES knowledge;

ALTER TABLE
    IF EXISTS notification ADD CONSTRAINT FKs951ba5cqr6ibbu6w295b3ljg FOREIGN KEY(receiver_id) REFERENCES public."user";

ALTER TABLE
    IF EXISTS project ADD CONSTRAINT FKt0just6g3205u402vn88i0fhy FOREIGN KEY(parent_id) REFERENCES project;

ALTER TABLE
    IF EXISTS project_membership ADD CONSTRAINT FK6nerxrll628ug1mvgpt2spgpk FOREIGN KEY(project_id) REFERENCES project;

ALTER TABLE
    IF EXISTS task_membership_team_members ADD CONSTRAINT FKqqe540ncjf6ylguc5r3mt51sm FOREIGN KEY(task_membership_id) REFERENCES task_membership;

