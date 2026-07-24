package io.repox.api3d.app.usecases.integration.sketchfab.domain;

import io.repox.api3d.app.usecases.harvest.domain.ExternalSourceEnum;
import io.repox.api3d.app.usecases.harvest.domain.ExternalSourceEnumRule;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
public class SketchFabSourceEnumRule implements ExternalSourceEnumRule {

    @Override
    public ExternalSourceEnum getExternalSourceEnum(String edmIsShownBy) {
        return edmIsShownBy.contains("sketchfab.com") || edmIsShownBy.contains("skfb.ly")
                ? ExternalSourceEnum.SKETCHFAB
                : null;
    }

    @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
    public interface ModelRecordSketchFabUidMapper {
        //        https://sketchfab.com/models/65ad00fb687048cdb99bbae84eeb0ef4/embed#?secret=6ulCv6RoFr%22
        default String map(String uri) {
            if (uri.contains("sketchfab.com")) {
                return UriComponentsBuilder.fromUriString(uri)
                        .build()
                        .getPathSegments()
                        .get(1);
            }
            if (uri.contains("skfb.ly")) {
                try {
                    // follow skfb.ly links to retrieve full sketchfab.com uri
                    URLConnection con = new URI(uri).toURL().openConnection();
                    con.connect();
                    try (InputStream is = con.getInputStream()) {
                        return Arrays.stream(UriComponentsBuilder.fromUri(
                                                con.getURL().toURI())
                                        .build()
                                        .getPathSegments()
                                        .get(1)
                                        .split("-"))
                                .toList()
                                .getLast();
                    } catch (Exception e) {
                        log.info(e.getMessage(), e);
                        return null;
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            return null;
        }
    }
}
