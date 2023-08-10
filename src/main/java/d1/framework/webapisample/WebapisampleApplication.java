package d1.framework.webapisample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("d1")
@EntityScan("d1")
public class WebapisampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebapisampleApplication.class, args);
    }
}
