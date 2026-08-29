package arile.toy.stocksystem.accountserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AccountServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountServerApplication.class, args);
    }
}