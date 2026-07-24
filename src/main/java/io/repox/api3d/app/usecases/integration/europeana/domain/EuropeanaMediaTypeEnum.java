package io.repox.api3d.app.usecases.integration.europeana.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EuropeanaMediaTypeEnum {
    IMAGE("IMAGE"),
    TEXT("TEXT"),
    VIDEO("VIDEO"),
    SOUND("SOUND"),
    _3D("3D");

    final String value;

    EuropeanaMediaTypeEnum(String s) {
        this.value = s;
    }

    @Override
    public String toString() {
        return this.value;
    }

    @JsonCreator
    public static EuropeanaMediaTypeEnum fromValue(String s) {
        for (EuropeanaMediaTypeEnum e : EuropeanaMediaTypeEnum.values()) {
            if (e.value.equals(s)) {
                return e;
            }
        }
        return null;
    }
}
