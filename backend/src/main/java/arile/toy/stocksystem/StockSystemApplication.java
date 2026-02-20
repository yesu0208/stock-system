package arile.toy.stocksystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockSystemApplication.class, args);
    }

}
