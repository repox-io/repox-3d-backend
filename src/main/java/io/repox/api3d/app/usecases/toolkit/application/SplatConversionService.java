package io.repox.api3d.app.usecases.toolkit.application;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.SplatConversionUseCase;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.SplatConversionPort;
import io.repox.api3d.app.usecases.toolkit.domain.SplatFormat;
import java.io.File;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class SplatConversionService implements SplatConversionUseCase {

    private final FileStoragePort fileStorage;
    private final SplatConversionPort splatConversionPort;

    @Override
    public ConversionResult convertSplat(MultipartFile file, SplatFormat target) {
        try (InputStream inputStream = file.getInputStream()) {
            return convertSplatFromFile(fileStorage.save(inputStream, file.getOriginalFilename(), true), target);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ConversionResult convertSplatFromUri(String url, SplatFormat target) {
        try {
            return convertSplatFromFile(fileStorage.saveFromUri(url, true), target);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ConversionResult convertSplatFromFile(File input, SplatFormat target) throws Exception {
        try {
            File output = splatConversionPort.convert(input, target);
            return ConversionResult.forOutput(output, () -> fileStorage.delete(output));
        } finally {
            fileStorage.delete(input);
        }
    }
}
