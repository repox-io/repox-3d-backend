package io.repox.api3d.app.usecases.samples.application.ports;

import io.repox.api3d.app.usecases.samples.application.ports.in.FileSampleUseCase;
import io.repox.api3d.app.usecases.samples.application.ports.out.FileSampleFindPort;
import io.repox.api3d.app.usecases.samples.domain.FileSampleDto;
import io.repox.api3d.app.usecases.samples.domain.FileSampleEntity;
import io.repox.api3d.app.usecases.samples.domain.ToolEnum;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class FileSampleService implements FileSampleUseCase {
    private final FileSampleFindPort fileSampleFindPort;
    private final FileSampleEntity.FileSampleEntityMapper fileSampleEntityMapper;

    @Override
    public List<FileSampleDto> getSamples(ToolEnum tool) {
        List<FileSampleEntity> samples =
                tool == null ? fileSampleFindPort.findAll() : fileSampleFindPort.findAllByTool(tool);
        return samples.stream().map(fileSampleEntityMapper::mapFrom).toList();
    }
}
