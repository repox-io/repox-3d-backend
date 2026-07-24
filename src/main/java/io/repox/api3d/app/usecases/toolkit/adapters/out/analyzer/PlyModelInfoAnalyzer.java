package io.repox.api3d.app.usecases.toolkit.adapters.out.analyzer;

import io.repox.api3d.app.usecases.toolkit.application.ports.out.ModelInfoAnalyzer;
import io.repox.api3d.app.usecases.toolkit.domain.ModelTechnicalDetailsDto;
import io.repox.api3d.app.usecases.toolkit.domain.PlyHeader;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Extracts vertex/point and face counts from a PLY file by parsing its ASCII header via {@link PlyHeader}.
 * When the header carries the 3D Gaussian Splatting signature each vertex is a gaussian, so the vertex
 * count is also reported as the gaussian count.
 */
@Slf4j
@Component
class PlyModelInfoAnalyzer implements ModelInfoAnalyzer {

    private static final Set<String> EXTENSIONS = Set.of("ply");

    @Override
    public Set<String> supportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    public ModelTechnicalDetailsDto analyze(File file) throws IOException {
        PlyHeader header;
        try (InputStream in = Files.newInputStream(file.toPath())) {
            header = PlyHeader.parse(in)
                    .orElseThrow(() -> new KnownRuntimeError("Not a valid PLY file: " + file.getName()));
        }

        Long vertexCount = header.vertexCount();
        if (vertexCount == null) {
            throw new KnownRuntimeError("PLY header missing vertex element: " + file.getName());
        }

        return new ModelTechnicalDetailsDto(
                null, // memoryConsumptionB
                null, // nodes
                1L, // meshes
                null, // animations
                null, // textures
                null, // materials
                null, // cameras
                null, // lights
                vertexCount, // vertices
                header.faceCount(), // faces
                null, // bones
                null, // animationChannels
                vertexCount, // pointCount
                header.gaussianSplat() ? vertexCount : null); // gaussianCount
    }
}
