package io.repox.api3d.app.usecases.integration.xrculture;

import io.repox.api3d.app.usecases.integration.xrculture.application.ports.in.XrCultureModelLoadUseCase;
import io.repox.api3d.app.usecases.integration.xrculture.application.ports.out.XrCulturePort;
import io.repox.api3d.app.usecases.integration.xrculture.domain.XrCultureLogEntity;
import io.repox.api3d.app.usecases.integration.xrculture.domain.loading.ModelLoadingRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.loading.ModelLoadingResponse;
import io.repox.api3d.app.usecases.integration.xrculture.domain.loading.ObjectFactory;
import io.repox.api3d.app.utils.ApplicationVariableSettings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.JAXBException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class XrCultureModelLoadingService implements XrCultureModelLoadUseCase {
    private final XrCulturePort xrCultureLogPort;
    private final ApplicationVariableSettings settings;

    @Override
    public ModelLoadingResponse fromModelLoadingRequest(ModelLoadingRequest request, HttpServletRequest ctx)
            throws JAXBException {
        ObjectFactory objectFactory = new ObjectFactory();
        ModelLoadingResponse res = objectFactory.createModelLoadingResponse();
        res.setStatus(BigInteger.valueOf(200));
        res.setMessage("Model loading request has been executed successfully");
        res.setEndpoint(UriComponentsBuilder.fromUriString("https://3d.repox.io/loader")
                .queryParam(
                        "url",
                        Base64.getEncoder()
                                .encodeToString(UriComponentsBuilder.fromUriString(settings.apiUrl())
                                        .pathSegment("model", "retrieve")
                                        .queryParam(
                                                "url",
                                                request.getSource()
                                                        .getUrlSource()
                                                        .getUrl())
                                        .build()
                                        .toUriString()
                                        .getBytes(StandardCharsets.UTF_8)))
                .toUriString());
        xrCultureLogPort.save(XrCultureLogEntity.fromModelLoading(request, res, ctx.getRemoteAddr()));
        return res;
    }
}
