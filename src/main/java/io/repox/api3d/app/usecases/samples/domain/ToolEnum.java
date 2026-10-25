package io.repox.api3d.app.usecases.samples.domain;

/**
 * A processing tool a {@link FileSampleEntity} can be used to exercise in the webapp. Deliberately kept
 * independent of {@code ModelProcessingJobType}: samples may advertise tools that are not (yet) backed by a
 * processing job, so this enum is free to hold a superset of values.
 */
public enum ToolEnum {
    COMPRESS,
    CONVERT,
    THUMBNAIL,
    DETAILS,
    VIEWER,
    GALLERY
}
