package io.repox.api3d.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.repox.api3d.app.usecases.integration.xrculture.XrCultureTechnicalMetadataService;
import io.repox.api3d.app.usecases.integration.xrculture.application.ports.out.XrCulturePort;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.LocalSourceType;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.SourceType;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.TechnicalMetadataExtractionRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.TechnicalMetadataExtractionResponse;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.TechnicalMetadataExtractionResultType;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.UrlSourceType;
import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.SceneAndModelInfoUseCase;
import io.repox.api3d.app.usecases.toolkit.domain.ModelTechnicalDetailsDto;
import io.repox.api3d.app.utils.ApplicationClock;
import io.repox.api3d.app.utils.XmlUtils;
import jakarta.xml.bind.JAXBContext;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

class XrCultureTechnicalMetadataServiceTests {

    private static final String SESSION_TOKEN = "e3be7cc2-3a7e-45e6-9a88-bd364e6de740";
    private static final BigInteger SERVICE_ID = BigInteger.valueOf(3);

    private final SceneAndModelInfoUseCase sceneAndModelInfoUseCase = mock(SceneAndModelInfoUseCase.class);
    private final FileStoragePort fileStorage = mock(FileStoragePort.class);
    private final XrCulturePort xrCultureLogPort = mock(XrCulturePort.class);
    private final ApplicationClock clock =
            new ApplicationClock(Clock.fixed(Instant.parse("2026-07-20T14:32:05Z"), ZoneOffset.UTC), ZoneOffset.UTC);

    private final XrCultureTechnicalMetadataService service =
            new XrCultureTechnicalMetadataService(sceneAndModelInfoUseCase, fileStorage, xrCultureLogPort, clock);

    private final MockHttpServletRequest ctx = new MockHttpServletRequest();

    private static File tempFile(String suffix) throws IOException {
        File file = Files.createTempFile("xrculture-details-test", suffix).toFile();
        file.deleteOnExit();
        return file;
    }

    private static TechnicalMetadataExtractionRequest localRequest(String filename, String extension, byte[] content) {
        LocalSourceType localSource = new LocalSourceType();
        localSource.setFilename(filename);
        localSource.setExtension(extension);
        localSource.setValue(content);

        SourceType source = new SourceType();
        source.setLocalSource(localSource);

        TechnicalMetadataExtractionRequest request = new TechnicalMetadataExtractionRequest();
        request.setServiceID(SERVICE_ID);
        request.setSessionToken(SESSION_TOKEN);
        request.setSource(source);
        return request;
    }

    private static ModelTechnicalDetailsDto meshDetails() {
        return new ModelTechnicalDetailsDto(3244032L, 2L, 1L, 0L, 5L, 1L, 0L, 0L, 14556L, 15452L, 0L, 0L, null, null);
    }

    private static ModelTechnicalDetailsDto splatDetails() {
        return new ModelTechnicalDetailsDto(
                null, null, null, null, null, null, null, null, null, null, null, null, 1200000L, 1200000L);
    }

    private static String marshal(TechnicalMetadataExtractionResponse response) throws Exception {
        return XmlUtils.stringify(JAXBContext.newInstance(TechnicalMetadataExtractionResponse.class), response);
    }

    @Test
    void meshRequestReportsVertexAndFaceCountsButNoGaussianCounts() throws Exception {
        when(fileStorage.save(any(), anyString(), anyBoolean())).thenReturn(tempFile(".glb"));
        when(sceneAndModelInfoUseCase.getModelTechnicalDetailsFromFile(any())).thenReturn(meshDetails());

        TechnicalMetadataExtractionResponse response = service.fromTechnicalMetadataExtractionRequest(
                localRequest("DamagedHelmet.glb", ".glb", new byte[] {1, 2, 3}), ctx);

        TechnicalMetadataExtractionResultType result = response.getTechnicalMetadataExtractionResult();
        assertThat(response.getStatus()).isEqualTo(BigInteger.valueOf(200));
        assertThat(response.getMessage()).isEqualTo("Technical Metadata extracted successfully");
        assertThat(result.getVertices()).isEqualTo(14556L);
        assertThat(result.getFaces()).isEqualTo(15452L);
        assertThat(result.getPointCount()).isNull();
        assertThat(result.getGaussianCount()).isNull();
    }

    @Test
    void splatRequestReportsGaussianCountsOnly() throws Exception {
        when(fileStorage.save(any(), anyString(), anyBoolean())).thenReturn(tempFile(".spz"));
        when(sceneAndModelInfoUseCase.getModelTechnicalDetailsFromFile(any())).thenReturn(splatDetails());

        TechnicalMetadataExtractionResponse response = service.fromTechnicalMetadataExtractionRequest(
                localRequest("cloud.spz", ".spz", new byte[] {1, 2, 3}), ctx);

        TechnicalMetadataExtractionResultType result = response.getTechnicalMetadataExtractionResult();
        assertThat(result.getPointCount()).isEqualTo(1200000L);
        assertThat(result.getGaussianCount()).isEqualTo(1200000L);
        assertThat(result.getVertices()).isNull();
        assertThat(result.getFaces()).isNull();
        assertThat(result.getMeshes()).isNull();
    }

    @Test
    void zeroValuedMetricsAreOmittedFromTheMarshalledXml() throws Exception {
        when(fileStorage.save(any(), anyString(), anyBoolean())).thenReturn(tempFile(".glb"));
        when(sceneAndModelInfoUseCase.getModelTechnicalDetailsFromFile(any())).thenReturn(meshDetails());

        String xml = marshal(service.fromTechnicalMetadataExtractionRequest(
                localRequest("DamagedHelmet.glb", ".glb", new byte[] {1, 2, 3}), ctx));

        // Animations, Cameras, Lights, Bones and AnimationChannels are all zero in meshDetails().
        assertThat(xml).contains("<Vertices>14556</Vertices>").contains("<Meshes>1</Meshes>");
        assertThat(xml)
                .doesNotContain("<Animations>")
                .doesNotContain("<Cameras>")
                .doesNotContain("<Lights>")
                .doesNotContain("<Bones>")
                .doesNotContain("<AnimationChannels>");
    }

    @Test
    void unsupportedFileTypeIsReportedAsUnsupportedMediaTypeWithNoResult() throws Exception {
        when(fileStorage.save(any(), anyString(), anyBoolean())).thenReturn(tempFile(".xyz"));
        when(sceneAndModelInfoUseCase.getModelTechnicalDetailsFromFile(any()))
                .thenThrow(new UnsupportedOperationException("Unsupported file."));

        TechnicalMetadataExtractionResponse response = service.fromTechnicalMetadataExtractionRequest(
                localRequest("model.xyz", ".xyz", new byte[] {1, 2, 3}), ctx);

        assertThat(response.getStatus()).isEqualTo(BigInteger.valueOf(415));
        assertThat(response.getMessage()).isEqualTo("Unsupported file.");
        assertThat(response.getTechnicalMetadataExtractionResult()).isNull();
    }

    @Test
    void serviceIdAndSessionTokenAreEchoedBackOnSuccessAndOnFailure() throws Exception {
        when(fileStorage.save(any(), anyString(), anyBoolean())).thenReturn(tempFile(".glb"));
        when(sceneAndModelInfoUseCase.getModelTechnicalDetailsFromFile(any()))
                .thenReturn(meshDetails())
                .thenThrow(new UnsupportedOperationException("Unsupported file."));

        TechnicalMetadataExtractionRequest request = localRequest("DamagedHelmet.glb", ".glb", new byte[] {1, 2, 3});

        TechnicalMetadataExtractionResponse success = service.fromTechnicalMetadataExtractionRequest(request, ctx);
        TechnicalMetadataExtractionResponse failure = service.fromTechnicalMetadataExtractionRequest(request, ctx);

        assertThat(success.getServiceID()).isEqualTo(SERVICE_ID);
        assertThat(success.getSessionToken()).isEqualTo(SESSION_TOKEN);
        assertThat(failure.getServiceID()).isEqualTo(SERVICE_ID);
        assertThat(failure.getSessionToken()).isEqualTo(SESSION_TOKEN);
    }

    @Test
    void filenameWrittenToStorageKeepsAnExtensionSoTheAnalyzerCanBeChosen() throws Exception {
        when(fileStorage.save(any(), anyString(), anyBoolean())).thenReturn(tempFile(".glb"));
        when(sceneAndModelInfoUseCase.getModelTechnicalDetailsFromFile(any())).thenReturn(meshDetails());
        ArgumentCaptor<String> filename = ArgumentCaptor.forClass(String.class);

        // Filename attribute absent, so the extension attribute has to supply it.
        service.fromTechnicalMetadataExtractionRequest(localRequest(null, "glb", new byte[] {1, 2, 3}), ctx);

        verify(fileStorage).save(any(), filename.capture(), anyBoolean());
        assertThat(filename.getValue()).endsWith(".glb");
    }

    @Test
    void localSourceWithoutAnyExtensionIsRejectedAsBadRequest() throws Exception {
        TechnicalMetadataExtractionResponse response =
                service.fromTechnicalMetadataExtractionRequest(localRequest("model", null, new byte[] {1, 2, 3}), ctx);

        assertThat(response.getStatus()).isEqualTo(BigInteger.valueOf(400));
        assertThat(response.getTechnicalMetadataExtractionResult()).isNull();
    }

    @Test
    void emptyLocalSourceContentIsRejectedAsBadRequest() throws Exception {
        TechnicalMetadataExtractionResponse response =
                service.fromTechnicalMetadataExtractionRequest(localRequest("model.glb", ".glb", new byte[0]), ctx);

        assertThat(response.getStatus()).isEqualTo(BigInteger.valueOf(400));
    }

    @Test
    void missingSourceIsRejectedAsBadRequest() throws Exception {
        TechnicalMetadataExtractionRequest request = new TechnicalMetadataExtractionRequest();
        request.setServiceID(SERVICE_ID);
        request.setSessionToken(SESSION_TOKEN);

        TechnicalMetadataExtractionResponse response = service.fromTechnicalMetadataExtractionRequest(request, ctx);

        assertThat(response.getStatus()).isEqualTo(BigInteger.valueOf(400));
    }

    @Test
    void urlSourceIsFetchedThroughFileStorage() throws Exception {
        UrlSourceType urlSource = new UrlSourceType();
        urlSource.setUrl("https://example.org/model.glb");
        SourceType source = new SourceType();
        source.setUrlSource(urlSource);
        TechnicalMetadataExtractionRequest request = new TechnicalMetadataExtractionRequest();
        request.setServiceID(SERVICE_ID);
        request.setSessionToken(SESSION_TOKEN);
        request.setSource(source);

        when(fileStorage.saveFromUri(anyString(), anyBoolean())).thenReturn(tempFile(".glb"));
        when(sceneAndModelInfoUseCase.getModelTechnicalDetailsFromFile(any())).thenReturn(meshDetails());

        TechnicalMetadataExtractionResponse response = service.fromTechnicalMetadataExtractionRequest(request, ctx);

        assertThat(response.getStatus()).isEqualTo(BigInteger.valueOf(200));
        verify(fileStorage).saveFromUri("https://example.org/model.glb", true);
    }

    @Test
    void timestampIsSetFromTheApplicationClock() throws Exception {
        when(fileStorage.save(any(), anyString(), anyBoolean())).thenReturn(tempFile(".glb"));
        when(sceneAndModelInfoUseCase.getModelTechnicalDetailsFromFile(any())).thenReturn(meshDetails());

        TechnicalMetadataExtractionResponse response = service.fromTechnicalMetadataExtractionRequest(
                localRequest("DamagedHelmet.glb", ".glb", new byte[] {1, 2, 3}), ctx);

        assertThat(response.getTimestamp().toGregorianCalendar().toInstant())
                .isEqualTo(Instant.parse("2026-07-20T14:32:05Z"));
    }

    @Test
    void everyRequestIsAudited() throws Exception {
        when(fileStorage.save(any(), anyString(), anyBoolean())).thenReturn(tempFile(".glb"));
        when(sceneAndModelInfoUseCase.getModelTechnicalDetailsFromFile(any())).thenReturn(meshDetails());

        service.fromTechnicalMetadataExtractionRequest(
                localRequest("DamagedHelmet.glb", ".glb", new byte[] {1, 2, 3}), ctx);

        verify(xrCultureLogPort).save(any());
    }
}
