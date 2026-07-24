package io.repox.api3d.app.usecases.toolkit.domain;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class AssimpInfoTechnicalDetails {
    Long memoryConsumptionB;
    Long nodes;
    Long meshes;
    Long animations;
    Long textures;
    Long materials;
    Long cameras;
    Long lights;
    Long vertices;
    Long faces;
    Long bones;
    Long animationChannels;

    private static final List<String> INFO_LINES_ENUM = List.of(
            "Memory consumption",
            "Nodes",
            "Meshes",
            "Animations",
            "Textures (embed.)",
            "Materials",
            "Cameras",
            "Lights",
            "Vertices",
            "Faces",
            "Bones",
            "Animation Channels");

    public static AssimpInfoTechnicalDetails fromOrdererValuesList(List<Long> ordererValues) {
        AssimpInfoTechnicalDetails s = new AssimpInfoTechnicalDetails();
        s.memoryConsumptionB = s.longValueIndexedAt(ordererValues, 0);
        s.nodes = s.longValueIndexedAt(ordererValues, 1);
        s.meshes = s.longValueIndexedAt(ordererValues, 2);
        s.animations = s.longValueIndexedAt(ordererValues, 3);
        s.textures = s.longValueIndexedAt(ordererValues, 4);
        s.materials = s.longValueIndexedAt(ordererValues, 5);
        s.cameras = s.longValueIndexedAt(ordererValues, 6);
        s.lights = s.longValueIndexedAt(ordererValues, 7);
        s.vertices = s.longValueIndexedAt(ordererValues, 8);
        s.faces = s.longValueIndexedAt(ordererValues, 9);
        s.bones = s.longValueIndexedAt(ordererValues, 10);
        s.animationChannels = s.longValueIndexedAt(ordererValues, 11);
        return s;
    }

    public static void collectLineValues(String line, List<Long> values) {
        if (INFO_LINES_ENUM.stream().anyMatch(line::contains)) {
            Pattern numericOnlyRegex = Pattern.compile("\\b\\d+\\b");
            Matcher matcher = numericOnlyRegex.matcher(line);
            if (matcher.find()) {
                values.add(Long.parseLong(matcher.group()));
            }
        }
    }

    private Long longValueIndexedAt(List<Long> ordererValues, int index) {
        try {
            return ordererValues.get(index);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
