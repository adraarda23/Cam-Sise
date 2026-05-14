package ardaaydinkilinc.Cam_Sise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CamSiseApplication {

	public static void main(String[] args) {
		SpringApplication.run(CamSiseApplication.class, args);
	}

}
