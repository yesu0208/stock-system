package arile.toy.stocksystem.bffserver.market.holiday.controller;

import arile.toy.stocksystem.bffserver.market.holiday.client.MarketHolidayApiClient;
import arile.toy.stocksystem.bffserver.market.holiday.dto.MarketHolidayCreateRequest;
import arile.toy.stocksystem.bffserver.market.holiday.dto.MarketHolidayResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 휴장일 관리 API. /api/v1/admin/** 이므로 SecurityConfig에서
 * 이미 ROLE_ADMIN으로 제한됨 (AdminController와 동일한 정책).
 * 실제 데이터는 stock-server가 원본으로 갖고 있고, 여기서는
 * 관리자 인가 처리 후 stock-server 내부 API로 위임
 */
@RestController
@RequestMapping("/api/v1/admin/market/holidays")
@RequiredArgsConstructor
public class MarketHolidayAdminController {

    private final MarketHolidayApiClient marketHolidayApiClient;

    @GetMapping
    public List<MarketHolidayResponse> getHolidays() {
        return marketHolidayApiClient.getHolidays();
    }

    @PostMapping
    public ResponseEntity<MarketHolidayResponse> addHoliday(@Valid @RequestBody MarketHolidayCreateRequest request) {
        var response = marketHolidayApiClient.addHoliday(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{date}")
    public ResponseEntity<Void> removeHoliday(@PathVariable LocalDate date) {
        marketHolidayApiClient.removeHoliday(date);
        return ResponseEntity.noContent().build();
    }
}
