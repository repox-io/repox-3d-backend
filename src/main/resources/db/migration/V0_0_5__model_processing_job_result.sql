alter table model_processing_job
    add column if not exists result jsonb;
