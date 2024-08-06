
-- DROP TABLE IF EXISTS "spb-metro-check".tpp;

CREATE TABLE IF NOT EXISTS "spb-metro-check".tpp
(
    id bigserial NOT NULL,
    date_add timestamp without time zone NOT NULL DEFAULT (now())::timestamp without time zone,
    date_local timestamp without time zone,
    date_fn timestamp without time zone,
    status character varying(255) COLLATE pg_catalog."default",
    id_transaction character varying(255) COLLATE pg_catalog."default",
    data json NOT NULL,
    PRIMARY KEY (id)
);


-- DROP TABLE IF EXISTS "spb-metro-check".orange;

CREATE TABLE IF NOT EXISTS "spb-metro-check".orange
(
    id bigserial NOT NULL,
    date_add timestamp without time zone NOT NULL DEFAULT (now())::timestamp without time zone,
    date_local timestamp without time zone,
    id_transaction character varying(255) COLLATE pg_catalog."default",
    data json NOT NULL,
    PRIMARY KEY (id)
);