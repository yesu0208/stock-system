package arile.toy.stocksystem.stockserver.market.holiday.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MarketHolidayCreateRequest(@NotNull LocalDate holidayDate, String memo) {
}
