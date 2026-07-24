package io.repox.api3d.app.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public interface HasJsonValue {
    ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    default <T> T convertValue(Object value, TypeReference<T> type) {
        return mapper.convertValue(value, type);
    }

    default <T> T convertValue(Object value, Class<T> type) {
        return mapper.convertValue(value, type);
    }

    default JsonNode getAsNode(Object object) {
        return mapper.valueToTree(object);
    }

    default JsonNode fromString(String object) throws JsonProcessingException {
        return mapper.readTree(object);
    }
}
