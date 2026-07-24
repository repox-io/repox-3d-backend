package io.repox.api3d.app.usecases.storage.application;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Sweeps leftover temporary files once the application is ready. Files orphaned by a previous run that
 * crashed mid-processing (so their normal {@code finally}-block cleanup never ran) accumulate in the temp
 * storage directory; this removes them on startup, when the process holds no in-flight work of its own.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupTempFileCleaner {

    private final FileStoragePort fileStorage;

    @EventListener(ApplicationReadyEvent.class)
    void cleanupLeftoverTempFilesOnStartup() {
        log.info("Running startup cleanup of leftover input files...");
        fileStorage.cleanupInputDirectory();
    }
}
