package arile.toy.stocksystem.bffserver.market.holiday.controller;

import arile.toy.stocksystem.bffserver.market.holiday.dto.MarketHolidayCalendarResponse;
import arile.toy.stocksystem.bffserver.market.holiday.service.MarketHolidayQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 일반 사용자용 휴장일 조회 API (로그인 사용자 전체).
 * 등록/삭제는 관리자 전용 MarketHolidayAdminController에서만 가능
 */
@RestController
@RequestMapping("/api/v1/market/holidays")
@RequiredArgsConstructor
public class MarketHolidayController {

    private final MarketHolidayQueryService marketHolidayQueryService;

    @GetMapping
    public MarketHolidayCalendarResponse getUpcomingHolidays() {
        return marketHolidayQueryService.getUpcomingHolidays();
    }
}
