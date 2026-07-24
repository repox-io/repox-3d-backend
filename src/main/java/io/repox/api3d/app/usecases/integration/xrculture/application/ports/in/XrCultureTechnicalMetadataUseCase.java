package io.repox.api3d.app.usecases.integration.xrculture.application.ports.in;

import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.TechnicalMetadataExtractionRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.TechnicalMetadataExtractionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.JAXBException;

public interface XrCultureTechnicalMetadataUseCase {
    TechnicalMetadataExtractionResponse fromTechnicalMetadataExtractionRequest(
            TechnicalMetadataExtractionRequest request, HttpServletRequest ctx) throws JAXBException;
}
