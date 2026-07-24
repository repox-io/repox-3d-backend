package io.repox.api3d.app.usecases.samples.application.ports.out;

import io.repox.api3d.app.usecases.samples.domain.FileSampleEntity;
import io.repox.api3d.app.usecases.samples.domain.ToolEnum;
import java.util.List;

public interface FileSampleFindPort {

    List<FileSampleEntity> findAll();

    List<FileSampleEntity> findAllByTool(ToolEnum tool);
}
