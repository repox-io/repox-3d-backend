create table if not exists xrculture_hub_registration
(
    id              uuid primary key,
    service_key     text,
    registration    text,
    timestamp       timestamp without time zone
);

