create table if not exists xrculture_log
(
    id              uuid primary key,
    endpoint        text,
    request         text,
    response        text,
    ip_address      varchar(128),
    timestamp       timestamp without time zone
);

