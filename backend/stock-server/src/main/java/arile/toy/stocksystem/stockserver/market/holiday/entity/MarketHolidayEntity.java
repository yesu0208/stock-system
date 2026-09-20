package arile.toy.stocksystem.stockserver.market.holiday.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "market_holiday")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketHolidayEntity {

    @Id
    private LocalDate holidayDate;

    private String memo;

    private MarketHolidayEntity(LocalDate holidayDate, String memo) {
        this.holidayDate = holidayDate;
        this.memo = memo;
    }

    public static MarketHolidayEntity of(LocalDate holidayDate, String memo) {
        return new MarketHolidayEntity(holidayDate, memo);
    }
}
