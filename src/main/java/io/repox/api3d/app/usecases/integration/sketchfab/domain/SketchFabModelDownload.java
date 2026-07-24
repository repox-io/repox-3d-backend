package io.repox.api3d.app.usecases.integration.sketchfab.domain;

public record SketchFabModelDownload(Asset source, Asset gltf, Asset usdz, Asset glb) {
    public record Asset(String url, long size, int expires) {}
}
