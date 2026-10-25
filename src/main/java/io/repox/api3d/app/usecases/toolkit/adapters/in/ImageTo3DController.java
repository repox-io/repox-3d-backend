package io.repox.api3d.app.usecases.toolkit.adapters.in;

import io.repox.api3d.app.usecases.toolkit.application.ports.in.ImageTo3DUseCase;
import io.repox.api3d.appconfig.OpenApiTagConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = OpenApiTagConfig.API_3D_TOOLKIT)
@RestController
@RequiredArgsConstructor
public class ImageTo3DController {

    private final ImageTo3DUseCase imageTo3DUseCase;

    @Operation(
            summary = "Convert an uploaded image to a 3D GLB model using TripoSR.",
            description = "Not available yet: answers 501 until the repox-3dtools-triposr service is integrated.")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping(
            value = "/tools/image-to-3d",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "model/gltf-binary")
    byte[] convert(@RequestParam("file") MultipartFile file) {
        return imageTo3DUseCase.convert(file);
    }
}
