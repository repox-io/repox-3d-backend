package io.repox.api3d.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.repox.api3d.app.utils.ModelSourceUtils;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ModelSourceUtilsTests {

    private static final String URL = "https://example.org/model.glb";

    private static MultipartFile file(byte[] content) {
        return new MockMultipartFile("file", "model.glb", "model/gltf-binary", content);
    }

    @Test
    void uploadOnlyIsAnUpload() {
        assertThat(ModelSourceUtils.isUpload(file(new byte[] {1, 2, 3}), null)).isTrue();
    }

    @Test
    void urlOnlyIsNotAnUpload() {
        assertThat(ModelSourceUtils.isUpload(null, URL)).isFalse();
    }

    @Test
    void bothSourcesAreRejectedAsBadRequest() {
        assertThatThrownBy(() -> ModelSourceUtils.isUpload(file(new byte[] {1, 2, 3}), URL))
                .isInstanceOf(KnownRuntimeError.class)
                .hasMessage("Provide either a file or a url, but not both.")
                .extracting(e -> ((KnownRuntimeError) e).errorType())
                .isEqualTo(KnownRuntimeError.ErrorType.BAD_REQUEST);
    }

    @Test
    void neitherSourceIsRejectedAsBadRequest() {
        assertThatThrownBy(() -> ModelSourceUtils.isUpload(null, null))
                .isInstanceOf(KnownRuntimeError.class)
                .extracting(e -> ((KnownRuntimeError) e).errorType())
                .isEqualTo(KnownRuntimeError.ErrorType.BAD_REQUEST);
    }

    @Test
    void emptyFilePartAlongsideUrlIsTreatedAsAbsent() {
        assertThat(ModelSourceUtils.isUpload(file(new byte[0]), URL)).isFalse();
    }

    @Test
    void blankUrlAlongsideUploadIsTreatedAsAbsent() {
        assertThat(ModelSourceUtils.isUpload(file(new byte[] {1, 2, 3}), "   ")).isTrue();
    }

    @Test
    void emptyFilePartAndBlankUrlIsRejected() {
        assertThatThrownBy(() -> ModelSourceUtils.isUpload(file(new byte[0]), "   "))
                .isInstanceOf(KnownRuntimeError.class);
    }
}
