package io.repox.api3d.app.usecases.integration.xrculture.domain;

import io.repox.api3d.app.usecases.integration.xrculture.domain.loading.ModelLoadingRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.loading.ModelLoadingResponse;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.LocalSourceType;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.SourceType;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.TechnicalMetadataExtractionRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.TechnicalMetadataExtractionResponse;
import io.repox.api3d.app.utils.XmlUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "xrculture_log")
public class XrCultureLogEntity {
    public static final String TECHNICAL_METADATA_ENDPOINT = "/xrculture/details";

    /** JAXBContexts are thread-safe and expensive to build, so they are created once. */
    private static final JAXBContext TECHNICAL_METADATA_REQUEST_CONTEXT =
            contextFor(TechnicalMetadataExtractionRequest.class);

    private static final JAXBContext TECHNICAL_METADATA_RESPONSE_CONTEXT =
            contextFor(TechnicalMetadataExtractionResponse.class);

    @Id
    UUID id;

    String endpoint;
    String request;
    String response;

    @Column(name = "ip_address")
    String ipAddress;

    LocalDateTime timestamp;

    public static XrCultureLogEntity fromModelLoading(
            ModelLoadingRequest request, ModelLoadingResponse response, String remoteAddr) throws JAXBException {
        XrCultureLogEntity logEntity = new XrCultureLogEntity();
        logEntity.id = UUID.randomUUID();
        logEntity.endpoint = request.getSource().getUrlSource().getUrl();
        logEntity.request = XmlUtils.stringify(JAXBContext.newInstance(ModelLoadingRequest.class), request);
        logEntity.response = XmlUtils.stringify(JAXBContext.newInstance(ModelLoadingResponse.class), response);
        logEntity.ipAddress = remoteAddr;
        logEntity.timestamp = LocalDateTime.now();
        return logEntity;
    }

    public static XrCultureLogEntity fromTechnicalMetadata(
            TechnicalMetadataExtractionRequest request, TechnicalMetadataExtractionResponse response, String remoteAddr)
            throws JAXBException {
        XrCultureLogEntity logEntity = new XrCultureLogEntity();
        logEntity.id = UUID.randomUUID();
        logEntity.endpoint = TECHNICAL_METADATA_ENDPOINT;
        logEntity.request = XmlUtils.stringify(TECHNICAL_METADATA_REQUEST_CONTEXT, withoutFileContent(request));
        logEntity.response = XmlUtils.stringify(TECHNICAL_METADATA_RESPONSE_CONTEXT, response);
        logEntity.ipAddress = remoteAddr;
        logEntity.timestamp = LocalDateTime.now();
        return logEntity;
    }

    /**
     * Returns a shallow copy whose LocalSource carries no bytes, recording their length in the dimension attribute
     * instead. An inline model can reach hundreds of megabytes, which has no business being stringified into the audit
     * column.
     */
    private static TechnicalMetadataExtractionRequest withoutFileContent(TechnicalMetadataExtractionRequest request) {
        LocalSourceType localSource =
                request.getSource() == null ? null : request.getSource().getLocalSource();
        if (localSource == null) {
            return request;
        }

        LocalSourceType redactedSource = new LocalSourceType();
        redactedSource.setFilename(localSource.getFilename());
        redactedSource.setExtension(localSource.getExtension());
        redactedSource.setDimension(
                localSource.getValue() == null ? localSource.getDimension() : (long) localSource.getValue().length);
        redactedSource.setValue(new byte[0]);

        SourceType redactedSourceHolder = new SourceType();
        redactedSourceHolder.setLocalSource(redactedSource);

        TechnicalMetadataExtractionRequest redacted = new TechnicalMetadataExtractionRequest();
        redacted.setServiceID(request.getServiceID());
        redacted.setSessionToken(request.getSessionToken());
        redacted.setSource(redactedSourceHolder);
        redacted.setTimestamp(request.getTimestamp());
        return redacted;
    }

    private static JAXBContext contextFor(Class<?> type) {
        try {
            return JAXBContext.newInstance(type);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create a JAXBContext for " + type.getName() + ".", e);
        }
    }
}
