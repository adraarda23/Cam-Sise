package ardaaydinkilinc.Cam_Sise;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Smoke Test")
class SmokeTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
        assertThat(context).isNotNull();
        assertThat(context.getBeanDefinitionCount()).isGreaterThan(0);
    }
}
