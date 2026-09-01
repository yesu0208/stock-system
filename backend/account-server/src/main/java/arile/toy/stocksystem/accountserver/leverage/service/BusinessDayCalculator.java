package arile.toy.stocksystem.accountserver.leverage.service;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class BusinessDayCalculator {

    /**
     * from(포함)부터 오늘(포함)까지 경과한 영업일 수를 계산한다.
     * 공휴일은 고려하지 않는다(프로젝트 전반의 기존 단순화 수준과 동일).
     * 예: from == 오늘이면 0, from 다음 영업일이 오늘이면 1.
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
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }
}
