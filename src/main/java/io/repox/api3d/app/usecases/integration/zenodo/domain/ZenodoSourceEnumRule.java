package io.repox.api3d.app.usecases.integration.zenodo.domain;

import io.repox.api3d.app.usecases.harvest.domain.ExternalSourceEnum;
import io.repox.api3d.app.usecases.harvest.domain.ExternalSourceEnumRule;

public class ZenodoSourceEnumRule implements ExternalSourceEnumRule {

    @Override
    public ExternalSourceEnum getExternalSourceEnum(String edmIsShownBy) {
        return edmIsShownBy.contains("zenodo.org") ? ExternalSourceEnum.ZENODO : null;
    }
}
