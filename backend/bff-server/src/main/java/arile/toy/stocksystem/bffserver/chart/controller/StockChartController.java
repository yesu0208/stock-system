package arile.toy.stocksystem.bffserver.chart.controller;

import arile.toy.stocksystem.bffserver.chart.dto.CandleData;
import arile.toy.stocksystem.bffserver.chart.service.StockChartService;
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
public class StockChartController {

    private final StockChartService stockChartService;

    /**
     * 일봉 조회
     * <p>
     * 예시:
     * GET /api/v1/chart/daily/005930?from=20260101&to=20260528
     */
    @GetMapping("/daily/{code}")
    public List<CandleData> getDailyChart(
            @PathVariable String code,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return stockChartService.getDailyChart(code, from, to);
    }
}