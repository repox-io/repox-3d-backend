package io.repox.api3d.app.usecases.toolkit.domain;

public record ModelTechnicalDetailsDto(
        Long memoryConsumptionB,
        Long nodes,
        Long meshes,
        Long animations,
        Long textures,
        Long materials,
        Long cameras,
        Long lights,
        Long vertices,
        Long faces,
        Long bones,
        Long animationChannels,
        Long pointCount,
        Long gaussianCount) {}
