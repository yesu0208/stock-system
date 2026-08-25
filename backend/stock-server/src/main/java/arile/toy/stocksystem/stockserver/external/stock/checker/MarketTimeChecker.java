package arile.toy.stocksystem.stockserver.external.stock.checker;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class MarketTimeChecker {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime OPEN = LocalTime.of(8, 50, 10);
    private static final LocalTime CLOSE = LocalTime.of(15, 39, 50);

    public boolean isMarketOpenNow() {
        ZonedDateTime now = ZonedDateTime.now(KST);

        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime time = now.toLocalTime();
        return !time.isBefore(OPEN) && time.isBefore(CLOSE);
    }
}
