package io.repox.api3d.appconfig.options;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("options.job")
public class JobProperties {

    /** Maximum number of processing jobs (native CLI runs) executed concurrently; excess jobs queue. */
    private int maxConcurrency = 2;

    /** Minutes a produced output remains downloadable after processing ends, before it is swept. */
    private long outputRetentionMinutes = 60;

    /** Interval between output-directory retention sweeps. */
    private long cleanupIntervalMinutes = 10;
}
