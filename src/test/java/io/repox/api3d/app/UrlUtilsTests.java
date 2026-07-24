package io.repox.api3d.app;

import static org.assertj.core.api.Assertions.assertThat;

import io.repox.api3d.app.utils.UrlUtils;
import java.net.URI;
import org.junit.jupiter.api.Test;

class UrlUtilsTests {

    @Test
    void alreadyEncodedUrlIsNotDoubleEncoded() {
        String url = "https://zenodo.org/api/records/18967995/files/1Kr3267-75,%20obj3.glb/content";

        URI uri = UrlUtils.toUri(url);

        // The existing %20 escape must survive verbatim and must NOT become %2520.
        assertThat(uri.getRawPath()).contains("%20").doesNotContain("%2520");
        assertThat(uri.toString()).isEqualTo(url);
    }

    @Test
    void rawUrlWithLiteralSpaceIsEncoded() {
        String url = "https://host/files/head_of_figurine (1).glb/content";

        URI uri = UrlUtils.toUri(url);

        assertThat(uri.getRawPath()).contains("%20").doesNotContain(" ");
        // The literal space is decoded back on the parsed path.
        assertThat(uri.getPath()).contains("head_of_figurine (1).glb");
    }

    @Test
    void plainUrlRoundTripsUnchanged() {
        String url = "https://example.org/model.glb";

        assertThat(UrlUtils.toUri(url).toString()).isEqualTo(url);
    }

    @Test
    void encodedQueryParamIsNotDoubleEncoded() {
        String url = "https://host/download?name=a%20b.glb";

        URI uri = UrlUtils.toUri(url);

        assertThat(uri.getRawQuery()).isEqualTo("name=a%20b.glb");
    }
}
