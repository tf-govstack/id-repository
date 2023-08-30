-- SEQUENCE: idmap.batch_job_execution_seq

-- DROP SEQUENCE idmap.batch_job_execution_seq;

CREATE SEQUENCE idmap.batch_job_execution_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

ALTER SEQUENCE idmap.batch_job_execution_seq
    OWNER TO postgres;

GRANT ALL ON SEQUENCE idmap.batch_job_execution_seq TO idmapuser WITH GRANT OPTION;

GRANT ALL ON SEQUENCE idmap.batch_job_execution_seq TO idrepouser;

GRANT ALL ON SEQUENCE idmap.batch_job_execution_seq TO postgres;



-- SEQUENCE: idmap.batch_job_seq

-- DROP SEQUENCE idmap.batch_job_seq;

CREATE SEQUENCE idmap.batch_job_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

ALTER SEQUENCE idmap.batch_job_seq
    OWNER TO postgres;

GRANT ALL ON SEQUENCE idmap.batch_job_seq TO idmapuser;

GRANT ALL ON SEQUENCE idmap.batch_job_seq TO idrepouser;

GRANT ALL ON SEQUENCE idmap.batch_job_seq TO postgres;



-- SEQUENCE: idmap.batch_step_execution_seq

-- DROP SEQUENCE idmap.batch_step_execution_seq;

CREATE SEQUENCE idmap.batch_step_execution_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

ALTER SEQUENCE idmap.batch_step_execution_seq
    OWNER TO postgres;

GRANT ALL ON SEQUENCE idmap.batch_step_execution_seq TO idmapuser WITH GRANT OPTION;

GRANT ALL ON SEQUENCE idmap.batch_step_execution_seq TO idrepouser;

GRANT ALL ON SEQUENCE idmap.batch_step_execution_seq TO postgres;



-- SEQUENCE: idmap.vid_expirylist_id_seq

-- DROP SEQUENCE idmap.vid_expirylist_id_seq;

CREATE SEQUENCE idmap.vid_expirylist_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 2147483647
    CACHE 1;

ALTER SEQUENCE idmap.vid_expirylist_id_seq
    OWNER TO postgres;

GRANT ALL ON SEQUENCE idmap.vid_expirylist_id_seq TO idmapuser WITH GRANT OPTION;

GRANT ALL ON SEQUENCE idmap.vid_expirylist_id_seq TO idrepouser;

GRANT ALL ON SEQUENCE idmap.vid_expirylist_id_seq TO postgres;



-- Table: idmap.batch_job_instance

-- DROP TABLE idmap.batch_job_instance;

CREATE TABLE IF NOT EXISTS idmap.batch_job_instance
(
    job_instance_id bigint NOT NULL,
    version bigint,
    job_name character varying(100) COLLATE pg_catalog."default" NOT NULL,
    job_key character varying(32) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT batch_job_instance_pkey PRIMARY KEY (job_instance_id),
    CONSTRAINT job_inst_un UNIQUE (job_name, job_key)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE idmap.batch_job_instance
    OWNER to postgres;

GRANT DELETE, INSERT, REFERENCES, SELECT, UPDATE ON TABLE idmap.batch_job_instance TO idmapuser;

GRANT ALL ON TABLE idmap.batch_job_instance TO postgres;



-- Table: idmap.batch_job_execution

-- DROP TABLE idmap.batch_job_execution;

CREATE TABLE IF NOT EXISTS idmap.batch_job_execution
(
    job_execution_id bigint NOT NULL,
    version bigint,
    job_instance_id bigint NOT NULL,
    create_time timestamp without time zone NOT NULL,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    status character varying(10) COLLATE pg_catalog."default",
    exit_code character varying(2500) COLLATE pg_catalog."default",
    exit_message character varying(2500) COLLATE pg_catalog."default",
    last_updated timestamp without time zone,
    job_configuration_location character varying(2500) COLLATE pg_catalog."default",
    CONSTRAINT batch_job_execution_pkey PRIMARY KEY (job_execution_id),
    CONSTRAINT job_inst_exec_fk FOREIGN KEY (job_instance_id)
        REFERENCES idmap.batch_job_instance (job_instance_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE idmap.batch_job_execution
    OWNER to postgres;

GRANT DELETE, INSERT, REFERENCES, SELECT, UPDATE ON TABLE idmap.batch_job_execution TO idmapuser;

GRANT ALL ON TABLE idmap.batch_job_execution TO postgres;



-- Table: idmap.batch_job_execution_context

-- DROP TABLE idmap.batch_job_execution_context;

CREATE TABLE IF NOT EXISTS idmap.batch_job_execution_context
(
    job_execution_id bigint NOT NULL,
    short_context character varying(2500) COLLATE pg_catalog."default" NOT NULL,
    serialized_context text COLLATE pg_catalog."default",
    CONSTRAINT batch_job_execution_context_pkey PRIMARY KEY (job_execution_id),
    CONSTRAINT job_exec_ctx_fk FOREIGN KEY (job_execution_id)
        REFERENCES idmap.batch_job_execution (job_execution_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE idmap.batch_job_execution_context
    OWNER to postgres;

GRANT DELETE, INSERT, REFERENCES, SELECT, UPDATE ON TABLE idmap.batch_job_execution_context TO idmapuser;

GRANT ALL ON TABLE idmap.batch_job_execution_context TO postgres;



-- Table: idmap.batch_job_execution_params

-- DROP TABLE idmap.batch_job_execution_params;

CREATE TABLE IF NOT EXISTS idmap.batch_job_execution_params
(
    job_execution_id bigint NOT NULL,
    type_cd character varying(6) COLLATE pg_catalog."default" NOT NULL,
    key_name character varying(100) COLLATE pg_catalog."default" NOT NULL,
    string_val character varying(250) COLLATE pg_catalog."default",
    date_val timestamp without time zone,
    long_val bigint,
    double_val double precision,
    identifying character(1) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT job_exec_params_fk FOREIGN KEY (job_execution_id)
        REFERENCES idmap.batch_job_execution (job_execution_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE idmap.batch_job_execution_params
    OWNER to postgres;

GRANT DELETE, INSERT, REFERENCES, SELECT, UPDATE ON TABLE idmap.batch_job_execution_params TO idmapuser;

GRANT ALL ON TABLE idmap.batch_job_execution_params TO postgres;



-- Table: idmap.batch_step_execution

-- DROP TABLE idmap.batch_step_execution;

CREATE TABLE IF NOT EXISTS idmap.batch_step_execution
(
    step_execution_id bigint NOT NULL,
    version bigint NOT NULL,
    step_name character varying(100) COLLATE pg_catalog."default" NOT NULL,
    job_execution_id bigint NOT NULL,
    start_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone,
    status character varying(10) COLLATE pg_catalog."default",
    commit_count bigint,
    read_count bigint,
    filter_count bigint,
    write_count bigint,
    read_skip_count bigint,
    write_skip_count bigint,
    process_skip_count bigint,
    rollback_count bigint,
    exit_code character varying(2500) COLLATE pg_catalog."default",
    exit_message character varying(2500) COLLATE pg_catalog."default",
    last_updated timestamp without time zone,
    CONSTRAINT batch_step_execution_pkey PRIMARY KEY (step_execution_id),
    CONSTRAINT job_exec_step_fk FOREIGN KEY (job_execution_id)
        REFERENCES idmap.batch_job_execution (job_execution_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE idmap.batch_step_execution
    OWNER to postgres;

GRANT DELETE, INSERT, REFERENCES, SELECT, UPDATE ON TABLE idmap.batch_step_execution TO idmapuser;

GRANT ALL ON TABLE idmap.batch_step_execution TO postgres;



-- Table: idmap.batch_step_execution_context

-- DROP TABLE idmap.batch_step_execution_context;

CREATE TABLE IF NOT EXISTS idmap.batch_step_execution_context
(
    step_execution_id bigint NOT NULL,
    short_context character varying(2500) COLLATE pg_catalog."default" NOT NULL,
    serialized_context text COLLATE pg_catalog."default",
    CONSTRAINT batch_step_execution_context_pkey PRIMARY KEY (step_execution_id),
    CONSTRAINT step_exec_ctx_fk FOREIGN KEY (step_execution_id)
        REFERENCES idmap.batch_step_execution (step_execution_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE idmap.batch_step_execution_context
    OWNER to postgres;

GRANT DELETE, INSERT, REFERENCES, SELECT, UPDATE ON TABLE idmap.batch_step_execution_context TO idmapuser;

GRANT ALL ON TABLE idmap.batch_step_execution_context TO postgres;



-- Table: idmap.vid_expirylist

-- DROP TABLE idmap.vid_expirylist;

CREATE TABLE IF NOT EXISTS idmap.vid_expirylist
(
    id integer NOT NULL DEFAULT nextval('vid_expirylist_id_seq'::regclass),
    vid text COLLATE pg_catalog."default" NOT NULL,
    cr_date date NOT NULL,
    is_emailsent boolean NOT NULL,
    is_smssent boolean NOT NULL,
    CONSTRAINT vid_expirylist_pkey PRIMARY KEY (id)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE idmap.vid_expirylist
    OWNER to postgres;

GRANT DELETE, INSERT, REFERENCES, SELECT, UPDATE ON TABLE idmap.vid_expirylist TO idmapuser;

GRANT ALL ON TABLE idmap.vid_expirylist TO postgres;
