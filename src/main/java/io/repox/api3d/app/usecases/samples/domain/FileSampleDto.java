package io.repox.api3d.app.usecases.samples.domain;

import java.util.List;
import java.util.UUID;

public record FileSampleDto(
        UUID uuid,
        String label,
        String description,
        String modelUri,
        String thumbnailUri,
        List<ToolEnum> applicableTools) {}
