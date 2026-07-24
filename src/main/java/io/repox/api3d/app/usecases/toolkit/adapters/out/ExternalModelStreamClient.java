package io.repox.api3d.app.usecases.toolkit.adapters.out;

import io.repox.api3d.app.usecases.toolkit.application.ports.out.ExternalModelStreamPort;
import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
class ExternalModelStreamClient implements ExternalModelStreamPort {
    private final RestTemplate restTemplate;

    @Override
    public void streamExternalModel(URI url, HttpServletResponse response, OutputStream outputStream) {
        restTemplate.execute(url, HttpMethod.GET, null, responseExtractor(response, outputStream));
    }

    ResponseExtractor<Void> responseExtractor(HttpServletResponse httpServletResponse, OutputStream outputStream) {
        return (ClientHttpResponse clientHttpResponse) -> {
            // Copy status code
            httpServletResponse.setStatus(clientHttpResponse.getStatusCode().value());
            // Copy headers
            HttpHeaders headers = clientHttpResponse.getHeaders();
            headers.forEach((String name, List<String> valueList) ->
                    valueList.forEach((String value) -> httpServletResponse.addHeader(name, value)));
            if (!httpServletResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)) {
                httpServletResponse.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            }
            // Copy body by streaming.
            StreamUtils.copy(clientHttpResponse.getBody(), outputStream);
            return null;
        };
    }
}
