package io.repox.api3d.app.usecases.samples.application.ports.in;

import io.repox.api3d.app.usecases.samples.domain.FileSampleDto;
import io.repox.api3d.app.usecases.samples.domain.ToolEnum;
import java.util.List;

public interface FileSampleUseCase {

    /**
     * Lists 3D model file samples. When {@code tool} is {@code null} all samples are returned; otherwise only
     * samples whose applicable tools contain the given value.
     */
    List<FileSampleDto> getSamples(ToolEnum tool);
}
