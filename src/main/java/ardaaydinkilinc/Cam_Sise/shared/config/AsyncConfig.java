package ardaaydinkilinc.Cam_Sise.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enable async processing for domain event listeners
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
