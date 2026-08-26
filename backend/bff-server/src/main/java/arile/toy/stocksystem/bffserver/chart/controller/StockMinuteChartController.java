package arile.toy.stocksystem.bffserver.chart.controller;

import arile.toy.stocksystem.bffserver.chart.dto.MinuteCandle;
import arile.toy.stocksystem.bffserver.chart.service.StockMinuteChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/chart")
public class StockMinuteChartController {

    private final StockMinuteChartService stockMinuteChartService;

    /**
     * 분봉 조회
     * <p>
     * 예시:
     * GET /api/v1/chart/minute/005930?date=20260528&hour=150000
     */
    @GetMapping("/minute/{code}")
    public List<MinuteCandle> getMinute(
            @PathVariable String code,
            @RequestParam String date,
            @RequestParam String hour
    ) {
        return stockMinuteChartService.getMinuteChart(code, date, hour);
    }
}