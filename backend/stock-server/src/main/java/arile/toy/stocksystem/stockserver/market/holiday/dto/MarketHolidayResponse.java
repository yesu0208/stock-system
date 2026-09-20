package arile.toy.stocksystem.stockserver.market.holiday.dto;

import arile.toy.stocksystem.stockserver.market.holiday.entity.MarketHolidayEntity;

import java.time.LocalDate;

public record MarketHolidayResponse(LocalDate holidayDate, String memo) {
    public static MarketHolidayResponse fromEntity(MarketHolidayEntity entity) {
        return new MarketHolidayResponse(entity.getHolidayDate(), entity.getMemo());
    }
}
