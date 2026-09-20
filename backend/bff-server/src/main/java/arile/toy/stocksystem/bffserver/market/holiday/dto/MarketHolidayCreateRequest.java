package arile.toy.stocksystem.bffserver.market.holiday.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MarketHolidayCreateRequest(@NotNull LocalDate holidayDate, String memo) {
}
