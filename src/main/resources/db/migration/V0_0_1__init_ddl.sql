create table if not exists model_record
(
    id              uuid primary key,
    title           text,
    description     text,
    provider        varchar(255),
    place           varchar(255),
    latitude        float,
    longitude       float,
    license         varchar(255),
    creator         varchar(255),
    creation_year   int,
    aggregator      varchar(255),
    external_id     text,
    external_source text,
    external_uri    text,
    thumbnail_uri   text
);

create index if not exists idx_model_record_title on model_record using btree (title);
