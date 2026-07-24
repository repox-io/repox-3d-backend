package io.repox.api3d.app.usecases.job.configuration;

import io.repox.api3d.appconfig.options.JobProperties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Background execution for processing jobs. A fixed pool of platform threads bounds how many native
 * CLI runs (gltfpack/Blender/Assimp) execute at once; excess submissions queue. Platform (not virtual)
 * threads are used because the heavy work is CPU-bound in the subprocesses. Also enables scheduling for
 * the output retention sweep.
 */
@Configuration
@EnableScheduling
public class JobExecutionConfig {

    @Bean(name = "jobExecutor", destroyMethod = "shutdown")
    public ExecutorService jobExecutor(JobProperties jobProperties) {
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "model-processing-job-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(Math.max(1, jobProperties.getMaxConcurrency()), threadFactory);
    }
}
