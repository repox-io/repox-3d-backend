package io.repox.api3d.app.usecases.toolkit.domain;

/**
 * Broad classification of a 3D model input. A {@code MESH} is polygonal geometry (glTF/OBJ/STL/USD/…);
 * a {@code SPLAT} is a 3D Gaussian Splatting scene (SPZ/KSPLAT/gaussian PLY/…). The two families are
 * served by disjoint toolchains, so classifying the input up front tells a caller which conversion
 * endpoints and target formats apply.
 */
public enum ModelKind {
    MESH,
    SPLAT
}
