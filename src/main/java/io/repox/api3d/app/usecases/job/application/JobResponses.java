package io.repox.api3d.app.usecases.job.application;

import java.net.URI;
import org.springframework.http.ResponseEntity;

/**
 * Shared construction of the {@code 202 Accepted} response for a submitted job: a {@link JobStatusDto} body
 * plus a {@code Location} header pointing at the polling endpoint. Used by every controller that enqueues
 * work so the accepted-response shape stays identical across the toolkit.
 */
public final class JobResponses {

    private JobResponses() {}

    public static ResponseEntity<JobStatusDto> accepted(JobStatusDto job) {
        return ResponseEntity.accepted()
                .location(URI.create("/jobs/" + job.id()))
                .body(job);
    }
}
