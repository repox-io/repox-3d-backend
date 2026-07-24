package io.repox.api3d.app.usecases.integration.xrculture;

import io.repox.api3d.app.usecases.integration.xrculture.application.ports.in.XrCultureTechnicalMetadataUseCase;
import io.repox.api3d.app.usecases.integration.xrculture.application.ports.out.XrCulturePort;
import io.repox.api3d.app.usecases.integration.xrculture.domain.XrCultureLogEntity;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.LocalSourceType;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.ObjectFactory;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.SourceType;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.TechnicalMetadataExtractionRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.TechnicalMetadataExtractionResponse;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.TechnicalMetadataExtractionResultType;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.UrlSourceType;
import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.SceneAndModelInfoUseCase;
import io.repox.api3d.app.usecases.toolkit.domain.ModelTechnicalDetailsDto;
import io.repox.api3d.app.utils.ApplicationClock;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class XrCultureTechnicalMetadataService implements XrCultureTechnicalMetadataUseCase {
    private static final String SUCCESS_MESSAGE = "Technical Metadata extracted successfully";
    private static final String DEFAULT_MODEL_NAME = "model";
    private static final String MISSING_SOURCE_MESSAGE =
            "A Source element carrying either a LocalSource or a UrlSource is required.";

    /** Thread-safe and expensive to build, so created once. */
    private static final DatatypeFactory DATATYPE_FACTORY = newDatatypeFactory();

    private final SceneAndModelInfoUseCase sceneAndModelInfoUseCase;
    private final FileStoragePort fileStorage;
    private final XrCulturePort xrCultureLogPort;
    private final ApplicationClock clock;

    @Override
    public TechnicalMetadataExtractionResponse fromTechnicalMetadataExtractionRequest(
            TechnicalMetadataExtractionRequest request, HttpServletRequest ctx) throws JAXBException {
        ObjectFactory objectFactory = new ObjectFactory();
        TechnicalMetadataExtractionResponse res = objectFactory.createTechnicalMetadataExtractionResponse();
        res.setServiceID(request.getServiceID());
        res.setSessionToken(request.getSessionToken());

        try {
            File model = materialise(request.getSource());
            // getModelTechnicalDetailsFromFile deletes the input in its own finally block.
            ModelTechnicalDetailsDto details = sceneAndModelInfoUseCase.getModelTechnicalDetailsFromFile(model);
            res.setStatus(BigInteger.valueOf(HttpStatus.OK.value()));
            res.setMessage(SUCCESS_MESSAGE);
            res.setTechnicalMetadataExtractionResult(toResult(details, objectFactory));
        } catch (Exception e) {
            HttpStatus status = statusOf(e);
            log.warn("Technical metadata extraction failed with status {}.", status.value(), e);
            res.setStatus(BigInteger.valueOf(status.value()));
            res.setMessage(StringUtils.hasText(e.getMessage()) ? e.getMessage() : status.getReasonPhrase());
        }

        res.setTimestamp(now());
        xrCultureLogPort.save(XrCultureLogEntity.fromTechnicalMetadata(request, res, ctx.getRemoteAddr()));
        return res;
    }

    private File materialise(SourceType source) throws IOException {
        if (source == null) {
            throw new KnownRuntimeError(MISSING_SOURCE_MESSAGE);
        }
        LocalSourceType localSource = source.getLocalSource();
        if (localSource != null) {
            byte[] content = localSource.getValue();
            if (content == null || content.length == 0) {
                throw new KnownRuntimeError("LocalSource carries no file content.");
            }
            return fileStorage.save(new ByteArrayInputStream(content), localFilename(localSource), true);
        }
        UrlSourceType urlSource = source.getUrlSource();
        if (urlSource == null || !StringUtils.hasText(urlSource.getUrl())) {
            throw new KnownRuntimeError(MISSING_SOURCE_MESSAGE);
        }
        return fileStorage.saveFromUri(urlSource.getUrl().trim(), true);
    }

    /**
     * The analyzer is chosen by file extension, so the name written to disk must keep one.
     */
    private static String localFilename(LocalSourceType localSource) {
        String filename = localSource.getFilename();
        if (StringUtils.hasText(filename) && StringUtils.getFilenameExtension(filename) != null) {
            return filename;
        }
        String extension = localSource.getExtension();
        if (!StringUtils.hasText(extension)) {
            throw new KnownRuntimeError(
                    "LocalSource must carry either a filename including its extension, or an extension attribute.");
        }
        String base = StringUtils.hasText(filename) ? filename : DEFAULT_MODEL_NAME;
        return base + (extension.startsWith(".") ? extension : "." + extension);
    }

    private static TechnicalMetadataExtractionResultType toResult(
            ModelTechnicalDetailsDto details, ObjectFactory objectFactory) {
        TechnicalMetadataExtractionResultType result = objectFactory.createTechnicalMetadataExtractionResultType();
        result.setMemoryConsumptionBytes(present(details.memoryConsumptionB()));
        result.setNodes(present(details.nodes()));
        result.setMeshes(present(details.meshes()));
        result.setAnimations(present(details.animations()));
        result.setTextures(present(details.textures()));
        result.setMaterials(present(details.materials()));
        result.setCameras(present(details.cameras()));
        result.setLights(present(details.lights()));
        result.setVertices(present(details.vertices()));
        result.setFaces(present(details.faces()));
        result.setBones(present(details.bones()));
        result.setAnimationChannels(present(details.animationChannels()));
        result.setPointCount(present(details.pointCount()));
        result.setGaussianCount(present(details.gaussianCount()));
        return result;
    }

    /**
     * Null and zero metrics are dropped, which is what lets the emitted element set describe the model type: a mesh
     * reports vertices and faces, a gaussian splat reports point and gaussian counts.
     */
    private static Long present(Long value) {
        return value == null || value == 0L ? null : value;
    }

    private static HttpStatus statusOf(Exception e) {
        if (e instanceof UnsupportedOperationException) {
            return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        }
        if (e instanceof KnownRuntimeError knownRuntimeError) {
            return switch (knownRuntimeError.errorType()) {
                case FORBIDDEN -> HttpStatus.FORBIDDEN;
                case NOT_FOUND -> HttpStatus.NOT_FOUND;
                case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            };
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private XMLGregorianCalendar now() {
        return DATATYPE_FACTORY.newXMLGregorianCalendar(GregorianCalendar.from(clock.nowWithZone()));
    }

    private static DatatypeFactory newDatatypeFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Unable to create a DatatypeFactory for XML timestamps.", e);
        }
    }
}
