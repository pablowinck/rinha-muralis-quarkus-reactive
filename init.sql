SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', 'public', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

DROP TABLE IF EXISTS "pessoas" CASCADE;

CREATE TABLE pessoas (
                         id VARCHAR(36) PRIMARY KEY,
                         apelido VARCHAR(32),
                         nome VARCHAR(101),
                         nascimento VARCHAR(10),
                         stack TEXT,
                         term TEXT
);

CREATE INDEX idx_pessoas_term ON pessoas USING BTREE (term);
