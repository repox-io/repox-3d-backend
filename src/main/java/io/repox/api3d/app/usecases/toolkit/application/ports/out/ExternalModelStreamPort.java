package io.repox.api3d.app.usecases.toolkit.application.ports.out;

import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URI;

public interface ExternalModelStreamPort {
    void streamExternalModel(URI url, HttpServletResponse response, OutputStream outputStream);
}
