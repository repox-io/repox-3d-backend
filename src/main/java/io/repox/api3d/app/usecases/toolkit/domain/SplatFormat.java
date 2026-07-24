package io.repox.api3d.app.usecases.toolkit.domain;

/**
 * Gaussian-splat container formats supported as conversion targets by GaussForge ({@code gfconvert}).
 * Choosing a compact target (SPZ/SOG/KSPLAT/COMPRESSED_PLY) is how a splat is "compressed" — GaussForge
 * exposes no separate compression step or quality knob, so conversion and compression are the same
 * operation here.
 */
public enum SplatFormat {
    PLY(".ply", false),
    COMPRESSED_PLY(".compressed.ply", true),
    SPLAT(".splat", false),
    KSPLAT(".ksplat", true),
    SPZ(".spz", true),
    SOG(".sog", true);

    private final String extension;
    private final boolean compressing;

    SplatFormat(String extension, boolean compressing) {
        this.extension = extension;
        this.compressing = compressing;
    }

    /**
     * The file extension (with leading dot) for this format. GaussForge is extension-driven, so the
     * output filename must carry this extension for the correct writer to be selected.
     */
    public String extension() {
        return extension;
    }

    /**
     * Whether this is a compressing (compact) container. {@code PLY} and {@code SPLAT} are effectively
     * raw/uncompressed, so {@code /tools/compress/splat} does not offer them as targets; the compressing
     * set is {@code COMPRESSED_PLY, KSPLAT, SPZ, SOG}. (Plain {@code /tools/convert/splat} still targets any format.)
     */
    public boolean isCompressing() {
        return compressing;
    }
}
