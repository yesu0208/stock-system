package arile.toy.stocksystem.bffserver.stockinfo.controller;

import arile.toy.stocksystem.bffserver.stockinfo.dto.StockInfo;
import arile.toy.stocksystem.bffserver.stockinfo.dto.TradePageResponse;
import arile.toy.stocksystem.bffserver.stockinfo.service.StockInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/stocks")
@RequiredArgsConstructor
public class StockInfoController {

    private final StockInfoService stockInfoService;

    @GetMapping("/{code}")
    public StockInfo getStockInfo(@PathVariable String code) {
        return stockInfoService.getStockInfo(code);
    }

    @GetMapping("/{code}/foreign")
    public TradePageResponse getForeignTrades(
            @PathVariable String code,
            @RequestParam(defaultValue = "1") int page
    ) {
        return stockInfoService.getForeignInstitutionTrades(code, page);
    }
}