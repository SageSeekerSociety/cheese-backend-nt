-- PostgreSQL Migration Script for Cheese Backend NT (0.7.x -> 0.8.0)
-- Backup Before Executing This in Production Database

CREATE SEQUENCE space_classification_topics_relation_seq
    START WITH 1 INCREMENT BY 50;
CREATE TABLE space_classification_topics_relation
(
    topic_id   INTEGER      NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6),
    id         BIGINT       NOT NULL,
    space_id   BIGINT       NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE SEQUENCE task_topics_relation_seq
    START WITH 1 INCREMENT BY 50;
CREATE TABLE task_topics_relation
(
    topic_id   INTEGER      NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6),
    id         BIGINT       NOT NULL,
    task_id    BIGINT       NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IDXo1hn4jgjd2kpue9yt0ptyj2jp ON space_classification_topics_relation (space_id);
CREATE INDEX IDXby2rktc4t1x2cvvlps3gx5pg3 ON space_classification_topics_relation (topic_id);
CREATE INDEX IDXka5lit0ursfthx3207eojpvn7 ON task_topics_relation (task_id);
CREATE INDEX IDXop6rbg45aknhvuqaakuonifao ON task_topics_relation (topic_id);

ALTER TABLE IF EXISTS space_classification_topics_relation
    ADD CONSTRAINT FKtfux67slxlrbd7975e7066vq FOREIGN KEY (space_id) REFERENCES SPACE;
ALTER TABLE IF EXISTS space_classification_topics_relation
    ADD CONSTRAINT FKpvs648o6f6bdvjsa27dd2pdp1 FOREIGN KEY (topic_id) REFERENCES topic;
ALTER TABLE IF EXISTS task_topics_relation
    ADD CONSTRAINT FKjbxaijxjd045fy4ry2pxenrir FOREIGN KEY (task_id) REFERENCES task;
ALTER TABLE IF EXISTS task_topics_relation
    ADD CONSTRAINT FKd1esf4rvrn7eedttnfqs5dfw1 FOREIGN KEY (topic_id) REFERENCES topic;
