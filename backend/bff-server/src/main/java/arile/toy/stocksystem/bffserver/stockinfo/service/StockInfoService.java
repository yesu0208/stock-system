package arile.toy.stocksystem.bffserver.stockinfo.service;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import arile.toy.stocksystem.bffserver.stockinfo.client.NaverStockCrawlerClient;
import arile.toy.stocksystem.bffserver.stockinfo.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockInfoService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final String KEY_PREFIX = "stock:info:";
    private static final String FOREIGN_TRADE_KEY_PREFIX = "stock:foreign:";
    private static final String POPULAR_KEY = "stock:popular";
    private static final String ALL_UPJONG_KEY = "stock:upjong:all";
    private static final String UPJONG_STOCK_KEY_PREFIX = "stock:upjong:stocks:";
    private static final String DETAIL_EXTRA_KEY_PREFIX = "stock:detail:extra:";
    private static final String INVESTOR_TREND_KEY_PREFIX = "stock:investor:trend:";
    private static final String DEAL_RANK_KEY_PREFIX = "stock:deal-rank:";

    // 네이버 장애는 서버 버그가 아니므로 503으로 응답 (ClientErrorException은 Slack 에러 알림 대상이 아님)
    private static final String UNAVAILABLE_MESSAGE = "시세 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";

    private final NaverStockCrawlerClient naverStockCrawlerClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public StockInfo getStockInfo(String code) {
        return cached(KEY_PREFIX + code.trim(), new TypeReference<StockInfo>() {},
                () -> naverStockCrawlerClient.getStockInfo(code));
    }

    public TradePageResponse getForeignInstitutionTrades(String code, int page) {
        validatePage(page);
        return cached(FOREIGN_TRADE_KEY_PREFIX + code.trim() + ":" + page, new TypeReference<TradePageResponse>() {},
                () -> naverStockCrawlerClient.getForeignInstitutionTrades(code, page));
    }

    public List<PopularStock> getPopularStocks() {
        return cached(POPULAR_KEY, new TypeReference<List<PopularStock>>() {},
                naverStockCrawlerClient::getPopularStocks);
    }

    public UpjongResponse getAllUpjongs() {
        return cached(ALL_UPJONG_KEY, new TypeReference<UpjongResponse>() {},
                naverStockCrawlerClient::getAllUpjongs);
    }

    public UpjongStockResponse getUpjongStocks(String upjongNo) {
        return cached(UPJONG_STOCK_KEY_PREFIX + upjongNo.trim(), new TypeReference<UpjongStockResponse>() {},
                () -> new UpjongStockResponse(
                        resolveUpjongName(upjongNo),
                        naverStockCrawlerClient.getUpjongStocks(upjongNo)));
    }

    public StockDetailExtraResponse getStockDetailExtra(String code) {
        return cached(DETAIL_EXTRA_KEY_PREFIX + code.trim(), new TypeReference<StockDetailExtraResponse>() {},
                () -> naverStockCrawlerClient.getStockDetailExtra(code));
    }

    public TrendResponse getInvestorTrend(MarketType market, TrendType type, int page) {
        validatePage(page);
        return cached(INVESTOR_TREND_KEY_PREFIX + market.name() + ":" + type.name() + ":" + page,
                new TypeReference<TrendResponse>() {},
                () -> naverStockCrawlerClient.getInvestorTrend(market, type, page));
    }

    public DealRankResponse getDealRank(DealRankMarket market, InvestorType investorType, DealType dealType,
                                        PeriodType periodType) {
        String cacheKey = DEAL_RANK_KEY_PREFIX + market.name() + ":" + investorType.name() + ":"
                + dealType.name() + ":" + periodType.name();
        return cached(cacheKey, new TypeReference<DealRankResponse>() {},
                () -> naverStockCrawlerClient.getDealRank(market, investorType, dealType, periodType));
    }

    /** 업종 목록(캐시 포함)에서 업종명을 찾음. 실패해도 종목 목록은 보여줄 수 있도록 빈 문자열로 대체 */
    private String resolveUpjongName(String upjongNo) {
        try {
            return getAllUpjongs().items().stream()
                    .filter(item -> item.no().equals(upjongNo))
                    .map(UpjongInfo::name)
                    .findFirst()
                    .orElse("");
        } catch (Exception e) {
            log.warn("업종명 조회 실패. no={}", upjongNo, e);
            return "";
        }
    }

    /** 네이버 페이지 번호는 1부터 시작 (0 이하는 네이버에 음수 시작 위치로 전달되어 실패함) */
    private void validatePage(int page) {
        if (page < 1) {
            throw new ClientErrorException(HttpStatus.BAD_REQUEST, "page는 1 이상이어야 합니다.");
        }
    }

    /**
     * 캐시가 있으면 반환하고, 없으면 크롤링해 60초간 캐시.
     * 캐시 조회·저장 실패(Redis 장애, 깨진 값)는 캐시 미스로 보고 크롤링으로 대체하며,
     * 네이버 장애(크롤러의 IllegalStateException)는 503으로 응답.
     */
    private <T> T cached(String cacheKey, TypeReference<T> type, Supplier<T> loader) {
        T cachedValue = readCache(cacheKey, type);
        if (cachedValue != null) {
            return cachedValue;
        }

        T value = load(loader);
        writeCache(cacheKey, value);
        return value;
    }

    private <T> T readCache(String cacheKey, TypeReference<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            return json == null ? null : objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("시세 정보 캐시 조회 실패. 크롤링으로 대체. key={}", cacheKey, e);
            return null;
        }
    }

    private <T> T load(Supplier<T> loader) {
        try {
            return loader.get();
        } catch (IllegalStateException e) {
            throw new ClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE_MESSAGE);
        }
    }

    private void writeCache(String cacheKey, Object value) {
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(value), CACHE_TTL);
        } catch (Exception e) {
            log.warn("시세 정보 캐시 저장 실패. key={}", cacheKey, e);
        }
    }
}
