package arile.toy.stocksystem.bffserver.market.holiday.service;

import arile.toy.stocksystem.bffserver.market.holiday.client.MarketHolidayApiClient;
import arile.toy.stocksystem.bffserver.market.holiday.dto.MarketHolidayCalendarResponse;
import arile.toy.stocksystem.bffserver.market.holiday.dto.MarketHolidayResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("[Service] 일반 사용자 휴장일 조회 테스트")
class MarketHolidayQueryServiceTest {

    private final MarketHolidayApiClient marketHolidayApiClient = mock(MarketHolidayApiClient.class);

    private MarketHolidayQueryService serviceAt(String utcInstant) {
        Clock clock = Clock.fixed(Instant.parse(utcInstant), ZoneOffset.UTC);
        return new MarketHolidayQueryService(marketHolidayApiClient, clock);
    }

    @Test
    @DisplayName("오늘(KST) 이전 휴장일은 제외하고, 날짜 오름차순으로 오늘~마지막 휴장일을 반환한다")
    void givenHolidays_whenQuerying_thenReturnsFromTodayToLast() {
        given(marketHolidayApiClient.getHolidays()).willReturn(List.of(
                new MarketHolidayResponse(LocalDate.of(2026, 12, 31), "연말 휴장"),
                new MarketHolidayResponse(LocalDate.of(2026, 9, 24), "추석"),
                new MarketHolidayResponse(LocalDate.of(2026, 10, 1), "당일 휴장"),
                new MarketHolidayResponse(LocalDate.of(2026, 10, 9), "한글날")));

        // UTC 9/30 16:00 = KST 10/1 01:00 → 오늘은 10/1
        MarketHolidayCalendarResponse result = serviceAt("2026-09-30T16:00:00Z").getUpcomingHolidays();

        assertThat(result.today()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(result.lastHolidayDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(result.holidays())
                .extracting(MarketHolidayResponse::holidayDate)
                .containsExactly(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 9), LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("예정된 휴장일이 없으면 빈 목록과 lastHolidayDate null을 반환한다")
    void givenNoUpcoming_whenQuerying_thenEmpty() {
        given(marketHolidayApiClient.getHolidays()).willReturn(List.of(
                new MarketHolidayResponse(LocalDate.of(2026, 9, 24), "추석")));

        MarketHolidayCalendarResponse result = serviceAt("2026-09-30T03:00:00Z").getUpcomingHolidays();

        assertThat(result.today()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(result.lastHolidayDate()).isNull();
        assertThat(result.holidays()).isEmpty();
    }
}
