package io.repox.api3d.app.usecases.job.domain;

/**
 * The kind of processing a {@link ModelProcessingJobEntity} performs. Each type is handled by a matching
 * {@code ModelJobProcessor}. {@link #producesOutput()} declares whether the type yields a downloadable
 * file, so the download endpoint can respond accordingly; a type that produces structured data instead
 * (e.g. {@link #MODEL_DETAILS}) reports {@code false} and carries its payload in the job's result.
 */
public enum ModelProcessingJobType {
    MESH_COMPRESS(true),
    MESH_CONVERT(true),
    SPLAT_CONVERT(true),
    USDZ_COMPRESS(true),
    MODEL_THUMBNAIL(true),
    MODEL_DETAILS(false);

    private final boolean producesOutput;

    ModelProcessingJobType(boolean producesOutput) {
        this.producesOutput = producesOutput;
    }

    public boolean producesOutput() {
        return producesOutput;
    }
}
