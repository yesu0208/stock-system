package arile.toy.stocksystem.bffserver.market.holiday.dto;

import java.time.LocalDate;

public record MarketHolidayResponse(LocalDate holidayDate, String memo) {
}
