-- DROP TABLE IF EXISTS "spb-metro-check".tpp;

CREATE TABLE IF NOT EXISTS "spb-metro-check".tpp
(
    id bigserial NOT NULL,
    date_add timestamp without time zone NOT NULL DEFAULT (now())::timestamp without time zone,
    date_local timestamp without time zone,
    date_fn timestamp without time zone,
    status character varying(255) COLLATE pg_catalog."default",
    id_transaction character varying(255) COLLATE pg_catalog."default",
    summa numeric,
    code character varying(255) NOT NULL,
    gate character varying(255) NOT NULL,
    f54 character varying(255) NOT NULL,
    processed character varying(255),
    date_processed timestamp without time zone,
    data json NOT NULL,
    PRIMARY KEY (id)
);

-- DROP INDEX IF EXISTS "spb-metro-check".idx_01;

CREATE UNIQUE INDEX IF NOT EXISTS tpp_idx_01
    ON "spb-metro-check".tpp USING btree
    (id_transaction COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pg_default;

-- DROP TABLE IF EXISTS "spb-metro-check".orange;

CREATE TABLE IF NOT EXISTS "spb-metro-check".orange
(
    id bigserial NOT NULL,
    date_add timestamp without time zone NOT NULL DEFAULT (now())::timestamp without time zone,
    date_local timestamp without time zone,
    id_transaction character varying(255) COLLATE pg_catalog."default",
    summa numeric,
    code character varying(255) NOT NULL,
    gate character varying(255) NOT NULL,
    f24 character varying(255) NOT NULL,
    processed character varying(255),
    date_processed timestamp without time zone,
    data json NOT NULL,
    PRIMARY KEY (id)
);

-- DROP INDEX IF EXISTS "spb-metro-check".orange_idx_01;

CREATE UNIQUE INDEX IF NOT EXISTS orange_idx_01
    ON "spb-metro-check".orange USING btree
    (id_transaction COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pg_default;

CREATE TABLE "spb-metro-check".station
(
    id bigserial NOT NULL,
    code character varying(255) NOT NULL,
    place text,
    PRIMARY KEY (id)
);