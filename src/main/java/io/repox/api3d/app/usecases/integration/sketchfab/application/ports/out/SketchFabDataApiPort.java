package io.repox.api3d.app.usecases.integration.sketchfab.application.ports.out;

import io.repox.api3d.app.usecases.integration.sketchfab.domain.SketchFabModel;
import io.repox.api3d.app.usecases.integration.sketchfab.domain.SketchFabModelDownload;

public interface SketchFabDataApiPort {
    /**
     * Returns the detail of a model. See <a href="https://docs.sketchfab.com/data-api/v3/index.html#!/models/get_v3_models_uid">/v3/models/{uid}</a>
     * @param uid The model UID
     * @param uid
     * @return
     */
    SketchFabModel modelInfo(String uid);

    /**
     * Returns download information for downloadable models. See
     * <a href="https://docs.sketchfab.com/data-api/v3/index.html#!/models/get_v3_models_uid_download">/v3/models/{uid}/download</a>
     * @param uid The model UID
     * @return
     */
    SketchFabModelDownload modelDownload(String uid);
}
