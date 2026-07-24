create table if not exists file_samples
(
    uuid             uuid primary key,
    label            varchar(32),
    description      text,
    model_uri        varchar(512),
    thumbnail_uri    varchar(512),
    applicable_tools text[]
);

create index if not exists idx_file_samples_applicable_tools on file_samples using gin (applicable_tools);
