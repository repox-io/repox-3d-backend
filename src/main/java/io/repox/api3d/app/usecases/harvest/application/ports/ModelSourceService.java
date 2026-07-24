package io.repox.api3d.app.usecases.harvest.application.ports;

import io.repox.api3d.app.usecases.harvest.application.ports.in.ModelSourceUseCase;
import io.repox.api3d.app.usecases.harvest.application.ports.out.ModelRecordFindPort;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelSourceService implements ModelSourceUseCase {
    private final ModelRecordFindPort modelRecordFindPort;

    @Override
    public List<String> allModelRecordExternalUriList() {
        return modelRecordFindPort.findAllExternalUris().stream()
                .filter(Objects::nonNull)
                .map(UriComponentsBuilder::fromUriString)
                .map(UriComponentsBuilder::build)
                .map(UriComponents::getHost)
                .distinct()
                .toList();
    }
}
