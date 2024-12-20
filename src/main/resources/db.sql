-- DROP TABLE IF EXISTS "spb-metro-check".tpp;
--fof (frame of reference) система отсчёта

CREATE TABLE IF NOT EXISTS "spb-metro-check".tpp
(
    id bigserial NOT NULL,
    date_fof date NOT NULL,
    date_add timestamp without time zone NOT NULL DEFAULT (now())::timestamp without time zone,
    date_local timestamp without time zone,
    date_fn timestamp without time zone,
    status character varying(255) COLLATE pg_catalog."default",
    id_transaction character varying(255) COLLATE pg_catalog."default",
    id_transaction_orange character varying(255) COLLATE pg_catalog."default",
    summa numeric,
    code character varying(255) NOT NULL,
    gate character varying(255) NOT NULL,
    f54 character varying(255) NOT NULL,
    processed character varying(255),
    date_processed timestamp without time zone,
    PRIMARY KEY (id)
);

-- DROP INDEX IF EXISTS "spb-metro-check".idx_01;

CREATE INDEX IF NOT EXISTS tpp_idx_01
    ON "spb-metro-check".tpp USING btree
    (id_transaction COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pg_default;

CREATE INDEX IF NOT EXISTS tpp_idx_02
    ON "spb-metro-check".tpp USING btree
    (id_transaction_orange COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pg_default;

CREATE INDEX IF NOT EXISTS tpp_idx_03
    ON "spb-metro-check".tpp USING btree
    (date_fof ASC NULLS LAST);

-- DROP TABLE IF EXISTS "spb-metro-check".orange;

CREATE TABLE IF NOT EXISTS "spb-metro-check".orange
(
    id bigserial NOT NULL,
    date_fof date NOT NULL,
    date_add timestamp without time zone NOT NULL DEFAULT (now())::timestamp without time zone,
    date_local timestamp without time zone,
    id_transaction character varying(255) COLLATE pg_catalog."default",
    summa numeric,
    code character varying(255) NOT NULL,
    gate character varying(255) NOT NULL,
    f24 character varying(255) NOT NULL,
    f25 character varying(255) NOT NULL,
    processed character varying(255),
    date_processed timestamp without time zone,
    PRIMARY KEY (id)
);

-- DROP INDEX IF EXISTS "spb-metro-check".orange_idx_01;

CREATE UNIQUE INDEX IF NOT EXISTS orange_idx_01
    ON "spb-metro-check".orange USING btree
    (id_transaction COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pg_default;

CREATE INDEX IF NOT EXISTS orange_idx_02
    ON "spb-metro-check".orange USING btree
    (date_fof ASC NULLS LAST);

CREATE TABLE "spb-metro-check".station
(
    id bigserial NOT NULL,
    code character varying(255) NOT NULL,
    place text,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS "spb-metro-check".kkt
(
    id bigserial NOT NULL,
    date_fof date NOT NULL,
    date_add timestamp without time zone NOT NULL DEFAULT (now())::timestamp without time zone,
    summa numeric,
    code character varying(255) NOT NULL,
    gate character varying(255) NOT NULL,
    count_agg numeric,
    summa_agg numeric,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS kkt_idx_01
    ON "spb-metro-check".kkt USING btree
    (date_fof ASC NULLS LAST);

CREATE TABLE IF NOT EXISTS "spb-metro-check".total
(
    id bigserial NOT NULL,
    date_add timestamp without time zone NOT NULL DEFAULT (now())::timestamp without time zone,
    date_fof date NOT NULL,
    group_key character varying(255) NOT NULL,
    group_title character varying(255) NOT NULL,
    group_count numeric,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS total_idx_01
    ON "spb-metro-check".total USING btree
    (date_fof ASC NULLS LAST);