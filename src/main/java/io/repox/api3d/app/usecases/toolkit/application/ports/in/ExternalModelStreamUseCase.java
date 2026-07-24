package io.repox.api3d.app.usecases.toolkit.application.ports.in;

import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URI;

public interface ExternalModelStreamUseCase {
    void streamExternalModel(URI url, HttpServletResponse response, OutputStream outputStream);
}
