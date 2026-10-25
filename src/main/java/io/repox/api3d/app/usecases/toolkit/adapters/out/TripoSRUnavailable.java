package io.repox.api3d.app.usecases.toolkit.adapters.out;

import io.repox.api3d.app.usecases.toolkit.application.ports.out.TripoSRProcessPort;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import java.io.File;
import org.springframework.stereotype.Component;

/**
 * Placeholder for image-to-3D reconstruction, which answers 501 on every call. TripoSR no longer runs in this
 * application; it is moving to the {@code repox-3dtools-triposr} service, and a {@code TripoSRHttpClient} replaces
 * this class once that integration lands. See {@code repox-3dtools-triposr-handover.md}.
 */
@Component
public class TripoSRUnavailable implements TripoSRProcessPort {

    static final int NOT_IMPLEMENTED = 501;

    @Override
    public File convert(File imageFile, File outputDir) {
        throw KnownRuntimeError.withStatus(
                NOT_IMPLEMENTED,
                "Image-to-3D is not available yet: TripoSR is being moved to the repox-3dtools-triposr service.");
    }
}
