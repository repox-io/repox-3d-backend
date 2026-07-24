package io.repox.api3d.app.usecases.toolkit.application.ports.in;

import io.repox.api3d.app.usecases.toolkit.application.ConversionResult;
import io.repox.api3d.app.usecases.toolkit.domain.MeshExportFormat;
import java.io.File;
import org.springframework.web.multipart.MultipartFile;

public interface MeshConversionUseCase {

    ConversionResult convertMesh(MultipartFile file, MeshExportFormat target);

    ConversionResult convertMeshFromUri(String url, MeshExportFormat target);

    /** Converts an already-stored input file. The caller owns the returned output file's lifecycle. */
    ConversionResult convertMeshFromFile(File input, MeshExportFormat target) throws Exception;
}
