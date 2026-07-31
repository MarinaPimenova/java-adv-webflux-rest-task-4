-- liquibase formatted sql
-- changeset pimenovm:2
CREATE SEQUENCE book.user_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE book.users (
    id BIGINT DEFAULT NEXT VALUE FOR book.user_seq NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_user PRIMARY KEY (id),
    CONSTRAINT uc_user_name UNIQUE (name)
);