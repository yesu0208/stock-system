package arile.toy.stocksystem.bffserver.market.holiday.service;

import arile.toy.stocksystem.bffserver.market.holiday.client.MarketHolidayApiClient;
import arile.toy.stocksystem.bffserver.market.holiday.dto.MarketHolidayCalendarResponse;
import arile.toy.stocksystem.bffserver.market.holiday.dto.MarketHolidayResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketHolidayQueryService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final MarketHolidayApiClient marketHolidayApiClient;
    private final Clock clock;

    /** 오늘(KST)부터 마지막으로 등록된 휴장일까지의 휴장일 목록 */
    public MarketHolidayCalendarResponse getUpcomingHolidays() {
        LocalDate today = LocalDate.now(clock.withZone(KST));

        List<MarketHolidayResponse> upcoming = marketHolidayApiClient.getHolidays().stream()
                .filter(h -> !h.holidayDate().isBefore(today))
                .sorted(Comparator.comparing(MarketHolidayResponse::holidayDate))
                .toList();

        return MarketHolidayCalendarResponse.of(today, upcoming);
    }
}
