package arile.toy.stocksystem.bffserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BffServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(BffServerApplication.class, args);
    }
}