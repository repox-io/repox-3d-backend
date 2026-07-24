package io.repox.api3d.app.usecases.integration.sketchfab.domain;

import java.util.List;
import java.util.Map;

public record SketchFabModel(
        int animationCount,
        List<Category> categories,
        int commentCount,
        String createdAt,
        String embedUrl,
        int faceCount,
        boolean isAgeRestricted,
        License license,
        int likeCount,
        String name,
        String publishedAt,
        int soundCount,
        String staffpickedAt,
        List<Tag> tags,
        Thumbnails thumbnails,
        String uid,
        String uri,
        User user,
        int vertexCount,
        int viewCount,
        String viewerUrl,
        boolean isDownloadable,
        boolean isProtected,
        String description,
        Object price,
        int downloadCount,
        String editorUrl,
        Status status,
        String source,
        boolean hasCommentsDisabled,
        String updatedAt,
        Object pbrType,
        int textureCount,
        int materialCount) {

    public record Category(String uid, String name, String slug, String uri, String icon, List<Thumbnail> thumbnails) {
        public record Thumbnail(String width, String height, String url) {}
    }

    public record License(String uri, String label, String fullName, String requirements, String url, String slug) {}

    public record Tag(String name, String slug, String uri) {}

    public record Thumbnails(List<Image> images) {
        public record Image(String uid, int width, int height, int size, String url) {}
    }

    public record User(
            String uid,
            String username,
            String displayName,
            String account,
            String profileUrl,
            Avatar avatar,
            String uri) {
        public record Avatar(String uid, String uri, List<Image> images) {
            public record Image(int width, int height, int size, String url) {}
        }
    }

    public record Status(Map<String, Object> warning, String processing) {}
}
