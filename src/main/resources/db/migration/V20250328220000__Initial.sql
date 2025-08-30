--
-- PostgreSQL database dump
--

-- Dumped from database version 16.8 (Ubuntu 16.8-1.pgdg24.04+1)
-- Dumped by pg_dump version 16.8 (Ubuntu 16.8-1.pgdg24.04+1)
--
-- SET statement_timeout = 0;
-- SET lock_timeout = 0;
-- SET idle_in_transaction_session_timeout = 0;
-- SET client_encoding = 'UTF8';
-- SET standard_conforming_strings = on;
-- SELECT pg_catalog.set_config('search_path', '', false);
-- SET check_function_bodies = false;
-- SET xmloption = content;
-- SET client_min_messages = warning;
-- SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

-- CREATE SCHEMA public;


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

-- COMMENT ON SCHEMA public IS 'standard public schema';


--
-- Name: AttachmentType; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."AttachmentType" AS ENUM (
    'image',
    'video',
    'audio',
    'file'
    );


--
-- Name: AttitudableType; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."AttitudableType" AS ENUM (
    'COMMENT',
    'QUESTION',
    'ANSWER'
    );


--
-- Name: AttitudeType; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."AttitudeType" AS ENUM (
    'UNDEFINED',
    'POSITIVE',
    'NEGATIVE'
    );


--
-- Name: AttitudeTypeNotUndefined; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."AttitudeTypeNotUndefined" AS ENUM (
    'POSITIVE',
    'NEGATIVE'
    );


--
-- Name: AvatarType; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."AvatarType" AS ENUM (
    'default',
    'predefined',
    'upload'
    );


--
-- Name: CommentCommentabletypeEnum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."CommentCommentabletypeEnum" AS ENUM (
    'ANSWER',
    'COMMENT',
    'QUESTION'
    );


--
-- Name: MaterialType; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."MaterialType" AS ENUM (
    'image',
    'file',
    'audio',
    'video'
    );


--
-- Name: UserRegisterLogType; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."UserRegisterLogType" AS ENUM (
    'RequestSuccess',
    'RequestFailDueToAlreadyRegistered',
    'RequestFailDueToInvalidOrNotSupportedEmail',
    'RequestFailDurToSecurity',
    'RequestFailDueToSendEmailFailure',
    'Success',
    'FailDueToUserExistence',
    'FailDueToWrongCodeOrExpired'
    );


--
-- Name: UserResetPasswordLogType; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."UserResetPasswordLogType" AS ENUM (
    'RequestSuccess',
    'RequestFailDueToNoneExistentEmail',
    'RequestFailDueToSecurity',
    'Success',
    'FailDueToInvalidToken',
    'FailDueToExpiredRequest',
    'FailDueToNoUser'
    );


-- SET default_tablespace = '';
--
-- SET default_table_access_method = heap;

--
-- Name: ai_conversation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ai_conversation
(
    context_id      bigint,
    created_at      timestamp(6) without time zone NOT NULL,
    deleted_at      timestamp(6) without time zone,
    id              bigint                         NOT NULL,
    owner_id        bigint                         NOT NULL,
    updated_at      timestamp(6) without time zone NOT NULL,
    conversation_id character varying(255)         NOT NULL,
    model_type      text DEFAULT 'standard'::text  NOT NULL,
    module_type     character varying(255)         NOT NULL,
    title           character varying(255)
);


--
-- Name: ai_conversation_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ai_conversation_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ai_message; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ai_message
(
    seu_consumed      numeric(10, 4),
    tokens_used       integer,
    conversation_id   bigint                         NOT NULL,
    created_at        timestamp(6) without time zone NOT NULL,
    deleted_at        timestamp(6) without time zone,
    id                bigint                         NOT NULL,
    parent_id         bigint,
    reasoning_time_ms bigint,
    updated_at        timestamp(6) without time zone NOT NULL,
    content           text                           NOT NULL,
    model_type        text DEFAULT 'standard'::text  NOT NULL,
    reasoning_content text,
    role              character varying(255)         NOT NULL,
    metadata          jsonb
);


--
-- Name: ai_message_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ai_message_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: answer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.answer
(
    id            integer                                               NOT NULL,
    created_by_id integer                                               NOT NULL,
    question_id   integer                                               NOT NULL,
    group_id      integer,
    content       text                                                  NOT NULL,
    created_at    timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at    timestamp(6) with time zone
);


--
-- Name: answer_delete_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.answer_delete_log
(
    id         integer                                               NOT NULL,
    deleter_id integer,
    answer_id  integer                                               NOT NULL,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: answer_delete_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.answer_delete_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: answer_delete_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.answer_delete_log_id_seq OWNED BY public.answer_delete_log.id;


--
-- Name: answer_favorited_by_user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.answer_favorited_by_user
(
    answer_id integer NOT NULL,
    user_id   integer NOT NULL
);


--
-- Name: answer_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.answer_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: answer_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.answer_id_seq OWNED BY public.answer.id;


--
-- Name: answer_query_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.answer_query_log
(
    id         integer                                               NOT NULL,
    viewer_id  integer,
    answer_id  integer                                               NOT NULL,
    ip         character varying                                     NOT NULL,
    user_agent character varying,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: answer_query_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.answer_query_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: answer_query_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.answer_query_log_id_seq OWNED BY public.answer_query_log.id;


--
-- Name: answer_update_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.answer_update_log
(
    id          integer                                               NOT NULL,
    updater_id  integer,
    answer_id   integer                                               NOT NULL,
    old_content text                                                  NOT NULL,
    new_content text                                                  NOT NULL,
    created_at  timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: answer_update_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.answer_update_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: answer_update_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.answer_update_log_id_seq OWNED BY public.answer_update_log.id;


--
-- Name: attachment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attachment
(
    id   integer                 NOT NULL,
    type public."AttachmentType" NOT NULL,
    url  text                    NOT NULL,
    meta json                    NOT NULL
);


--
-- Name: attachment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.attachment_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: attachment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.attachment_id_seq OWNED BY public.attachment.id;


--
-- Name: attitude; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attitude
(
    id               integer                                                  NOT NULL,
    user_id          integer                                                  NOT NULL,
    attitudable_type public."AttitudableType"                                 NOT NULL,
    attitudable_id   integer                                                  NOT NULL,
    attitude         public."AttitudeTypeNotUndefined"                        NOT NULL,
    created_at       timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at       timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: attitude_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.attitude_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: attitude_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.attitude_id_seq OWNED BY public.attitude.id;


--
-- Name: attitude_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attitude_log
(
    id               integer                                                  NOT NULL,
    user_id          integer                                                  NOT NULL,
    attitudable_type public."AttitudableType"                                 NOT NULL,
    attitudable_id   integer                                                  NOT NULL,
    attitude         public."AttitudeType"                                    NOT NULL,
    created_at       timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: attitude_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.attitude_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: attitude_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.attitude_log_id_seq OWNED BY public.attitude_log.id;


--
-- Name: avatar; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.avatar
(
    id          integer                                               NOT NULL,
    url         character varying                                     NOT NULL,
    name        character varying                                     NOT NULL,
    created_at  timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    avatar_type public."AvatarType"                                   NOT NULL,
    usage_count integer                     DEFAULT 0                 NOT NULL
);


--
-- Name: avatar_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.avatar_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: avatar_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.avatar_id_seq OWNED BY public.avatar.id;


--
-- Name: comment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.comment
(
    id               integer                                                  NOT NULL,
    commentable_type public."CommentCommentabletypeEnum"                      NOT NULL,
    commentable_id   integer                                                  NOT NULL,
    content          text                                                     NOT NULL,
    created_by_id    integer                                                  NOT NULL,
    created_at       timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at       timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at       timestamp(6) without time zone
);


--
-- Name: comment_delete_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.comment_delete_log
(
    id             integer                                               NOT NULL,
    comment_id     integer                                               NOT NULL,
    operated_by_id integer                                               NOT NULL,
    created_at     timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: comment_delete_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.comment_delete_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: comment_delete_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.comment_delete_log_id_seq OWNED BY public.comment_delete_log.id;


--
-- Name: comment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.comment_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: comment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.comment_id_seq OWNED BY public.comment.id;


--
-- Name: comment_query_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.comment_query_log
(
    id         integer                                               NOT NULL,
    comment_id integer                                               NOT NULL,
    viewer_id  integer,
    ip         character varying                                     NOT NULL,
    user_agent character varying,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: comment_query_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.comment_query_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: comment_query_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.comment_query_log_id_seq OWNED BY public.comment_query_log.id;


--
-- Name: discussion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.discussion
(
    sender_id  integer                        NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    id         bigint                         NOT NULL,
    model_id   bigint                         NOT NULL,
    parent_id  bigint,
    updated_at timestamp(6) without time zone NOT NULL,
    model_type character varying(255)         NOT NULL,
    content    jsonb                          NOT NULL,
    CONSTRAINT discussion_model_type_check CHECK (((model_type)::text = 'PROJECT'::text))
);


--
-- Name: discussion_mentioned_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.discussion_mentioned_users
(
    discussion_id bigint NOT NULL,
    user_id       bigint
);


--
-- Name: discussion_reaction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.discussion_reaction
(
    user_id          integer                        NOT NULL,
    created_at       timestamp(6) without time zone NOT NULL,
    deleted_at       timestamp(6) without time zone,
    discussion_id    bigint                         NOT NULL,
    id               bigint                         NOT NULL,
    reaction_type_id bigint                         NOT NULL,
    updated_at       timestamp(6) without time zone NOT NULL
);


--
-- Name: discussion_reaction_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.discussion_reaction_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: discussion_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.discussion_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: encryption_keys; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.encryption_keys
(
    purpose           smallint                    NOT NULL,
    created_at        timestamp(6) with time zone NOT NULL,
    related_entity_id bigint,
    id                character varying(255)      NOT NULL,
    key_value         character varying(255)      NOT NULL,
    CONSTRAINT encryption_keys_purpose_check CHECK (((purpose >= 0) AND (purpose <= 1)))
);


--
-- Name: group; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."group"
(
    id         integer                                                                           NOT NULL,
    name       character varying                                                                 NOT NULL,
    created_at timestamp(3) without time zone DEFAULT ('now'::text)::timestamp(3) with time zone NOT NULL,
    updated_at timestamp(6) with time zone    DEFAULT CURRENT_TIMESTAMP                          NOT NULL,
    deleted_at timestamp(6) with time zone
);


--
-- Name: group_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.group_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: group_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.group_id_seq OWNED BY public."group".id;


--
-- Name: group_membership; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.group_membership
(
    id         integer                                               NOT NULL,
    group_id   integer                                               NOT NULL,
    member_id  integer                                               NOT NULL,
    role       character varying                                     NOT NULL,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at timestamp(6) with time zone
);


--
-- Name: group_membership_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.group_membership_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: group_membership_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.group_membership_id_seq OWNED BY public.group_membership.id;


--
-- Name: group_profile; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.group_profile
(
    id         integer                                               NOT NULL,
    intro      character varying                                     NOT NULL,
    avatar_id  integer,
    group_id   integer                                               NOT NULL,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at timestamp(6) with time zone
);


--
-- Name: group_profile_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.group_profile_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: group_profile_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.group_profile_id_seq OWNED BY public.group_profile.id;


--
-- Name: group_question_relationship; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.group_question_relationship
(
    id          integer                                               NOT NULL,
    group_id    integer                                               NOT NULL,
    question_id integer                                               NOT NULL,
    created_at  timestamp(6) with time zone                           NOT NULL,
    updated_at  timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at  timestamp(6) with time zone
);


--
-- Name: group_question_relationship_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.group_question_relationship_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: group_question_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.group_question_relationship_id_seq OWNED BY public.group_question_relationship.id;


--
-- Name: group_target; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.group_target
(
    id                   integer                                               NOT NULL,
    group_id             integer                                               NOT NULL,
    name                 character varying                                     NOT NULL,
    intro                character varying                                     NOT NULL,
    created_at           timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at           timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at           timestamp(6) with time zone,
    started_at           date                                                  NOT NULL,
    ended_at             date                                                  NOT NULL,
    attendance_frequency character varying                                     NOT NULL
);


--
-- Name: group_target_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.group_target_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: group_target_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.group_target_id_seq OWNED BY public.group_target.id;


--
-- Name: knowledge; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.knowledge
(
    created_by_id integer                        NOT NULL,
    material_id   integer,
    created_at    timestamp(6) without time zone NOT NULL,
    deleted_at    timestamp(6) without time zone,
    discussion_id bigint,
    id            bigint                         NOT NULL,
    project_id    bigint,
    team_id       bigint                         NOT NULL,
    updated_at    timestamp(6) without time zone NOT NULL,
    description   text                           NOT NULL,
    name          character varying(255)         NOT NULL,
    source_type   character varying(255)         NOT NULL,
    type          character varying(255)         NOT NULL,
    content       jsonb                          NOT NULL,
    CONSTRAINT knowledge_source_type_check CHECK (((source_type)::text = ANY
                                                   ((ARRAY ['MANUAL'::character varying, 'FROM_DISCUSSION'::character varying])::text[]))),
    CONSTRAINT knowledge_type_check CHECK (((type)::text = ANY
                                            ((ARRAY ['MATERIAL'::character varying, 'LINK'::character varying, 'TEXT'::character varying, 'CODE'::character varying])::text[])))
);


--
-- Name: knowledge_knowledge_labels; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.knowledge_knowledge_labels
(
    knowledge_id        bigint NOT NULL,
    knowledge_labels_id bigint NOT NULL
);


--
-- Name: knowledge_label; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.knowledge_label
(
    created_at   timestamp(6) without time zone NOT NULL,
    deleted_at   timestamp(6) without time zone,
    id           bigint                         NOT NULL,
    knowledge_id bigint                         NOT NULL,
    updated_at   timestamp(6) without time zone NOT NULL,
    label        character varying(50)          NOT NULL
);


--
-- Name: knowledge_label_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.knowledge_label_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: knowledge_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.knowledge_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: material; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.material
(
    id             integer                                               NOT NULL,
    type           public."MaterialType"                                 NOT NULL,
    url            text                                                  NOT NULL,
    name           text                                                  NOT NULL,
    uploader_id    integer                                               NOT NULL,
    created_at     timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires        integer,
    download_count integer                     DEFAULT 0                 NOT NULL,
    meta           json                                                  NOT NULL
);


--
-- Name: material_bundle; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.material_bundle
(
    id             integer                                               NOT NULL,
    title          text                                                  NOT NULL,
    content        text                                                  NOT NULL,
    creator_id     integer                                               NOT NULL,
    created_at     timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at     timestamp(6) with time zone                           NOT NULL,
    rating         double precision            DEFAULT 0                 NOT NULL,
    rating_count   integer                     DEFAULT 0                 NOT NULL,
    my_rating      double precision,
    comments_count integer                     DEFAULT 0                 NOT NULL
);


--
-- Name: material_bundle_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.material_bundle_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: material_bundle_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.material_bundle_id_seq OWNED BY public.material_bundle.id;


--
-- Name: material_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.material_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: material_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.material_id_seq OWNED BY public.material.id;


--
-- Name: materialbundles_relation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.materialbundles_relation
(
    material_id integer NOT NULL,
    bundle_id   integer NOT NULL
);


--
-- Name: notification; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification
(
    read        boolean                        NOT NULL,
    receiver_id integer                        NOT NULL,
    created_at  timestamp(6) without time zone NOT NULL,
    deleted_at  timestamp(6) without time zone,
    id          bigint                         NOT NULL,
    updated_at  timestamp(6) without time zone NOT NULL,
    type        character varying(255)         NOT NULL,
    content     jsonb,
    CONSTRAINT notification_type_check CHECK (((type)::text = ANY
                                               ((ARRAY ['MENTION'::character varying, 'REPLY'::character varying, 'REACTION'::character varying, 'PROJECT_INVITE'::character varying, 'DEADLINE_REMIND'::character varying])::text[])))
);


--
-- Name: notification_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notification_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: passkey; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.passkey
(
    id            integer                                               NOT NULL,
    credential_id text                                                  NOT NULL,
    public_key    bytea                                                 NOT NULL,
    counter       integer                                               NOT NULL,
    device_type   text                                                  NOT NULL,
    backed_up     boolean                                               NOT NULL,
    transports    text,
    user_id       integer                                               NOT NULL,
    created_at    timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: passkey_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.passkey_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: passkey_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.passkey_id_seq OWNED BY public.passkey.id;


--
-- Name: project; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project
(
    archived         boolean DEFAULT false          NOT NULL,
    color_code       character varying(7)           NOT NULL,
    created_at       timestamp(6) without time zone NOT NULL,
    deleted_at       timestamp(6) without time zone,
    end_date         timestamp(6) with time zone    NOT NULL,
    external_task_id bigint,
    id               bigint                         NOT NULL,
    leader_id        bigint                         NOT NULL,
    parent_id        bigint,
    start_date       timestamp(6) with time zone    NOT NULL,
    team_id          bigint                         NOT NULL,
    updated_at       timestamp(6) without time zone NOT NULL,
    description      text                           NOT NULL,
    github_repo      character varying(255),
    name             character varying(255)         NOT NULL
);


--
-- Name: project_membership; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_membership
(
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    id         bigint                         NOT NULL,
    project_id bigint                         NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id    bigint                         NOT NULL,
    notes      text,
    role       character varying(255)         NOT NULL,
    CONSTRAINT project_membership_role_check CHECK (((role)::text = ANY
                                                     ((ARRAY ['LEADER'::character varying, 'MEMBER'::character varying, 'EXTERNAL'::character varying])::text[])))
);


--
-- Name: project_membership_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_membership_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: question; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.question
(
    id                 integer                                               NOT NULL,
    created_by_id      integer                                               NOT NULL,
    title              text                                                  NOT NULL,
    content            text                                                  NOT NULL,
    type               integer                                               NOT NULL,
    group_id           integer,
    created_at         timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at         timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at         timestamp(6) with time zone,
    bounty             integer                     DEFAULT 0                 NOT NULL,
    bounty_start_at    timestamp(6) with time zone,
    accepted_answer_id integer
);


--
-- Name: question_elasticsearch_relation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.question_elasticsearch_relation
(
    id               integer NOT NULL,
    question_id      integer NOT NULL,
    elasticsearch_id text    NOT NULL
);


--
-- Name: question_elasticsearch_relation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.question_elasticsearch_relation_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: question_elasticsearch_relation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.question_elasticsearch_relation_id_seq OWNED BY public.question_elasticsearch_relation.id;


--
-- Name: question_follower_relation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.question_follower_relation
(
    id          integer                                               NOT NULL,
    question_id integer                                               NOT NULL,
    follower_id integer                                               NOT NULL,
    created_at  timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at  timestamp(6) with time zone
);


--
-- Name: question_follower_relation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.question_follower_relation_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: question_follower_relation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.question_follower_relation_id_seq OWNED BY public.question_follower_relation.id;


--
-- Name: question_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.question_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: question_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.question_id_seq OWNED BY public.question.id;


--
-- Name: question_invitation_relation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.question_invitation_relation
(
    id          integer                                                  NOT NULL,
    question_id integer                                                  NOT NULL,
    user_id     integer                                                  NOT NULL,
    created_at  timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  timestamp(3) without time zone                           NOT NULL
);


--
-- Name: question_invitation_relation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.question_invitation_relation_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: question_invitation_relation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.question_invitation_relation_id_seq OWNED BY public.question_invitation_relation.id;


--
-- Name: question_query_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.question_query_log
(
    id          integer                                               NOT NULL,
    viewer_id   integer,
    question_id integer                                               NOT NULL,
    ip          character varying                                     NOT NULL,
    user_agent  character varying,
    created_at  timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: question_query_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.question_query_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: question_query_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.question_query_log_id_seq OWNED BY public.question_query_log.id;


--
-- Name: question_search_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.question_search_log
(
    id                integer                                               NOT NULL,
    keywords          character varying                                     NOT NULL,
    first_question_id integer,
    page_size         integer                                               NOT NULL,
    result            character varying                                     NOT NULL,
    duration          double precision                                      NOT NULL,
    searcher_id       integer,
    ip                character varying                                     NOT NULL,
    user_agent        character varying,
    created_at        timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: question_search_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.question_search_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: question_search_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.question_search_log_id_seq OWNED BY public.question_search_log.id;


--
-- Name: question_topic_relation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.question_topic_relation
(
    id            integer                                               NOT NULL,
    question_id   integer                                               NOT NULL,
    topic_id      integer                                               NOT NULL,
    created_by_id integer                                               NOT NULL,
    created_at    timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at    timestamp(6) with time zone
);


--
-- Name: question_topic_relation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.question_topic_relation_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: question_topic_relation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.question_topic_relation_id_seq OWNED BY public.question_topic_relation.id;


--
-- Name: reaction_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reaction_type
(
    display_order integer                        NOT NULL,
    is_active     boolean                        NOT NULL,
    created_at    timestamp(6) without time zone NOT NULL,
    deleted_at    timestamp(6) without time zone,
    id            bigint                         NOT NULL,
    updated_at    timestamp(6) without time zone NOT NULL,
    code          character varying(32)          NOT NULL,
    name          character varying(64)          NOT NULL,
    description   character varying(255)
);


--
-- Name: reaction_type_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.reaction_type_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: session; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.session
(
    id                integer                                               NOT NULL,
    valid_until       timestamp(6) with time zone                           NOT NULL,
    revoked           boolean                                               NOT NULL,
    user_id           integer                                               NOT NULL,
    "authorization"   text                                                  NOT NULL,
    last_refreshed_at bigint                                                NOT NULL,
    created_at        timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: session_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.session_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: session_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.session_id_seq OWNED BY public.session.id;


--
-- Name: session_refresh_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.session_refresh_log
(
    id                integer                                               NOT NULL,
    session_id        integer                                               NOT NULL,
    old_refresh_token text                                                  NOT NULL,
    new_refresh_token text                                                  NOT NULL,
    access_token      text                                                  NOT NULL,
    created_at        timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: session_refresh_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.session_refresh_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: session_refresh_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.session_refresh_log_id_seq OWNED BY public.session_refresh_log.id;


--
-- Name: space; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.space
(
    avatar_id      integer                        NOT NULL,
    created_at     timestamp(6) without time zone NOT NULL,
    deleted_at     timestamp(6) without time zone,
    id             bigint                         NOT NULL,
    updated_at     timestamp(6) without time zone NOT NULL,
    description    text                           NOT NULL,
    name           character varying(255)         NOT NULL,
    enable_rank    boolean DEFAULT false          NOT NULL,
    intro          character varying(255)         NOT NULL,
    announcements  text    DEFAULT '{}'::text     NOT NULL,
    task_templates text    DEFAULT '{}'::text     NOT NULL
);


--
-- Name: space_admin_relation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.space_admin_relation
(
    role       smallint                       NOT NULL,
    user_id    integer                        NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    id         bigint                         NOT NULL,
    space_id   bigint                         NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT space_admin_relation_role_check CHECK (((role >= 0) AND (role <= 1)))
);


--
-- Name: space_admin_relation_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.space_admin_relation_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: space_classification_topics_relation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.space_classification_topics_relation
(
    topic_id   integer                        NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    id         bigint                         NOT NULL,
    space_id   bigint                         NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);


--
-- Name: space_classification_topics_relation_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.space_classification_topics_relation_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: space_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.space_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: space_user_rank; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.space_user_rank
(
    rank       integer                        NOT NULL,
    user_id    integer                        NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    id         bigint                         NOT NULL,
    space_id   bigint                         NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);


--
-- Name: space_user_rank_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.space_user_rank_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: task; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.task
(
    creator_id        integer                        NOT NULL,
    editable          boolean                        NOT NULL,
    resubmittable     boolean                        NOT NULL,
    submitter_type    smallint                       NOT NULL,
    created_at        timestamp(6) without time zone NOT NULL,
    deadline          timestamp(6) without time zone,
    deleted_at        timestamp(6) without time zone,
    id                bigint                         NOT NULL,
    space_id          bigint,
    team_id           bigint,
    updated_at        timestamp(6) without time zone NOT NULL,
    description       text                           NOT NULL,
    name              character varying(255)         NOT NULL,
    rank              integer,
    intro             character varying(255)         NOT NULL,
    approved          smallint                       NOT NULL,
    default_deadline  bigint  DEFAULT 30             NOT NULL,
    reject_reason     character varying(255)         NOT NULL,
    participant_limit integer DEFAULT 2,
    require_real_name boolean DEFAULT false          NOT NULL,
    CONSTRAINT approved_check CHECK (((approved >= 0) AND (approved <= 2))),
    CONSTRAINT task_submitter_type_check CHECK (((submitter_type >= 0) AND (submitter_type <= 1)))
);


--
-- Name: task_ai_advice; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.task_ai_advice
(
    created_at       timestamp(6) without time zone NOT NULL,
    id               bigint                         NOT NULL,
    task_id          bigint                         NOT NULL,
    updated_at       timestamp(6) without time zone NOT NULL,
    model_hash       character varying(64)          NOT NULL,
    knowledge_fields text,
    learning_paths   text,
    methodology      text,
    raw_response     text,
    status           character varying(255)         NOT NULL,
    team_tips        text,
    topic_summary    text,
    CONSTRAINT task_ai_advice_status_check CHECK (((status)::text = ANY
                                                   ((ARRAY ['PENDING'::character varying, 'PROCESSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: task_ai_advice_context; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.task_ai_advice_context
(
    section_index integer,
    created_at    timestamp(6) without time zone NOT NULL,
    deleted_at    timestamp(6) without time zone,
    id            bigint                         NOT NULL,
    task_id       bigint                         NOT NULL,
    updated_at    timestamp(6) without time zone NOT NULL,
    section       character varying(255)
);


--
-- Name: task_ai_advice_context_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.task_ai_advice_context_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: task_ai_advice_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.task_ai_advice
    ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
        SEQUENCE NAME public.task_ai_advice_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1
        );


--
-- Name: task_membership; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.task_membership
(
    created_at         timestamp(6) without time zone       NOT NULL,
    deleted_at         timestamp(6) without time zone,
    id                 bigint                               NOT NULL,
    member_id          bigint                               NOT NULL,
    task_id            bigint                               NOT NULL,
    updated_at         timestamp(6) without time zone       NOT NULL,
    deadline           timestamp(6) without time zone,
    approved           smallint               DEFAULT 2     NOT NULL,
    apply_reason       character varying(255) DEFAULT ''::character varying,
    class_name         character varying(255) DEFAULT ''::character varying,
    email              character varying(255) DEFAULT ''::character varying,
    grade              character varying(255) DEFAULT ''::character varying,
    major              character varying(255) DEFAULT ''::character varying,
    personal_advantage character varying(255) DEFAULT ''::character varying,
    phone              character varying(255) DEFAULT ''::character varying,
    real_name          character varying(255) DEFAULT ''::character varying,
    remark             character varying(255) DEFAULT ''::character varying,
    student_id         character varying(255) DEFAULT ''::character varying,
    encrypted          boolean                DEFAULT false,
    is_team            boolean                DEFAULT false NOT NULL,
    encryption_key_id  character varying(255),
    CONSTRAINT task_membership_approved_check CHECK (((approved >= 0) AND (approved <= 2)))
);


--
-- Name: task_membership_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.task_membership_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: task_membership_team_members; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.task_membership_team_members
(
    encrypted          boolean DEFAULT false,
    member_id          bigint NOT NULL,
    task_membership_id bigint NOT NULL,
    class_name         character varying(255),
    grade              character varying(255),
    major              character varying(255),
    real_name          character varying(255),
    student_id         character varying(255)
);


--
-- Name: task_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.task_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: task_submission; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.task_submission
(
    submitter_id  integer                        NOT NULL,
    version       integer                        NOT NULL,
    created_at    timestamp(6) without time zone NOT NULL,
    deleted_at    timestamp(6) without time zone,
    id            bigint                         NOT NULL,
    membership_id bigint                         NOT NULL,
    updated_at    timestamp(6) without time zone NOT NULL
);


--
-- Name: task_submission_entry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.task_submission_entry
(
    content_attachment_id integer,
    index                 integer                        NOT NULL,
    created_at            timestamp(6) without time zone NOT NULL,
    deleted_at            timestamp(6) without time zone,
    id                    bigint                         NOT NULL,
    task_submission_id    bigint                         NOT NULL,
    updated_at            timestamp(6) without time zone NOT NULL,
    content_text          character varying(255)
);


--
-- Name: task_submission_entry_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.task_submission_entry_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: task_submission_review; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.task_submission_review
(
    accepted      boolean                        NOT NULL,
    score         integer                        NOT NULL,
    created_at    timestamp(6) without time zone NOT NULL,
    deleted_at    timestamp(6) without time zone,
    id            bigint                         NOT NULL,
    submission_id bigint                         NOT NULL,
    updated_at    timestamp(6) without time zone NOT NULL,
    comment       character varying(255)         NOT NULL
);


--
-- Name: task_submission_review_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.task_submission_review_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: task_submission_schema; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.task_submission_schema
(
    index       integer                NOT NULL,
    type        smallint               NOT NULL,
    task_id     bigint                 NOT NULL,
    description character varying(255) NOT NULL,
    CONSTRAINT task_submission_schema_type_check CHECK (((type >= 0) AND (type <= 1)))
);


--
-- Name: task_submission_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.task_submission_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: task_topics_relation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.task_topics_relation
(
    topic_id   integer                        NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    id         bigint                         NOT NULL,
    task_id    bigint                         NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);


--
-- Name: task_topics_relation_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.task_topics_relation_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: team; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.team
(
    avatar_id   integer                        NOT NULL,
    created_at  timestamp(6) without time zone NOT NULL,
    deleted_at  timestamp(6) without time zone,
    id          bigint                         NOT NULL,
    updated_at  timestamp(6) without time zone NOT NULL,
    description text                           NOT NULL,
    name        character varying(255)         NOT NULL,
    intro       character varying(255)         NOT NULL
);


--
-- Name: team_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.team_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: team_user_relation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.team_user_relation
(
    role       smallint                       NOT NULL,
    user_id    integer                        NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    id         bigint                         NOT NULL,
    team_id    bigint                         NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT team_user_relation_role_check CHECK (((role >= 0) AND (role <= 2)))
);


--
-- Name: team_user_relation_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.team_user_relation_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: topic; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.topic
(
    id            integer                                               NOT NULL,
    name          character varying                                     NOT NULL,
    created_by_id integer                                               NOT NULL,
    created_at    timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at    timestamp(6) with time zone
);


--
-- Name: topic_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.topic_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: topic_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.topic_id_seq OWNED BY public.topic.id;


--
-- Name: topic_search_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.topic_search_log
(
    id             integer                                               NOT NULL,
    keywords       character varying                                     NOT NULL,
    first_topic_id integer,
    page_size      integer                                               NOT NULL,
    result         character varying                                     NOT NULL,
    duration       double precision                                      NOT NULL,
    searcher_id    integer,
    ip             character varying                                     NOT NULL,
    user_agent     character varying,
    created_at     timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: topic_search_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.topic_search_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: topic_search_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.topic_search_log_id_seq OWNED BY public.topic_search_log.id;


--
-- Name: user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."user"
(
    id                       integer                                               NOT NULL,
    username                 character varying                                     NOT NULL,
    hashed_password          character varying,
    email                    character varying                                     NOT NULL,
    created_at               timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at               timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at               timestamp(6) with time zone,
    totp_always_required     boolean                     DEFAULT false             NOT NULL,
    totp_enabled             boolean                     DEFAULT false             NOT NULL,
    totp_secret              character varying(64),
    srp_salt                 character varying(500),
    srp_upgraded             boolean                     DEFAULT false             NOT NULL,
    srp_verifier             character varying(1000),
    last_password_changed_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: user_ai_quota; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_ai_quota
(
    daily_seu_quota    numeric(10, 4),
    remaining_seu      numeric(10, 4),
    total_seu_consumed numeric(10, 4),
    created_at         timestamp(6) without time zone NOT NULL,
    deleted_at         timestamp(6) without time zone,
    id                 bigint                         NOT NULL,
    last_reset_time    timestamp(6) without time zone,
    updated_at         timestamp(6) without time zone NOT NULL,
    user_id            bigint
);


--
-- Name: user_ai_quota_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_ai_quota_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_backup_code; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_backup_code
(
    id         integer                                               NOT NULL,
    user_id    integer                                               NOT NULL,
    code_hash  character varying(128)                                NOT NULL,
    used       boolean                     DEFAULT false             NOT NULL,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: user_backup_code_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_backup_code_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_backup_code_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_backup_code_id_seq OWNED BY public.user_backup_code.id;


--
-- Name: user_following_relationship; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_following_relationship
(
    id          integer                                               NOT NULL,
    followee_id integer                                               NOT NULL,
    follower_id integer                                               NOT NULL,
    created_at  timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at  timestamp(6) with time zone
);


--
-- Name: user_following_relationship_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_following_relationship_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_following_relationship_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_following_relationship_id_seq OWNED BY public.user_following_relationship.id;


--
-- Name: user_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_id_seq OWNED BY public."user".id;


--
-- Name: user_login_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_login_log
(
    id         integer                                               NOT NULL,
    user_id    integer                                               NOT NULL,
    ip         character varying                                     NOT NULL,
    user_agent character varying,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: user_login_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_login_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_login_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_login_log_id_seq OWNED BY public.user_login_log.id;


--
-- Name: user_profile; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_profile
(
    id         integer                                               NOT NULL,
    user_id    integer                                               NOT NULL,
    nickname   character varying                                     NOT NULL,
    avatar_id  integer                                               NOT NULL,
    intro      character varying                                     NOT NULL,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at timestamp(6) with time zone
);


--
-- Name: user_profile_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_profile_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_profile_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_profile_id_seq OWNED BY public.user_profile.id;


--
-- Name: user_profile_query_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_profile_query_log
(
    id         integer                                               NOT NULL,
    viewer_id  integer,
    viewee_id  integer                                               NOT NULL,
    ip         character varying                                     NOT NULL,
    user_agent character varying,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: user_profile_query_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_profile_query_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_profile_query_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_profile_query_log_id_seq OWNED BY public.user_profile_query_log.id;


--
-- Name: user_real_name_access_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_real_name_access_logs
(
    accessor_id      bigint                         NOT NULL,
    created_at       timestamp(6) without time zone NOT NULL,
    deleted_at       timestamp(6) without time zone,
    id               bigint                         NOT NULL,
    module_entity_id bigint,
    target_id        bigint                         NOT NULL,
    updated_at       timestamp(6) without time zone NOT NULL,
    access_reason    character varying(255)         NOT NULL,
    access_type      character varying(255)         NOT NULL,
    ip_address       character varying(255)         NOT NULL,
    module_type      character varying(255),
    CONSTRAINT user_real_name_access_logs_access_type_check CHECK (((access_type)::text = ANY
                                                                    ((ARRAY ['VIEW'::character varying, 'EXPORT'::character varying])::text[]))),
    CONSTRAINT user_real_name_access_logs_module_type_check CHECK (((module_type)::text = 'TASK'::text))
);


--
-- Name: user_real_name_access_logs_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_real_name_access_logs_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_real_name_identities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_real_name_identities
(
    encrypted         boolean DEFAULT false          NOT NULL,
    created_at        timestamp(6) without time zone NOT NULL,
    deleted_at        timestamp(6) without time zone,
    id                bigint                         NOT NULL,
    updated_at        timestamp(6) without time zone NOT NULL,
    user_id           bigint                         NOT NULL,
    class_name        character varying(255)         NOT NULL,
    encryption_key_id character varying(255)         NOT NULL,
    grade             character varying(255)         NOT NULL,
    major             character varying(255)         NOT NULL,
    real_name         character varying(255)         NOT NULL,
    student_id        character varying(255)         NOT NULL
);


--
-- Name: user_real_name_identities_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_real_name_identities_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_register_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_register_log
(
    id         integer                                               NOT NULL,
    email      character varying                                     NOT NULL,
    type       public."UserRegisterLogType"                          NOT NULL,
    ip         character varying                                     NOT NULL,
    user_agent character varying,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: user_register_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_register_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_register_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_register_log_id_seq OWNED BY public.user_register_log.id;


--
-- Name: user_register_request; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_register_request
(
    id         integer                                               NOT NULL,
    email      character varying                                     NOT NULL,
    code       character varying                                     NOT NULL,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: user_register_request_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_register_request_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_register_request_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_register_request_id_seq OWNED BY public.user_register_request.id;


--
-- Name: user_reset_password_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_reset_password_log
(
    id         integer                                               NOT NULL,
    user_id    integer,
    type       public."UserResetPasswordLogType"                     NOT NULL,
    ip         character varying                                     NOT NULL,
    user_agent character varying,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: user_reset_password_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_reset_password_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_reset_password_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_reset_password_log_id_seq OWNED BY public.user_reset_password_log.id;


--
-- Name: user_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_role
(
    id      bigint                 NOT NULL,
    user_id bigint                 NOT NULL,
    role    character varying(255) NOT NULL,
    CONSTRAINT user_role_role_check CHECK (((role)::text = ANY
                                            ((ARRAY ['SUPER_ADMIN'::character varying, 'ADMIN'::character varying, 'MODERATOR'::character varying, 'USER'::character varying])::text[])))
);


--
-- Name: user_role_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.user_role
    ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
        SEQUENCE NAME public.user_role_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1
        );


--
-- Name: user_seu_consumption; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_seu_consumption
(
    is_cached       boolean,
    seu_consumed    numeric(10, 4),
    tokens_used     integer,
    cache_expire_at timestamp(6) without time zone,
    created_at      timestamp(6) without time zone NOT NULL,
    deleted_at      timestamp(6) without time zone,
    id              bigint                         NOT NULL,
    updated_at      timestamp(6) without time zone NOT NULL,
    user_id         bigint,
    cache_key       character varying(255),
    request_id      character varying(255),
    resource_type   character varying(255),
    CONSTRAINT user_seu_consumption_resource_type_check CHECK (((resource_type)::text = ANY
                                                                ((ARRAY ['LIGHTWEIGHT'::character varying, 'STANDARD'::character varying, 'ADVANCED'::character varying])::text[])))
);


--
-- Name: user_seu_consumption_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_seu_consumption_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: answer id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer
    ALTER COLUMN id SET DEFAULT nextval('public.answer_id_seq'::regclass);


--
-- Name: answer_delete_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_delete_log
    ALTER COLUMN id SET DEFAULT nextval('public.answer_delete_log_id_seq'::regclass);


--
-- Name: answer_query_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_query_log
    ALTER COLUMN id SET DEFAULT nextval('public.answer_query_log_id_seq'::regclass);


--
-- Name: answer_update_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_update_log
    ALTER COLUMN id SET DEFAULT nextval('public.answer_update_log_id_seq'::regclass);


--
-- Name: attachment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attachment
    ALTER COLUMN id SET DEFAULT nextval('public.attachment_id_seq'::regclass);


--
-- Name: attitude id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attitude
    ALTER COLUMN id SET DEFAULT nextval('public.attitude_id_seq'::regclass);


--
-- Name: attitude_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attitude_log
    ALTER COLUMN id SET DEFAULT nextval('public.attitude_log_id_seq'::regclass);


--
-- Name: avatar id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avatar
    ALTER COLUMN id SET DEFAULT nextval('public.avatar_id_seq'::regclass);


--
-- Name: comment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comment
    ALTER COLUMN id SET DEFAULT nextval('public.comment_id_seq'::regclass);


--
-- Name: comment_delete_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comment_delete_log
    ALTER COLUMN id SET DEFAULT nextval('public.comment_delete_log_id_seq'::regclass);


--
-- Name: comment_query_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comment_query_log
    ALTER COLUMN id SET DEFAULT nextval('public.comment_query_log_id_seq'::regclass);


--
-- Name: group id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."group"
    ALTER COLUMN id SET DEFAULT nextval('public.group_id_seq'::regclass);


--
-- Name: group_membership id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_membership
    ALTER COLUMN id SET DEFAULT nextval('public.group_membership_id_seq'::regclass);


--
-- Name: group_profile id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_profile
    ALTER COLUMN id SET DEFAULT nextval('public.group_profile_id_seq'::regclass);


--
-- Name: group_question_relationship id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_question_relationship
    ALTER COLUMN id SET DEFAULT nextval('public.group_question_relationship_id_seq'::regclass);


--
-- Name: group_target id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_target
    ALTER COLUMN id SET DEFAULT nextval('public.group_target_id_seq'::regclass);


--
-- Name: material id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material
    ALTER COLUMN id SET DEFAULT nextval('public.material_id_seq'::regclass);


--
-- Name: material_bundle id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_bundle
    ALTER COLUMN id SET DEFAULT nextval('public.material_bundle_id_seq'::regclass);


--
-- Name: passkey id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.passkey
    ALTER COLUMN id SET DEFAULT nextval('public.passkey_id_seq'::regclass);


--
-- Name: question id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question
    ALTER COLUMN id SET DEFAULT nextval('public.question_id_seq'::regclass);


--
-- Name: question_elasticsearch_relation id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_elasticsearch_relation
    ALTER COLUMN id SET DEFAULT nextval('public.question_elasticsearch_relation_id_seq'::regclass);


--
-- Name: question_follower_relation id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_follower_relation
    ALTER COLUMN id SET DEFAULT nextval('public.question_follower_relation_id_seq'::regclass);


--
-- Name: question_invitation_relation id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_invitation_relation
    ALTER COLUMN id SET DEFAULT nextval('public.question_invitation_relation_id_seq'::regclass);


--
-- Name: question_query_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_query_log
    ALTER COLUMN id SET DEFAULT nextval('public.question_query_log_id_seq'::regclass);


--
-- Name: question_search_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_search_log
    ALTER COLUMN id SET DEFAULT nextval('public.question_search_log_id_seq'::regclass);


--
-- Name: question_topic_relation id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_topic_relation
    ALTER COLUMN id SET DEFAULT nextval('public.question_topic_relation_id_seq'::regclass);


--
-- Name: session id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session
    ALTER COLUMN id SET DEFAULT nextval('public.session_id_seq'::regclass);


--
-- Name: session_refresh_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_refresh_log
    ALTER COLUMN id SET DEFAULT nextval('public.session_refresh_log_id_seq'::regclass);


--
-- Name: topic id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topic
    ALTER COLUMN id SET DEFAULT nextval('public.topic_id_seq'::regclass);


--
-- Name: topic_search_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topic_search_log
    ALTER COLUMN id SET DEFAULT nextval('public.topic_search_log_id_seq'::regclass);


--
-- Name: user id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."user"
    ALTER COLUMN id SET DEFAULT nextval('public.user_id_seq'::regclass);


--
-- Name: user_backup_code id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_backup_code
    ALTER COLUMN id SET DEFAULT nextval('public.user_backup_code_id_seq'::regclass);


--
-- Name: user_following_relationship id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_following_relationship
    ALTER COLUMN id SET DEFAULT nextval('public.user_following_relationship_id_seq'::regclass);


--
-- Name: user_login_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_login_log
    ALTER COLUMN id SET DEFAULT nextval('public.user_login_log_id_seq'::regclass);


--
-- Name: user_profile id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile
    ALTER COLUMN id SET DEFAULT nextval('public.user_profile_id_seq'::regclass);


--
-- Name: user_profile_query_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile_query_log
    ALTER COLUMN id SET DEFAULT nextval('public.user_profile_query_log_id_seq'::regclass);


--
-- Name: user_register_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_register_log
    ALTER COLUMN id SET DEFAULT nextval('public.user_register_log_id_seq'::regclass);


--
-- Name: user_register_request id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_register_request
    ALTER COLUMN id SET DEFAULT nextval('public.user_register_request_id_seq'::regclass);


--
-- Name: user_reset_password_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_reset_password_log
    ALTER COLUMN id SET DEFAULT nextval('public.user_reset_password_log_id_seq'::regclass);


--
-- Name: comment PK_0b0e4bbc8415ec426f87f3a88e2; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comment
    ADD CONSTRAINT "PK_0b0e4bbc8415ec426f87f3a88e2" PRIMARY KEY (id);


--
-- Name: question PK_21e5786aa0ea704ae185a79b2d5; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question
    ADD CONSTRAINT "PK_21e5786aa0ea704ae185a79b2d5" PRIMARY KEY (id);


--
-- Name: group PK_256aa0fda9b1de1a73ee0b7106b; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."group"
    ADD CONSTRAINT "PK_256aa0fda9b1de1a73ee0b7106b" PRIMARY KEY (id);


--
-- Name: question_query_log PK_2876061262a774e4aba4daaaae4; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_query_log
    ADD CONSTRAINT "PK_2876061262a774e4aba4daaaae4" PRIMARY KEY (id);


--
-- Name: group_profile PK_2a62b59d1bf8a3191c992e8daf4; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_profile
    ADD CONSTRAINT "PK_2a62b59d1bf8a3191c992e8daf4" PRIMARY KEY (id);


--
-- Name: topic PK_33aa4ecb4e4f20aa0157ea7ef61; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topic
    ADD CONSTRAINT "PK_33aa4ecb4e4f20aa0157ea7ef61" PRIMARY KEY (id);


--
-- Name: user_register_log PK_3596a6f74bd2a80be930f6d1e39; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_register_log
    ADD CONSTRAINT "PK_3596a6f74bd2a80be930f6d1e39" PRIMARY KEY (id);


--
-- Name: user_following_relationship PK_3b0199015f8814633fc710ff09d; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_following_relationship
    ADD CONSTRAINT "PK_3b0199015f8814633fc710ff09d" PRIMARY KEY (id);


--
-- Name: user_reset_password_log PK_3ee4f25e7f4f1d5a9bd9817b62b; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_reset_password_log
    ADD CONSTRAINT "PK_3ee4f25e7f4f1d5a9bd9817b62b" PRIMARY KEY (id);


--
-- Name: topic_search_log PK_41a432f5f993017b2502c73c78e; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topic_search_log
    ADD CONSTRAINT "PK_41a432f5f993017b2502c73c78e" PRIMARY KEY (id);


--
-- Name: comment_delete_log PK_429889b4bdc646cb80ef8bc1814; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comment_delete_log
    ADD CONSTRAINT "PK_429889b4bdc646cb80ef8bc1814" PRIMARY KEY (id);


--
-- Name: group_question_relationship PK_47ee7be0b0f0e51727012382922; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_question_relationship
    ADD CONSTRAINT "PK_47ee7be0b0f0e51727012382922" PRIMARY KEY (id);


--
-- Name: answer_query_log PK_4f65c4804d0693f458a716aa72c; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_query_log
    ADD CONSTRAINT "PK_4f65c4804d0693f458a716aa72c" PRIMARY KEY (id);


--
-- Name: answer_favorited_by_user PK_5a857fe93c44fdb538ec5aa4771; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_favorited_by_user
    ADD CONSTRAINT "PK_5a857fe93c44fdb538ec5aa4771" PRIMARY KEY (answer_id, user_id);


--
-- Name: answer_update_log PK_5ae381609b7ae9f2319fe26031f; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_update_log
    ADD CONSTRAINT "PK_5ae381609b7ae9f2319fe26031f" PRIMARY KEY (id);


--
-- Name: question_follower_relation PK_5f5ce2e314f975612a13d601362; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_follower_relation
    ADD CONSTRAINT "PK_5f5ce2e314f975612a13d601362" PRIMARY KEY (id);


--
-- Name: question_search_log PK_6f41b41474cf92c67a7da97384c; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_search_log
    ADD CONSTRAINT "PK_6f41b41474cf92c67a7da97384c" PRIMARY KEY (id);


--
-- Name: answer PK_9232db17b63fb1e94f97e5c224f; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer
    ADD CONSTRAINT "PK_9232db17b63fb1e94f97e5c224f" PRIMARY KEY (id);


--
-- Name: user_profile_query_log PK_9aeff7c959703fad866e9ad581a; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile_query_log
    ADD CONSTRAINT "PK_9aeff7c959703fad866e9ad581a" PRIMARY KEY (id);


--
-- Name: comment_query_log PK_afbfb3d92cbf55c99cb6bdcd58f; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comment_query_log
    ADD CONSTRAINT "PK_afbfb3d92cbf55c99cb6bdcd58f" PRIMARY KEY (id);


--
-- Name: group_membership PK_b631623cf04fa74513b975e7059; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_membership
    ADD CONSTRAINT "PK_b631623cf04fa74513b975e7059" PRIMARY KEY (id);


--
-- Name: question_topic_relation PK_c50ec8a9ac6c3007f0861e4a383; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_topic_relation
    ADD CONSTRAINT "PK_c50ec8a9ac6c3007f0861e4a383" PRIMARY KEY (id);


--
-- Name: user PK_cace4a159ff9f2512dd42373760; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."user"
    ADD CONSTRAINT "PK_cace4a159ff9f2512dd42373760" PRIMARY KEY (id);


--
-- Name: user_register_request PK_cdf2d880551e43d9362ddd37ae0; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_register_request
    ADD CONSTRAINT "PK_cdf2d880551e43d9362ddd37ae0" PRIMARY KEY (id);


--
-- Name: group_target PK_f1671a42b347bd96ce6595f91ee; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_target
    ADD CONSTRAINT "PK_f1671a42b347bd96ce6595f91ee" PRIMARY KEY (id);


--
-- Name: answer_delete_log PK_f1696d27f69ec9c6133a12aadcf; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_delete_log
    ADD CONSTRAINT "PK_f1696d27f69ec9c6133a12aadcf" PRIMARY KEY (id);


--
-- Name: user_profile PK_f44d0cd18cfd80b0fed7806c3b7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile
    ADD CONSTRAINT "PK_f44d0cd18cfd80b0fed7806c3b7" PRIMARY KEY (id);


--
-- Name: session PK_f55da76ac1c3ac420f444d2ff11; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session
    ADD CONSTRAINT "PK_f55da76ac1c3ac420f444d2ff11" PRIMARY KEY (id);


--
-- Name: user_login_log PK_f8db79b1af1f385db4f45a2222e; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_login_log
    ADD CONSTRAINT "PK_f8db79b1af1f385db4f45a2222e" PRIMARY KEY (id);


--
-- Name: session_refresh_log PK_f8f46c039b0955a7df6ad6631d7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_refresh_log
    ADD CONSTRAINT "PK_f8f46c039b0955a7df6ad6631d7" PRIMARY KEY (id);


--
-- Name: passkey PK_passkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.passkey
    ADD CONSTRAINT "PK_passkey" PRIMARY KEY (id);


--
-- Name: ai_conversation ai_conversation_conversation_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_conversation
    ADD CONSTRAINT ai_conversation_conversation_id_key UNIQUE (conversation_id);


--
-- Name: ai_conversation ai_conversation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_conversation
    ADD CONSTRAINT ai_conversation_pkey PRIMARY KEY (id);


--
-- Name: ai_message ai_message_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_message
    ADD CONSTRAINT ai_message_pkey PRIMARY KEY (id);


--
-- Name: attachment attachment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT attachment_pkey PRIMARY KEY (id);


--
-- Name: attitude_log attitude_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attitude_log
    ADD CONSTRAINT attitude_log_pkey PRIMARY KEY (id);


--
-- Name: attitude attitude_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attitude
    ADD CONSTRAINT attitude_pkey PRIMARY KEY (id);


--
-- Name: avatar avatar_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avatar
    ADD CONSTRAINT avatar_pkey PRIMARY KEY (id);


--
-- Name: discussion discussion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discussion
    ADD CONSTRAINT discussion_pkey PRIMARY KEY (id);


--
-- Name: discussion_reaction discussion_reaction_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discussion_reaction
    ADD CONSTRAINT discussion_reaction_pkey PRIMARY KEY (id);


--
-- Name: encryption_keys encryption_keys_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.encryption_keys
    ADD CONSTRAINT encryption_keys_pkey PRIMARY KEY (id);


--
-- Name: reaction_type idx_reaction_type_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reaction_type
    ADD CONSTRAINT idx_reaction_type_code UNIQUE (code);


--
-- Name: knowledge_knowledge_labels knowledge_knowledge_labels_knowledge_labels_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_knowledge_labels
    ADD CONSTRAINT knowledge_knowledge_labels_knowledge_labels_id_key UNIQUE (knowledge_labels_id);


--
-- Name: knowledge_label knowledge_label_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_label
    ADD CONSTRAINT knowledge_label_pkey PRIMARY KEY (id);


--
-- Name: knowledge knowledge_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge
    ADD CONSTRAINT knowledge_pkey PRIMARY KEY (id);


--
-- Name: material_bundle material_bundle_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_bundle
    ADD CONSTRAINT material_bundle_pkey PRIMARY KEY (id);


--
-- Name: material material_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material
    ADD CONSTRAINT material_pkey PRIMARY KEY (id);


--
-- Name: materialbundles_relation materialbundles_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materialbundles_relation
    ADD CONSTRAINT materialbundles_relation_pkey PRIMARY KEY (material_id, bundle_id);


--
-- Name: notification notification_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);


--
-- Name: project_membership project_membership_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_membership
    ADD CONSTRAINT project_membership_pkey PRIMARY KEY (id);


--
-- Name: project_membership project_membership_project_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_membership
    ADD CONSTRAINT project_membership_project_id_user_id_key UNIQUE (project_id, user_id);


--
-- Name: project project_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_pkey PRIMARY KEY (id);


--
-- Name: question_elasticsearch_relation question_elasticsearch_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_elasticsearch_relation
    ADD CONSTRAINT question_elasticsearch_relation_pkey PRIMARY KEY (id);


--
-- Name: question_invitation_relation question_invitation_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_invitation_relation
    ADD CONSTRAINT question_invitation_relation_pkey PRIMARY KEY (id);


--
-- Name: reaction_type reaction_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reaction_type
    ADD CONSTRAINT reaction_type_pkey PRIMARY KEY (id);


--
-- Name: space_admin_relation space_admin_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.space_admin_relation
    ADD CONSTRAINT space_admin_relation_pkey PRIMARY KEY (id);


--
-- Name: space_classification_topics_relation space_classification_topics_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.space_classification_topics_relation
    ADD CONSTRAINT space_classification_topics_relation_pkey PRIMARY KEY (id);


--
-- Name: space space_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.space
    ADD CONSTRAINT space_pkey PRIMARY KEY (id);


--
-- Name: space_user_rank space_user_rank_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.space_user_rank
    ADD CONSTRAINT space_user_rank_pkey PRIMARY KEY (id);


--
-- Name: task_ai_advice_context task_ai_advice_context_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_ai_advice_context
    ADD CONSTRAINT task_ai_advice_context_pkey PRIMARY KEY (id);


--
-- Name: task_ai_advice task_ai_advice_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_ai_advice
    ADD CONSTRAINT task_ai_advice_pkey PRIMARY KEY (id);


--
-- Name: task_ai_advice task_ai_advice_task_id_model_hash_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_ai_advice
    ADD CONSTRAINT task_ai_advice_task_id_model_hash_key UNIQUE (task_id, model_hash);


--
-- Name: task_membership task_membership_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_membership
    ADD CONSTRAINT task_membership_pkey PRIMARY KEY (id);


--
-- Name: task task_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task
    ADD CONSTRAINT task_pkey PRIMARY KEY (id);


--
-- Name: task_submission_entry task_submission_entry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_submission_entry
    ADD CONSTRAINT task_submission_entry_pkey PRIMARY KEY (id);


--
-- Name: task_submission task_submission_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_submission
    ADD CONSTRAINT task_submission_pkey PRIMARY KEY (id);


--
-- Name: task_submission_review task_submission_review_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_submission_review
    ADD CONSTRAINT task_submission_review_pkey PRIMARY KEY (id);


--
-- Name: task_topics_relation task_topics_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_topics_relation
    ADD CONSTRAINT task_topics_relation_pkey PRIMARY KEY (id);


--
-- Name: team team_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.team
    ADD CONSTRAINT team_pkey PRIMARY KEY (id);


--
-- Name: team_user_relation team_user_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.team_user_relation
    ADD CONSTRAINT team_user_relation_pkey PRIMARY KEY (id);


--
-- Name: discussion_reaction uk_discussion_reaction_user_type; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discussion_reaction
    ADD CONSTRAINT uk_discussion_reaction_user_type UNIQUE (discussion_id, user_id, reaction_type_id);


--
-- Name: task_ai_advice_context uk_task_section_index; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_ai_advice_context
    ADD CONSTRAINT uk_task_section_index UNIQUE (task_id, section, section_index);


--
-- Name: user_ai_quota user_ai_quota_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_ai_quota
    ADD CONSTRAINT user_ai_quota_pkey PRIMARY KEY (id);


--
-- Name: user_backup_code user_backup_code_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_backup_code
    ADD CONSTRAINT user_backup_code_pkey PRIMARY KEY (id);


--
-- Name: user_real_name_access_logs user_real_name_access_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_real_name_access_logs
    ADD CONSTRAINT user_real_name_access_logs_pkey PRIMARY KEY (id);


--
-- Name: user_real_name_identities user_real_name_identities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_real_name_identities
    ADD CONSTRAINT user_real_name_identities_pkey PRIMARY KEY (id);


--
-- Name: user_role user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT user_role_pkey PRIMARY KEY (id);


--
-- Name: user_role user_role_user_id_role_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT user_role_user_id_role_key UNIQUE (user_id, role);


--
-- Name: user_seu_consumption user_seu_consumption_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_seu_consumption
    ADD CONSTRAINT user_seu_consumption_pkey PRIMARY KEY (id);


--
-- Name: IDX_0ef2a982b61980d95b5ae7f1a6; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_0ef2a982b61980d95b5ae7f1a6" ON public.answer_update_log USING btree (updater_id);


--
-- Name: IDX_1261db28434fde159acda6094b; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_1261db28434fde159acda6094b" ON public.user_profile_query_log USING btree (viewer_id);


--
-- Name: IDX_13c7e9fd7403cc5a87ab6524bc; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_13c7e9fd7403cc5a87ab6524bc" ON public.question_search_log USING btree (searcher_id);


--
-- Name: IDX_187915d8eaa010cde8b053b35d; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_187915d8eaa010cde8b053b35d" ON public.question USING btree (created_by_id);


--
-- Name: IDX_1887685ce6667b435b01c646a2; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_1887685ce6667b435b01c646a2" ON public.answer USING btree (group_id);


--
-- Name: IDX_19d57f140124c5100e8e1ca308; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_19d57f140124c5100e8e1ca308" ON public.group_target USING btree (group_id);


--
-- Name: IDX_21a30245c4a32d5ac67da80901; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_21a30245c4a32d5ac67da80901" ON public.question_follower_relation USING btree (follower_id);


--
-- Name: IDX_2fbe3aa9f62233381aefeafa00; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_2fbe3aa9f62233381aefeafa00" ON public.question_search_log USING btree (keywords);


--
-- Name: IDX_3af79f07534d9f1c945cd4c702; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_3af79f07534d9f1c945cd4c702" ON public.user_register_log USING btree (email);


--
-- Name: IDX_3d2f174ef04fb312fdebd0ddc5; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_3d2f174ef04fb312fdebd0ddc5" ON public.session USING btree (user_id);


--
-- Name: IDX_4020ff7fcffb2737e990f8bde5; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_4020ff7fcffb2737e990f8bde5" ON public.comment_query_log USING btree (comment_id);


--
-- Name: IDX_4ead8566a6fa987264484b13d5; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_4ead8566a6fa987264484b13d5" ON public.comment_query_log USING btree (viewer_id);


--
-- Name: IDX_51cb79b5555effaf7d69ba1cff; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "IDX_51cb79b5555effaf7d69ba1cff" ON public.user_profile USING btree (user_id);


--
-- Name: IDX_525212ea7a75cba69724e42303; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_525212ea7a75cba69724e42303" ON public.comment USING btree (commentable_id);


--
-- Name: IDX_53f0a8befcc12c0f7f2bab7584; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_53f0a8befcc12c0f7f2bab7584" ON public.comment_delete_log USING btree (operated_by_id);


--
-- Name: IDX_59d7548ea797208240417106e2; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_59d7548ea797208240417106e2" ON public.topic USING btree (created_by_id);


--
-- Name: IDX_63ac916757350d28f05c5a6a4b; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_63ac916757350d28f05c5a6a4b" ON public.comment USING btree (created_by_id);


--
-- Name: IDX_6544f7f7579bf88e3c62f995f8; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_6544f7f7579bf88e3c62f995f8" ON public.question_follower_relation USING btree (question_id);


--
-- Name: IDX_66705ce7d7908554cff01b260e; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_66705ce7d7908554cff01b260e" ON public.comment_delete_log USING btree (comment_id);


--
-- Name: IDX_66c592c7f7f20d1214aba2d004; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_66c592c7f7f20d1214aba2d004" ON public.user_login_log USING btree (user_id);


--
-- Name: IDX_6f0964cf74c12678a86e49b23f; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_6f0964cf74c12678a86e49b23f" ON public.answer_update_log USING btree (answer_id);


--
-- Name: IDX_71ed57d6bb340716f5e17043bb; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_71ed57d6bb340716f5e17043bb" ON public.answer_query_log USING btree (answer_id);


--
-- Name: IDX_78a916df40e02a9deb1c4b75ed; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "IDX_78a916df40e02a9deb1c4b75ed" ON public."user" USING btree (username);


--
-- Name: IDX_7d88d00d8617a802b698c0cd60; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_7d88d00d8617a802b698c0cd60" ON public.group_membership USING btree (member_id);


--
-- Name: IDX_85c1844b4fa3e29b1b8dfaeac6; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_85c1844b4fa3e29b1b8dfaeac6" ON public.topic_search_log USING btree (keywords);


--
-- Name: IDX_868df0c2c3a138ee54d2a515bc; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_868df0c2c3a138ee54d2a515bc" ON public.user_following_relationship USING btree (follower_id);


--
-- Name: IDX_8a45300fd825918f3b40195fbd; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "IDX_8a45300fd825918f3b40195fbd" ON public."group" USING btree (name);


--
-- Name: IDX_8b24620899a8556c3f22f52145; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_8b24620899a8556c3f22f52145" ON public.question USING btree (title, content);


--
-- Name: IDX_8ce4bcc67caf0406e6f20923d4; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_8ce4bcc67caf0406e6f20923d4" ON public.question_query_log USING btree (viewer_id);


--
-- Name: IDX_910393b814aac627593588c17f; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_910393b814aac627593588c17f" ON public.answer_delete_log USING btree (answer_id);


--
-- Name: IDX_9556368d270d73579a68db7e1b; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_9556368d270d73579a68db7e1b" ON public.answer_favorited_by_user USING btree (user_id);


--
-- Name: IDX_a0ee1672e103ed0a0266f217a3; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_a0ee1672e103ed0a0266f217a3" ON public.question_query_log USING btree (question_id);


--
-- Name: IDX_a4013f10cd6924793fbd5f0d63; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_a4013f10cd6924793fbd5f0d63" ON public.answer USING btree (question_id);


--
-- Name: IDX_ac7c68d428ab7ffd2f4752eeaa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_ac7c68d428ab7ffd2f4752eeaa" ON public.question USING btree (group_id);


--
-- Name: IDX_b1411f07fafcd5ad93c6ee1642; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_b1411f07fafcd5ad93c6ee1642" ON public.group_membership USING btree (group_id);


--
-- Name: IDX_b31bf3b3688ec41daaced89a0a; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_b31bf3b3688ec41daaced89a0a" ON public.group_question_relationship USING btree (group_id);


--
-- Name: IDX_bb46e87d5b3f1e55c625755c00; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_bb46e87d5b3f1e55c625755c00" ON public.session USING btree (valid_until);


--
-- Name: IDX_c1d0ecc369d7a6a3d7e876c589; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_c1d0ecc369d7a6a3d7e876c589" ON public.user_register_request USING btree (email);


--
-- Name: IDX_c27a91d761c26ad612a0a35697; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_c27a91d761c26ad612a0a35697" ON public.answer_favorited_by_user USING btree (answer_id);


--
-- Name: IDX_c2d0251df4669e17a57d6dbc06; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_c2d0251df4669e17a57d6dbc06" ON public.answer_delete_log USING btree (deleter_id);


--
-- Name: IDX_c78831eeee179237b1482d0c6f; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_c78831eeee179237b1482d0c6f" ON public.user_following_relationship USING btree (followee_id);


--
-- Name: IDX_dd4b9a1b83559fa38a3a50463f; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_dd4b9a1b83559fa38a3a50463f" ON public.question_topic_relation USING btree (topic_id);


--
-- Name: IDX_e12875dfb3b1d92d7d7c5377e2; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "IDX_e12875dfb3b1d92d7d7c5377e2" ON public."user" USING btree (email);


--
-- Name: IDX_f4b7cd859700f8928695b6c2ba; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_f4b7cd859700f8928695b6c2ba" ON public.answer_query_log USING btree (viewer_id);


--
-- Name: IDX_f636f6e852686173ea947f2904; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_f636f6e852686173ea947f2904" ON public.answer USING btree (created_by_id);


--
-- Name: IDX_fab99c5e4fc380d9b7f9abbbb0; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_fab99c5e4fc380d9b7f9abbbb0" ON public.question_topic_relation USING btree (question_id);


--
-- Name: IDX_fe1e75b8b625499f0119faaba5; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_fe1e75b8b625499f0119faaba5" ON public.topic_search_log USING btree (searcher_id);


--
-- Name: IDX_ff592e4403b328be0de4f2b397; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_ff592e4403b328be0de4f2b397" ON public.user_profile_query_log USING btree (viewee_id);


--
-- Name: IDX_passkey_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_passkey_user_id" ON public.passkey USING btree (user_id);


--
-- Name: IDX_user_backup_code_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "IDX_user_backup_code_user_id" ON public.user_backup_code USING btree (user_id);


--
-- Name: REL_5b1232271bf29d99456fcf39e7; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "REL_5b1232271bf29d99456fcf39e7" ON public.group_question_relationship USING btree (question_id);


--
-- Name: REL_7359ba99cc116d00cf74e048ed; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "REL_7359ba99cc116d00cf74e048ed" ON public.group_profile USING btree (group_id);


--
-- Name: attitude_attitudable_id_user_id_attitudable_type_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX attitude_attitudable_id_user_id_attitudable_type_key ON public.attitude USING btree (attitudable_id, user_id, attitudable_type);


--
-- Name: attitude_log_attitudable_id_attitudable_type_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX attitude_log_attitudable_id_attitudable_type_idx ON public.attitude_log USING btree (attitudable_id, attitudable_type);


--
-- Name: attitude_log_user_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX attitude_log_user_id_idx ON public.attitude_log USING btree (user_id);


--
-- Name: attitude_user_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX attitude_user_id_idx ON public.attitude USING btree (user_id);


--
-- Name: idx2lm01yrx3fukjj9a89v0hpr2; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx2lm01yrx3fukjj9a89v0hpr2 ON public.knowledge_label USING btree (label);


--
-- Name: idx3ctl72phv5d0yluddhpkxb1y4; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx3ctl72phv5d0yluddhpkxb1y4 ON public.task_submission_review USING btree (submission_id);


--
-- Name: idx3k75vvu7mevyvvb5may5lj8k7; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx3k75vvu7mevyvvb5may5lj8k7 ON public.project USING btree (name);


--
-- Name: idx4poyg61j8nhdhsryne7s8snmr; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx4poyg61j8nhdhsryne7s8snmr ON public.space_user_rank USING btree (user_id);


--
-- Name: idx5se9sb4u9yywkp49gnim8s85n; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx5se9sb4u9yywkp49gnim8s85n ON public.space_user_rank USING btree (space_id);


--
-- Name: idx70x5oq6omtraaie2fttiv25rd; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx70x5oq6omtraaie2fttiv25rd ON public.task_membership USING btree (task_id);


--
-- Name: idx95fxueh91d9uobhmly6smcdtw; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx95fxueh91d9uobhmly6smcdtw ON public.project_membership USING btree (project_id);


--
-- Name: idx9kayuwile36o13jipo63b10sb; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx9kayuwile36o13jipo63b10sb ON public.space_admin_relation USING btree (user_id);


--
-- Name: idx_reaction_discussion_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reaction_discussion_type ON public.discussion_reaction USING btree (discussion_id, reaction_type_id);


--
-- Name: idx_reaction_discussion_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reaction_discussion_user ON public.discussion_reaction USING btree (discussion_id, user_id);


--
-- Name: idx_topic_name_ft; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_topic_name_ft ON public.topic USING btree (name);


--
-- Name: idx_topic_name_unique; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_topic_name_unique ON public.topic USING btree (name);


--
-- Name: idxapcc8lxk2xnug8377fatvbn04; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxapcc8lxk2xnug8377fatvbn04 ON public.user_role USING btree (user_id);


--
-- Name: idxby2rktc4t1x2cvvlps3gx5pg3; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxby2rktc4t1x2cvvlps3gx5pg3 ON public.space_classification_topics_relation USING btree (topic_id);


--
-- Name: idxc0ggmiab5m1chu88hitikohha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxc0ggmiab5m1chu88hitikohha ON public.project_membership USING btree (user_id);


--
-- Name: idxd45hsmkordpydcytq25ahcnby; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxd45hsmkordpydcytq25ahcnby ON public.team_user_relation USING btree (team_id);


--
-- Name: idxfmekoev1y1edqr9achyy8jp3b; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxfmekoev1y1edqr9achyy8jp3b ON public.space_admin_relation USING btree (space_id);


--
-- Name: idxg2l9qqsoeuynt4r5ofdt1x2td; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxg2l9qqsoeuynt4r5ofdt1x2td ON public.team USING btree (name);


--
-- Name: idxh685vv2ufp7ohjnfw6hw231tv; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxh685vv2ufp7ohjnfw6hw231tv ON public.task_membership USING btree (member_id);


--
-- Name: idxif1pq04iwqmv1xrsf892htked; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxif1pq04iwqmv1xrsf892htked ON public.project USING btree (leader_id);


--
-- Name: idxiuy7mmgy0obas3bfqu1j519bc; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxiuy7mmgy0obas3bfqu1j519bc ON public.discussion USING btree (model_type, model_id);


--
-- Name: idxiyfipycyf7afycfn9f8gkhcvf; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxiyfipycyf7afycfn9f8gkhcvf ON public.knowledge_label USING btree (knowledge_id);


--
-- Name: idxjabwimmbvt40wl20fp0kaevp4; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxjabwimmbvt40wl20fp0kaevp4 ON public.knowledge USING btree (source_type);


--
-- Name: idxk01yi4pk1orlwhihsorqhnwr8; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxk01yi4pk1orlwhihsorqhnwr8 ON public.knowledge USING btree (project_id);


--
-- Name: idxka5lit0ursfthx3207eojpvn7; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxka5lit0ursfthx3207eojpvn7 ON public.task_topics_relation USING btree (task_id);


--
-- Name: idxkhxb42bgml7ds4m1mn25ulgpj; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxkhxb42bgml7ds4m1mn25ulgpj ON public.discussion USING btree (sender_id);


--
-- Name: idxkycbyj306lg659w6g2ceuqpnu; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxkycbyj306lg659w6g2ceuqpnu ON public.project USING btree (parent_id);


--
-- Name: idxlk5f1tf6v3vslj8o1rfqwgcwi; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxlk5f1tf6v3vslj8o1rfqwgcwi ON public.knowledge USING btree (team_id);


--
-- Name: idxmllyu96n9vj606vm9w0gp3obx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxmllyu96n9vj606vm9w0gp3obx ON public.space USING btree (name);


--
-- Name: idxo1hn4jgjd2kpue9yt0ptyj2jp; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxo1hn4jgjd2kpue9yt0ptyj2jp ON public.space_classification_topics_relation USING btree (space_id);


--
-- Name: idxobr8vksep0or878byk2kohpty; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxobr8vksep0or878byk2kohpty ON public.project_membership USING btree (role);


--
-- Name: idxop6rbg45aknhvuqaakuonifao; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxop6rbg45aknhvuqaakuonifao ON public.task_topics_relation USING btree (topic_id);


--
-- Name: idxp6qd6pe6q2uaa78bypcy99k2k; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxp6qd6pe6q2uaa78bypcy99k2k ON public.knowledge USING btree (discussion_id);


--
-- Name: idxquiqutq8cnj700u982xlo8bsm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxquiqutq8cnj700u982xlo8bsm ON public.team_user_relation USING btree (user_id);


--
-- Name: idxrl2bt06yn51j9nbk2d0tjituy; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idxrl2bt06yn51j9nbk2d0tjituy ON public.project USING btree (team_id);


--
-- Name: question_accepted_answer_id_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX question_accepted_answer_id_key ON public.question USING btree (accepted_answer_id);


--
-- Name: question_elasticsearch_relation_elasticsearch_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX question_elasticsearch_relation_elasticsearch_id_idx ON public.question_elasticsearch_relation USING btree (elasticsearch_id);


--
-- Name: question_elasticsearch_relation_question_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX question_elasticsearch_relation_question_id_idx ON public.question_elasticsearch_relation USING btree (question_id);


--
-- Name: question_elasticsearch_relation_question_id_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX question_elasticsearch_relation_question_id_key ON public.question_elasticsearch_relation USING btree (question_id);


--
-- Name: question_invitation_relation_question_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX question_invitation_relation_question_id_idx ON public.question_invitation_relation USING btree (question_id);


--
-- Name: question_invitation_relation_user_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX question_invitation_relation_user_id_idx ON public.question_invitation_relation USING btree (user_id);


--
-- Name: answer_update_log FK_0ef2a982b61980d95b5ae7f1a60; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_update_log
    ADD CONSTRAINT "FK_0ef2a982b61980d95b5ae7f1a60" FOREIGN KEY (updater_id) REFERENCES public."user" (id);


--
-- Name: user_profile_query_log FK_1261db28434fde159acda6094bc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile_query_log
    ADD CONSTRAINT "FK_1261db28434fde159acda6094bc" FOREIGN KEY (viewer_id) REFERENCES public."user" (id);


--
-- Name: question_search_log FK_13c7e9fd7403cc5a87ab6524bc4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_search_log
    ADD CONSTRAINT "FK_13c7e9fd7403cc5a87ab6524bc4" FOREIGN KEY (searcher_id) REFERENCES public."user" (id);


--
-- Name: question FK_187915d8eaa010cde8b053b35d5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question
    ADD CONSTRAINT "FK_187915d8eaa010cde8b053b35d5" FOREIGN KEY (created_by_id) REFERENCES public."user" (id);


--
-- Name: answer FK_1887685ce6667b435b01c646a2c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer
    ADD CONSTRAINT "FK_1887685ce6667b435b01c646a2c" FOREIGN KEY (group_id) REFERENCES public."group" (id);


--
-- Name: group_target FK_19d57f140124c5100e8e1ca3088; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_target
    ADD CONSTRAINT "FK_19d57f140124c5100e8e1ca3088" FOREIGN KEY (group_id) REFERENCES public."group" (id);


--
-- Name: question_follower_relation FK_21a30245c4a32d5ac67da809010; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_follower_relation
    ADD CONSTRAINT "FK_21a30245c4a32d5ac67da809010" FOREIGN KEY (follower_id) REFERENCES public."user" (id);


--
-- Name: comment_query_log FK_4020ff7fcffb2737e990f8bde5e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comment_query_log
    ADD CONSTRAINT "FK_4020ff7fcffb2737e990f8bde5e" FOREIGN KEY (comment_id) REFERENCES public.comment (id);


--
-- Name: comment_query_log FK_4ead8566a6fa987264484b13d54; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comment_query_log
    ADD CONSTRAINT "FK_4ead8566a6fa987264484b13d54" FOREIGN KEY (viewer_id) REFERENCES public."user" (id);


--
-- Name: comment_delete_log FK_53f0a8befcc12c0f7f2bab7584d; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comment_delete_log
    ADD CONSTRAINT "FK_53f0a8befcc12c0f7f2bab7584d" FOREIGN KEY (operated_by_id) REFERENCES public."user" (id);


--
-- Name: topic FK_59d7548ea797208240417106e2d; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topic
    ADD CONSTRAINT "FK_59d7548ea797208240417106e2d" FOREIGN KEY (created_by_id) REFERENCES public."user" (id);


--
-- Name: group_question_relationship FK_5b1232271bf29d99456fcf39e75; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_question_relationship
    ADD CONSTRAINT "FK_5b1232271bf29d99456fcf39e75" FOREIGN KEY (question_id) REFERENCES public.question (id);


--
-- Name: comment FK_63ac916757350d28f05c5a6a4ba; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comment
    ADD CONSTRAINT "FK_63ac916757350d28f05c5a6a4ba" FOREIGN KEY (created_by_id) REFERENCES public."user" (id);


--
-- Name: question_follower_relation FK_6544f7f7579bf88e3c62f995f8a; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_follower_relation
    ADD CONSTRAINT "FK_6544f7f7579bf88e3c62f995f8a" FOREIGN KEY (question_id) REFERENCES public.question (id);


--
-- Name: comment_delete_log FK_66705ce7d7908554cff01b260ec; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comment_delete_log
    ADD CONSTRAINT "FK_66705ce7d7908554cff01b260ec" FOREIGN KEY (comment_id) REFERENCES public.comment (id);


--
-- Name: user_login_log FK_66c592c7f7f20d1214aba2d0046; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_login_log
    ADD CONSTRAINT "FK_66c592c7f7f20d1214aba2d0046" FOREIGN KEY (user_id) REFERENCES public."user" (id);


--
-- Name: answer_update_log FK_6f0964cf74c12678a86e49b23fe; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_update_log
    ADD CONSTRAINT "FK_6f0964cf74c12678a86e49b23fe" FOREIGN KEY (answer_id) REFERENCES public.answer (id);


--
-- Name: answer_query_log FK_71ed57d6bb340716f5e17043bbb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_query_log
    ADD CONSTRAINT "FK_71ed57d6bb340716f5e17043bbb" FOREIGN KEY (answer_id) REFERENCES public.answer (id);


--
-- Name: group_profile FK_7359ba99cc116d00cf74e048edd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_profile
    ADD CONSTRAINT "FK_7359ba99cc116d00cf74e048edd" FOREIGN KEY (group_id) REFERENCES public."group" (id);


--
-- Name: group_membership FK_7d88d00d8617a802b698c0cd609; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_membership
    ADD CONSTRAINT "FK_7d88d00d8617a802b698c0cd609" FOREIGN KEY (member_id) REFERENCES public."user" (id);


--
-- Name: user_following_relationship FK_868df0c2c3a138ee54d2a515bce; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_following_relationship
    ADD CONSTRAINT "FK_868df0c2c3a138ee54d2a515bce" FOREIGN KEY (follower_id) REFERENCES public."user" (id);


--
-- Name: question_query_log FK_8ce4bcc67caf0406e6f20923d4d; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_query_log
    ADD CONSTRAINT "FK_8ce4bcc67caf0406e6f20923d4d" FOREIGN KEY (viewer_id) REFERENCES public."user" (id);


--
-- Name: answer_delete_log FK_910393b814aac627593588c17fd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_delete_log
    ADD CONSTRAINT "FK_910393b814aac627593588c17fd" FOREIGN KEY (answer_id) REFERENCES public.answer (id);


--
-- Name: answer_favorited_by_user FK_9556368d270d73579a68db7e1bf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_favorited_by_user
    ADD CONSTRAINT "FK_9556368d270d73579a68db7e1bf" FOREIGN KEY (user_id) REFERENCES public."user" (id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: question_query_log FK_a0ee1672e103ed0a0266f217a3f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_query_log
    ADD CONSTRAINT "FK_a0ee1672e103ed0a0266f217a3f" FOREIGN KEY (question_id) REFERENCES public.question (id);


--
-- Name: answer FK_a4013f10cd6924793fbd5f0d637; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer
    ADD CONSTRAINT "FK_a4013f10cd6924793fbd5f0d637" FOREIGN KEY (question_id) REFERENCES public.question (id);


--
-- Name: group_membership FK_b1411f07fafcd5ad93c6ee16424; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_membership
    ADD CONSTRAINT "FK_b1411f07fafcd5ad93c6ee16424" FOREIGN KEY (group_id) REFERENCES public."group" (id);


--
-- Name: group_question_relationship FK_b31bf3b3688ec41daaced89a0ab; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_question_relationship
    ADD CONSTRAINT "FK_b31bf3b3688ec41daaced89a0ab" FOREIGN KEY (group_id) REFERENCES public."group" (id);


--
-- Name: answer_favorited_by_user FK_c27a91d761c26ad612a0a356971; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_favorited_by_user
    ADD CONSTRAINT "FK_c27a91d761c26ad612a0a356971" FOREIGN KEY (answer_id) REFERENCES public.answer (id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: answer_delete_log FK_c2d0251df4669e17a57d6dbc06f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_delete_log
    ADD CONSTRAINT "FK_c2d0251df4669e17a57d6dbc06f" FOREIGN KEY (deleter_id) REFERENCES public."user" (id);


--
-- Name: user_following_relationship FK_c78831eeee179237b1482d0c6fb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_following_relationship
    ADD CONSTRAINT "FK_c78831eeee179237b1482d0c6fb" FOREIGN KEY (followee_id) REFERENCES public."user" (id);


--
-- Name: question_topic_relation FK_d439ea68a02c1e7ea9863fc3df1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_topic_relation
    ADD CONSTRAINT "FK_d439ea68a02c1e7ea9863fc3df1" FOREIGN KEY (created_by_id) REFERENCES public."user" (id);


--
-- Name: question_topic_relation FK_dd4b9a1b83559fa38a3a50463fd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_topic_relation
    ADD CONSTRAINT "FK_dd4b9a1b83559fa38a3a50463fd" FOREIGN KEY (topic_id) REFERENCES public.topic (id);


--
-- Name: answer_query_log FK_f4b7cd859700f8928695b6c2bab; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_query_log
    ADD CONSTRAINT "FK_f4b7cd859700f8928695b6c2bab" FOREIGN KEY (viewer_id) REFERENCES public."user" (id);


--
-- Name: answer FK_f636f6e852686173ea947f29045; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer
    ADD CONSTRAINT "FK_f636f6e852686173ea947f29045" FOREIGN KEY (created_by_id) REFERENCES public."user" (id);


--
-- Name: question_topic_relation FK_fab99c5e4fc380d9b7f9abbbb02; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_topic_relation
    ADD CONSTRAINT "FK_fab99c5e4fc380d9b7f9abbbb02" FOREIGN KEY (question_id) REFERENCES public.question (id);


--
-- Name: topic_search_log FK_fe1e75b8b625499f0119faaba5b; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topic_search_log
    ADD CONSTRAINT "FK_fe1e75b8b625499f0119faaba5b" FOREIGN KEY (searcher_id) REFERENCES public."user" (id);


--
-- Name: user_profile_query_log FK_ff592e4403b328be0de4f2b3973; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile_query_log
    ADD CONSTRAINT "FK_ff592e4403b328be0de4f2b3973" FOREIGN KEY (viewee_id) REFERENCES public."user" (id);


--
-- Name: attitude_log attitude_log_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attitude_log
    ADD CONSTRAINT attitude_log_user_id_fkey FOREIGN KEY (user_id) REFERENCES public."user" (id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: attitude attitude_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attitude
    ADD CONSTRAINT attitude_user_id_fkey FOREIGN KEY (user_id) REFERENCES public."user" (id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: knowledge fk1df46ccdrd9w7i5bf1urij4j2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge
    ADD CONSTRAINT fk1df46ccdrd9w7i5bf1urij4j2 FOREIGN KEY (discussion_id) REFERENCES public.discussion (id);


--
-- Name: material fk21nqwvdonsvsnp7r3d9uo17bo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material
    ADD CONSTRAINT fk21nqwvdonsvsnp7r3d9uo17bo FOREIGN KEY (uploader_id) REFERENCES public."user" (id) ON DELETE RESTRICT;


--
-- Name: knowledge_label fk43k38w4y2j691mb6nhyijhq96; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_label
    ADD CONSTRAINT fk43k38w4y2j691mb6nhyijhq96 FOREIGN KEY (knowledge_id) REFERENCES public.knowledge (id);


--
-- Name: materialbundles_relation fk5fkpr8538ghw2wfjten9hergi; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materialbundles_relation
    ADD CONSTRAINT fk5fkpr8538ghw2wfjten9hergi FOREIGN KEY (bundle_id) REFERENCES public.material_bundle (id) ON DELETE CASCADE;


--
-- Name: materialbundles_relation fk62blmnwrqevg0whwwv5231b5g; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materialbundles_relation
    ADD CONSTRAINT fk62blmnwrqevg0whwwv5231b5g FOREIGN KEY (material_id) REFERENCES public.material (id) ON DELETE RESTRICT;


--
-- Name: task fk67uenor8d9f8lq7wjv7h56n2o; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task
    ADD CONSTRAINT fk67uenor8d9f8lq7wjv7h56n2o FOREIGN KEY (creator_id) REFERENCES public."user" (id);


--
-- Name: task_membership fk6jngswgv5k77n9pj70f18a49y; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_membership
    ADD CONSTRAINT fk6jngswgv5k77n9pj70f18a49y FOREIGN KEY (task_id) REFERENCES public.task (id);


--
-- Name: project_membership fk6nerxrll628ug1mvgpt2spgpk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_membership
    ADD CONSTRAINT fk6nerxrll628ug1mvgpt2spgpk FOREIGN KEY (project_id) REFERENCES public.project (id);


--
-- Name: task fk6r32b6vk1rpu7ww7gratmce1i; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task
    ADD CONSTRAINT fk6r32b6vk1rpu7ww7gratmce1i FOREIGN KEY (team_id) REFERENCES public.team (id);


--
-- Name: space fk77tn26hq1ml6ri82fp970we8n; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.space
    ADD CONSTRAINT fk77tn26hq1ml6ri82fp970we8n FOREIGN KEY (avatar_id) REFERENCES public.avatar (id);


--
-- Name: discussion fk7wmfmeiq6a9y5ygdi1obp01r; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discussion
    ADD CONSTRAINT fk7wmfmeiq6a9y5ygdi1obp01r FOREIGN KEY (sender_id) REFERENCES public."user" (id);


--
-- Name: team_user_relation fk8rg61fyticaiphplc6wb3o68p; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.team_user_relation
    ADD CONSTRAINT fk8rg61fyticaiphplc6wb3o68p FOREIGN KEY (user_id) REFERENCES public."user" (id);


--
-- Name: discussion_reaction fk9r8w78vm7sdf6iqkcl0pc9jqp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discussion_reaction
    ADD CONSTRAINT fk9r8w78vm7sdf6iqkcl0pc9jqp FOREIGN KEY (reaction_type_id) REFERENCES public.reaction_type (id);


--
-- Name: question_elasticsearch_relation fk_question_elasticsearch_relation_question_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_elasticsearch_relation
    ADD CONSTRAINT fk_question_elasticsearch_relation_question_id FOREIGN KEY (question_id) REFERENCES public.question (id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: user_profile fk_user_profile_avatar_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile
    ADD CONSTRAINT fk_user_profile_avatar_id FOREIGN KEY (avatar_id) REFERENCES public.avatar (id);


--
-- Name: user_profile fk_user_profile_user_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile
    ADD CONSTRAINT fk_user_profile_user_id FOREIGN KEY (user_id) REFERENCES public."user" (id);


--
-- Name: knowledge fkacal20h046lgv8bl4bkl2di1f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge
    ADD CONSTRAINT fkacal20h046lgv8bl4bkl2di1f FOREIGN KEY (created_by_id) REFERENCES public."user" (id);


--
-- Name: discussion_reaction fkawpinudxl9749buwtradij9p6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discussion_reaction
    ADD CONSTRAINT fkawpinudxl9749buwtradij9p6 FOREIGN KEY (discussion_id) REFERENCES public.discussion (id);


--
-- Name: task_submission_review fkba2fmo0mgjdlohcnpf97tvgvt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_submission_review
    ADD CONSTRAINT fkba2fmo0mgjdlohcnpf97tvgvt FOREIGN KEY (submission_id) REFERENCES public.task_submission (id);


--
-- Name: knowledge_knowledge_labels fkbdxbbpetfqn7y3cuq50tgcuoq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_knowledge_labels
    ADD CONSTRAINT fkbdxbbpetfqn7y3cuq50tgcuoq FOREIGN KEY (knowledge_id) REFERENCES public.knowledge (id);


--
-- Name: task_topics_relation fkd1esf4rvrn7eedttnfqs5dfw1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_topics_relation
    ADD CONSTRAINT fkd1esf4rvrn7eedttnfqs5dfw1 FOREIGN KEY (topic_id) REFERENCES public.topic (id);


--
-- Name: space_admin_relation fkd3x1m946orup61b4f4wu6h2xn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.space_admin_relation
    ADD CONSTRAINT fkd3x1m946orup61b4f4wu6h2xn FOREIGN KEY (user_id) REFERENCES public."user" (id);


--
-- Name: knowledge_knowledge_labels fkd9dp2f3c65b8d2gdc9oeql6ja; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_knowledge_labels
    ADD CONSTRAINT fkd9dp2f3c65b8d2gdc9oeql6ja FOREIGN KEY (knowledge_labels_id) REFERENCES public.knowledge_label (id);


--
-- Name: task fke6m3e5625asfu59r4doayop1o; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task
    ADD CONSTRAINT fke6m3e5625asfu59r4doayop1o FOREIGN KEY (space_id) REFERENCES public.space (id);


--
-- Name: task_submission_schema fkeipe4rx4f493n82xgon4padr5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_submission_schema
    ADD CONSTRAINT fkeipe4rx4f493n82xgon4padr5 FOREIGN KEY (task_id) REFERENCES public.task (id);


--
-- Name: discussion_mentioned_users fkg7pxkxyxyxq2g2qttjeyph6ra; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discussion_mentioned_users
    ADD CONSTRAINT fkg7pxkxyxyxq2g2qttjeyph6ra FOREIGN KEY (discussion_id) REFERENCES public.discussion (id);


--
-- Name: discussion fkgs1dxhfnb68rh344y4f34fwqa; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discussion
    ADD CONSTRAINT fkgs1dxhfnb68rh344y4f34fwqa FOREIGN KEY (parent_id) REFERENCES public.discussion (id);


--
-- Name: space_user_rank fkh0k0jxvhnph0eoc5gw7652hdy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.space_user_rank
    ADD CONSTRAINT fkh0k0jxvhnph0eoc5gw7652hdy FOREIGN KEY (space_id) REFERENCES public.space (id);


--
-- Name: space_admin_relation fkhkpyunhmsubl1gvahyk9e9lff; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.space_admin_relation
    ADD CONSTRAINT fkhkpyunhmsubl1gvahyk9e9lff FOREIGN KEY (space_id) REFERENCES public.space (id);


--
-- Name: knowledge fkhniy1bsjjeoxbpfnlwydlgxtk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge
    ADD CONSTRAINT fkhniy1bsjjeoxbpfnlwydlgxtk FOREIGN KEY (material_id) REFERENCES public.material (id);


--
-- Name: space_user_rank fkiego7kcolpikn8o93o3qdjt3p; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.space_user_rank
    ADD CONSTRAINT fkiego7kcolpikn8o93o3qdjt3p FOREIGN KEY (user_id) REFERENCES public."user" (id);


--
-- Name: task_topics_relation fkjbxaijxjd045fy4ry2pxenrir; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_topics_relation
    ADD CONSTRAINT fkjbxaijxjd045fy4ry2pxenrir FOREIGN KEY (task_id) REFERENCES public.task (id);


--
-- Name: discussion_reaction fkjh9fs8xa2fuupaw2josqcjbfv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discussion_reaction
    ADD CONSTRAINT fkjh9fs8xa2fuupaw2josqcjbfv FOREIGN KEY (user_id) REFERENCES public."user" (id);


--
-- Name: team fkjv1k745e89swu3gj896pxcq3y; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.team
    ADD CONSTRAINT fkjv1k745e89swu3gj896pxcq3y FOREIGN KEY (avatar_id) REFERENCES public.avatar (id);


--
-- Name: material_bundle fkl3r75ka0qydpitvbtayq7grsi; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_bundle
    ADD CONSTRAINT fkl3r75ka0qydpitvbtayq7grsi FOREIGN KEY (creator_id) REFERENCES public."user" (id) ON DELETE RESTRICT;


--
-- Name: user_profile fko5dcemd97atrmjapi9x4s1j32; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile
    ADD CONSTRAINT fko5dcemd97atrmjapi9x4s1j32 FOREIGN KEY (avatar_id) REFERENCES public.avatar (id);


--
-- Name: space_classification_topics_relation fkpvs648o6f6bdvjsa27dd2pdp1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.space_classification_topics_relation
    ADD CONSTRAINT fkpvs648o6f6bdvjsa27dd2pdp1 FOREIGN KEY (topic_id) REFERENCES public.topic (id);


--
-- Name: user_profile fkqcd5nmg7d7ement27tt9sf3bi; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile
    ADD CONSTRAINT fkqcd5nmg7d7ement27tt9sf3bi FOREIGN KEY (user_id) REFERENCES public."user" (id);


--
-- Name: task_membership_team_members fkqqe540ncjf6ylguc5r3mt51sm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_membership_team_members
    ADD CONSTRAINT fkqqe540ncjf6ylguc5r3mt51sm FOREIGN KEY (task_membership_id) REFERENCES public.task_membership (id);


--
-- Name: knowledge fks854y5ajesie1jja4thskulsc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge
    ADD CONSTRAINT fks854y5ajesie1jja4thskulsc FOREIGN KEY (team_id) REFERENCES public.team (id);


--
-- Name: notification fks951ba5cqr6ibbu6w295b3ljg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT fks951ba5cqr6ibbu6w295b3ljg FOREIGN KEY (receiver_id) REFERENCES public."user" (id);


--
-- Name: ai_message fksaxwtwysovynl73cqwffe1fbn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_message
    ADD CONSTRAINT fksaxwtwysovynl73cqwffe1fbn FOREIGN KEY (conversation_id) REFERENCES public.ai_conversation (id);


--
-- Name: project fkt0just6g3205u402vn88i0fhy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fkt0just6g3205u402vn88i0fhy FOREIGN KEY (parent_id) REFERENCES public.project (id);


--
-- Name: team_user_relation fktf9y6q1stv6vpqtlclj1okjxs; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.team_user_relation
    ADD CONSTRAINT fktf9y6q1stv6vpqtlclj1okjxs FOREIGN KEY (team_id) REFERENCES public.team (id);


--
-- Name: space_classification_topics_relation fktfux67slxlrbd7975e7066vq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.space_classification_topics_relation
    ADD CONSTRAINT fktfux67slxlrbd7975e7066vq FOREIGN KEY (space_id) REFERENCES public.space (id);


--
-- Name: group_profile group_profile_avatar_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_profile
    ADD CONSTRAINT group_profile_avatar_id_fkey FOREIGN KEY (avatar_id) REFERENCES public.avatar (id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: material_bundle material_bundle_creator_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_bundle
    ADD CONSTRAINT material_bundle_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES public."user" (id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: material material_uploader_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material
    ADD CONSTRAINT material_uploader_id_fkey FOREIGN KEY (uploader_id) REFERENCES public."user" (id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: materialbundles_relation materialbundles_relation_bundle_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materialbundles_relation
    ADD CONSTRAINT materialbundles_relation_bundle_id_fkey FOREIGN KEY (bundle_id) REFERENCES public.material_bundle (id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: materialbundles_relation materialbundles_relation_material_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materialbundles_relation
    ADD CONSTRAINT materialbundles_relation_material_id_fkey FOREIGN KEY (material_id) REFERENCES public.material (id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: passkey passkey_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.passkey
    ADD CONSTRAINT passkey_user_id_fkey FOREIGN KEY (user_id) REFERENCES public."user" (id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: question question_accepted_answer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question
    ADD CONSTRAINT question_accepted_answer_id_fkey FOREIGN KEY (accepted_answer_id) REFERENCES public.answer (id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: question_invitation_relation question_invitation_relation_question_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_invitation_relation
    ADD CONSTRAINT question_invitation_relation_question_id_fkey FOREIGN KEY (question_id) REFERENCES public.question (id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: question_invitation_relation question_invitation_relation_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_invitation_relation
    ADD CONSTRAINT question_invitation_relation_user_id_fkey FOREIGN KEY (user_id) REFERENCES public."user" (id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: user_backup_code user_backup_code_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_backup_code
    ADD CONSTRAINT user_backup_code_user_id_fkey FOREIGN KEY (user_id) REFERENCES public."user" (id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- PostgreSQL database dump complete
--

