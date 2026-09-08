package arile.toy.stocksystem.bffserver.portfolio.service;

import arile.toy.stocksystem.bffserver.account.dto.AccountSnapshot;
import arile.toy.stocksystem.bffserver.account.dto.LeveragePositionInfo;
import arile.toy.stocksystem.bffserver.account.dto.StockInfo;
import arile.toy.stocksystem.bffserver.account.service.AccountPullService;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerStockSummaryTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerStockSummaryRepository;
import arile.toy.stocksystem.bffserver.portfolio.dto.PortfolioResponse;
import arile.toy.stocksystem.bffserver.portfolio.dto.PortfolioSectorItem;
import arile.toy.stocksystem.bffserver.portfolio.dto.PortfolioStockItem;
import arile.toy.stocksystem.bffserver.sector.SectorRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioCalculator {

    private final AccountPullService accountPullService;
    private final BffServerStockSummaryRepository stockSummaryRepository;
    private final SectorRegistry sectorRegistry;

    public PortfolioResponse calculate(String username) {

        AccountSnapshot snapshot = accountPullService.getAccountMessage(username);

        long cash = snapshot.availableCash() + snapshot.reservedCash();

        Map<String, Integer> priceCache = new HashMap<>();
        Map<String, StockAmount> mergedByStock = new LinkedHashMap<>();

        accumulateSpot(snapshot.stocks(), priceCache, mergedByStock);
        accumulateLeverage(snapshot.leveragePositions(), priceCache, mergedByStock);

        long stockTotalValue = mergedByStock.values().stream()
                .mapToLong(StockAmount::total)
                .sum();

        long totalAssetValue = cash + stockTotalValue;

        double cashRatio = ratio(cash, totalAssetValue);

        List<PortfolioSectorItem> sectorItems = buildSectorItems(mergedByStock, totalAssetValue);

        // 업종 평가금액이 큰 순으로 정렬 (편의성)
        sectorItems.sort((a, b) -> Long.compare(b.evaluationAmount(), a.evaluationAmount()));

        return new PortfolioResponse(username, totalAssetValue, cash, cashRatio, sectorItems);
    }

    /** 현물 보유 종목의 평가금액을 stockCode 기준으로 누적 */
    private void accumulateSpot(
            Map<String, StockInfo> stocks,
            Map<String, Integer> priceCache,
            Map<String, StockAmount> mergedByStock
    ) {
        if (stocks == null) return;

        for (Map.Entry<String, StockInfo> entry : stocks.entrySet()) {
            String stockCode = entry.getKey();
            StockInfo stockInfo = entry.getValue();

            Integer curPrice = resolvePrice(stockCode, priceCache);
            if (curPrice == null) {
                log.warn("No stock summary for {}. Skip in portfolio calculation.", stockCode);
                continue;
            }

            long evaluationAmount = (long) stockInfo.quantity() * curPrice;

            mergedByStock.computeIfAbsent(stockCode, k -> new StockAmount())
                    .addSpot(evaluationAmount);
        }
    }

    /**
     * 레버리지 포지션의 평가금액을 stockCode 기준으로 누적
     * 대출금(loanAmount)은 차감하지 않고 quantity × curPrice 그대로 반영
     * 포지션 key는 "stockCode:leverageRatio" 형식이며, 동일 종목의 여러 배율 포지션은 합산
     */
    private void accumulateLeverage(
            Map<String, LeveragePositionInfo> leveragePositions,
            Map<String, Integer> priceCache,
            Map<String, StockAmount> mergedByStock
    ) {
        if (leveragePositions == null) return;

        for (Map.Entry<String, LeveragePositionInfo> entry : leveragePositions.entrySet()) {
            String[] parts = entry.getKey().split(":", 2);
            if (parts.length != 2) {
                log.warn("Malformed leverage position key: {}", entry.getKey());
                continue;
            }
            String stockCode = parts[0];
            LeveragePositionInfo info = entry.getValue();

            Integer curPrice = resolvePrice(stockCode, priceCache);
            if (curPrice == null) {
                log.warn("No stock summary for leverage position stockCode={}. Skip.", stockCode);
                continue;
            }

            long evaluationAmount = (long) info.quantity() * curPrice;

            mergedByStock.computeIfAbsent(stockCode, k -> new StockAmount())
                    .addLeverage(evaluationAmount);
        }
    }

    private Integer resolvePrice(String stockCode, Map<String, Integer> priceCache) {
        return priceCache.computeIfAbsent(stockCode, code -> {
            BffServerStockSummaryTickMessage summary = stockSummaryRepository.findByStockCode(code);
            return summary == null ? null : summary.curPrice();
        });
    }

    private List<PortfolioSectorItem> buildSectorItems(
            Map<String, StockAmount> mergedByStock, long totalAssetValue
    ) {
        // 업종 -> (stockCode, StockAmount) 그룹핑
        Map<String, List<Map.Entry<String, StockAmount>>> sectorGroups = new LinkedHashMap<>();

        for (Map.Entry<String, StockAmount> entry : mergedByStock.entrySet()) {
            String sector = sectorRegistry.resolveSector(entry.getKey());
            sectorGroups.computeIfAbsent(sector, k -> new ArrayList<>()).add(entry);
        }

        List<PortfolioSectorItem> sectorItems = new ArrayList<>();

        for (Map.Entry<String, List<Map.Entry<String, StockAmount>>> sectorEntry : sectorGroups.entrySet()) {
            String sector = sectorEntry.getKey();
            List<Map.Entry<String, StockAmount>> stockEntries = sectorEntry.getValue();

            long sectorValue = stockEntries.stream()
                    .mapToLong(e -> e.getValue().total())
                    .sum();

            List<PortfolioStockItem> stockItems = stockEntries.stream()
                    .map(e -> {
                        StockAmount amount = e.getValue();
                        return new PortfolioStockItem(
                                e.getKey(),
                                amount.spot(),
                                amount.leverage(),
                                amount.total(),
                                ratio(amount.total(), sectorValue),
                                ratio(amount.total(), totalAssetValue)
                        );
                    })
                    .toList();

            sectorItems.add(new PortfolioSectorItem(sector, sectorValue, ratio(sectorValue, totalAssetValue), stockItems));
        }

        return sectorItems;
    }

    private double ratio(long part, long total) {
        if (total == 0) return 0.0;
        return part * 100.0 / total;
    }

    /** 종목별 현물/레버리지 금액을 누적하기 위한 내부 가변 컨테이너 */
    private static class StockAmount {
        private long spot = 0L;
        private long leverage = 0L;

        void addSpot(long amount) { this.spot += amount; }
        void addLeverage(long amount) { this.leverage += amount; }

        long spot() { return spot; }
        long leverage() { return leverage; }
        long total() { return spot + leverage; }
    }
}
