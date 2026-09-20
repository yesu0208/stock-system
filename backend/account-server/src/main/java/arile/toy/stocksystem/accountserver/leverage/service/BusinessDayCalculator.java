package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.market.holiday.HolidayRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class BusinessDayCalculator {

    private final HolidayRegistry holidayRegistry; // [신규]

    /**
     * from(포함)부터 오늘(포함)까지 경과한 영업일 수를 계산한다.
     * 관리자가 등록한 평일 휴장일도 함께 제외한다.
     */
    public int businessDaysElapsed(LocalDate from, LocalDate today) {
        int count = 0;
        LocalDate cursor = from;
        while (cursor.isBefore(today)) {
            cursor = cursor.plusDays(1);
            if (isBusinessDay(cursor)) {
                count++;
            }
        }
        return count;
    }

    public boolean isBusinessDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        return !holidayRegistry.isHoliday(date);
    }
}
