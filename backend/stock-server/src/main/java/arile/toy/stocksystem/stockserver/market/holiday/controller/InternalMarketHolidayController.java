package arile.toy.stocksystem.stockserver.market.holiday.controller;

import arile.toy.stocksystem.stockserver.market.holiday.dto.MarketHolidayCreateRequest;
import arile.toy.stocksystem.stockserver.market.holiday.dto.MarketHolidayResponse;
import arile.toy.stocksystem.stockserver.market.holiday.service.MarketHolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/internal/market/holidays")
@RequiredArgsConstructor
public class InternalMarketHolidayController {

    private final MarketHolidayService marketHolidayService;

    @GetMapping
    public List<MarketHolidayResponse> getHolidays() {
        return marketHolidayService.getAll().stream()
                .map(MarketHolidayResponse::fromEntity)
                .toList();
    }

    @PostMapping
    public ResponseEntity<MarketHolidayResponse> addHoliday(@Valid @RequestBody MarketHolidayCreateRequest request) {
        var entity = marketHolidayService.addHoliday(request.holidayDate(), request.memo());
        return ResponseEntity.status(HttpStatus.CREATED).body(MarketHolidayResponse.fromEntity(entity));
    }

    @DeleteMapping("/{date}")
    public ResponseEntity<Void> removeHoliday(@PathVariable LocalDate date) {
        marketHolidayService.removeHoliday(date);
        return ResponseEntity.noContent().build();
    }
}
