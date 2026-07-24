package io.repox.api3d.app.usecases.toolkit.application.ports.in;

import io.repox.api3d.app.usecases.toolkit.application.ConversionResult;
import io.repox.api3d.app.usecases.toolkit.domain.UsdzOptimizeOptions;
import java.io.File;
import org.springframework.web.multipart.MultipartFile;

public interface UsdzCompressionUseCase {

    ConversionResult compressUsdz(MultipartFile file, UsdzOptimizeOptions options);

    ConversionResult compressUsdzFromUri(String url, UsdzOptimizeOptions options);

    /** Optimizes an already-stored input file. The caller owns the returned output file's lifecycle. */
    ConversionResult compressUsdzFromFile(File input, UsdzOptimizeOptions options) throws Exception;
}
