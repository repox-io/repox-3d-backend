package io.repox.api3d.app.usecases.samples.adapters.ports.out;

import io.repox.api3d.app.usecases.samples.application.ports.out.FileSampleFindPort;
import io.repox.api3d.app.usecases.samples.domain.FileSampleEntity;
import io.repox.api3d.app.usecases.samples.domain.ToolEnum;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Slf4j
@Component
@RequiredArgsConstructor
class FileSamplePersistenceAdapter implements FileSampleFindPort {
    private final FileSampleRepository fileSampleRepository;

    @Override
    public List<FileSampleEntity> findAll() {
        return fileSampleRepository.findAll();
    }

    @Override
    public List<FileSampleEntity> findAllByTool(ToolEnum tool) {
        return fileSampleRepository.findAllByToolContaining(tool.name());
    }
}
