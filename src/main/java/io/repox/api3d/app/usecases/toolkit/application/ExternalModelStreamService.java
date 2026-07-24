package io.repox.api3d.app.usecases.toolkit.application;

import io.repox.api3d.app.usecases.toolkit.application.ports.in.ExternalModelStreamUseCase;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.ExternalModelStreamPort;
import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalModelStreamService implements ExternalModelStreamUseCase {
    private final ExternalModelStreamPort externalModelStreamPort;

    @Override
    public void streamExternalModel(URI url, HttpServletResponse response, OutputStream outputStream) {
        externalModelStreamPort.streamExternalModel(url, response, outputStream);
    }
}
