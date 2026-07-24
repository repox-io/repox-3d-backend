package io.repox.api3d.app.usecases.toolkit.domain;

public enum AssimpExportFormatEnum {
    COLLADA("collada"),
    X("x"),
    STP("stp"),
    OBJ("obj"),
    OBJNOMT("objnomt"),
    STL("stl"),
    STLB("stlb"),
    PLY("ply"),
    PLYB("plyb"),
    _3DS("3ds"),
    GLTF2("gltf2"),
    GLB2("glb2"),
    GLTF("gltf"),
    GLB("glb"),
    ASSBIN("assbin"),
    ASSXML("assxml"),
    X3D("x3d"),
    FBX("fbx"),
    FBXA("fbxa"),
    _3MF("3mf"),
    PBRT("pbrt"),
    ASSJSON("assjson");

    public final String value;

    AssimpExportFormatEnum(String value) {
        this.value = value;
    }

    public String toFormatExtension() {
        return "." + this.value;
    }
}
