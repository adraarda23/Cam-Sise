package ardaaydinkilinc.Cam_Sise.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for asynchronous processing of domain events.
 * Enables @Async annotation support for event handlers.
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {
}
