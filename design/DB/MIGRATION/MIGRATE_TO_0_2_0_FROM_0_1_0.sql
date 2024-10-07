-- PostgreSQL Migration Script for Cheese Backend NT (0.1.0 -> 0.2.0)
-- Backup Before Excuting This in Production Database

CREATE
    SEQUENCE space_user_rank_seq
START WITH
    1 INCREMENT BY 50;

CREATE
    SEQUENCE task_submission_review_seq
START WITH
    1 INCREMENT BY 50;

ALTER TABLE SPACE ADD COLUMN enable_rank BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE SPACE ADD COLUMN intro VARCHAR(255) NOT NULL DEFAULT '';
UPDATE SPACE SET intro = LEFT(description, 255);
ALTER TABLE SPACE ALTER COLUMN description TYPE TEXT;
ALTER TABLE SPACE ALTER COLUMN intro DROP DEFAULT;

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

ALTER TABLE TASK ADD COLUMN RANK INTEGER;

ALTER TABLE TASK ADD COLUMN intro VARCHAR(255) NOT NULL DEFAULT '';
UPDATE TASK SET intro = LEFT(description, 255);
ALTER TABLE TASK ALTER COLUMN description TYPE TEXT;
ALTER TABLE TASK ALTER COLUMN intro DROP DEFAULT;

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

ALTER TABLE TEAM ADD COLUMN intro VARCHAR(255) NOT NULL DEFAULT '';
UPDATE TEAM SET intro = LEFT(description, 255);
ALTER TABLE TEAM ALTER COLUMN description TYPE TEXT;
ALTER TABLE TEAM ALTER COLUMN intro DROP DEFAULT;

CREATE
    INDEX IDX5se9sb4u9yywkp49gnim8s85n ON
    space_user_rank(space_id);

CREATE
    INDEX IDX4poyg61j8nhdhsryne7s8snmr ON
    space_user_rank(user_id);

CREATE
    INDEX IDX3ctl72phv5d0yluddhpkxb1y4 ON
    task_submission_review(submission_id);

ALTER TABLE
    IF EXISTS space_user_rank ADD CONSTRAINT FKh0k0jxvhnph0eoc5gw7652hdy FOREIGN KEY(space_id) REFERENCES SPACE;

ALTER TABLE
    IF EXISTS space_user_rank ADD CONSTRAINT FKiego7kcolpikn8o93o3qdjt3p FOREIGN KEY("user_id") REFERENCES public."user";

ALTER TABLE
    IF EXISTS task_submission_review ADD CONSTRAINT FKba2fmo0mgjdlohcnpf97tvgvt FOREIGN KEY(submission_id) REFERENCES task_submission;
