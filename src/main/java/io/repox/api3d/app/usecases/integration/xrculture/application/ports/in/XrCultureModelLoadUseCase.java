package io.repox.api3d.app.usecases.integration.xrculture.application.ports.in;

import io.repox.api3d.app.usecases.integration.xrculture.domain.loading.ModelLoadingRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.loading.ModelLoadingResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.JAXBException;

public interface XrCultureModelLoadUseCase {
    ModelLoadingResponse fromModelLoadingRequest(ModelLoadingRequest request, HttpServletRequest ctx)
            throws JAXBException;
}
