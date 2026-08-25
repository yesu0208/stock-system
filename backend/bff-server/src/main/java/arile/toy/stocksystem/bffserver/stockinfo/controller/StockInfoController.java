package arile.toy.stocksystem.bffserver.stockinfo.controller;

import arile.toy.stocksystem.bffserver.stockinfo.dto.StockInfo;
import arile.toy.stocksystem.bffserver.stockinfo.service.StockInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/stocks")
@RequiredArgsConstructor
public class StockInfoController {

    private final StockInfoService stockInfoService;

    @GetMapping("/{code}")
    public StockInfo getStockInfo(@PathVariable String code) {
        return stockInfoService.getStockInfo(code);
    }
}