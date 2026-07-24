create table if not exists model_processing_job
(
    id            uuid primary key,
    type          text                        not null,
    status        text                        not null,
    input         jsonb,
    output_file   text,
    error_message text,
    created_at    timestamp without time zone not null,
    started_at    timestamp without time zone,
    ended_at      timestamp without time zone,
    expires_at    timestamp without time zone
);

create index if not exists idx_model_processing_job_status on model_processing_job (status);
create index if not exists idx_model_processing_job_expires_at on model_processing_job (expires_at);
