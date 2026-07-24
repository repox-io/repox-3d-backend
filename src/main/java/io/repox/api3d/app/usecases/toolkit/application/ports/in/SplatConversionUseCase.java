package io.repox.api3d.app.usecases.toolkit.application.ports.in;

import io.repox.api3d.app.usecases.toolkit.application.ConversionResult;
import io.repox.api3d.app.usecases.toolkit.domain.SplatFormat;
import java.io.File;
import org.springframework.web.multipart.MultipartFile;

public interface SplatConversionUseCase {

    ConversionResult convertSplat(MultipartFile file, SplatFormat target);

    ConversionResult convertSplatFromUri(String url, SplatFormat target);

    /** Converts an already-stored input file. The caller owns the returned output file's lifecycle. */
    ConversionResult convertSplatFromFile(File input, SplatFormat target) throws Exception;
}
