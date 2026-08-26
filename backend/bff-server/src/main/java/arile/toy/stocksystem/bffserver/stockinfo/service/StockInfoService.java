package arile.toy.stocksystem.bffserver.stockinfo.service;

import arile.toy.stocksystem.bffserver.stockinfo.client.NaverStockCrawlerClient;
import arile.toy.stocksystem.bffserver.stockinfo.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockInfoService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final String KEY_PREFIX = "stock:info:";
    private static final String POPULAR_KEY = "stock:popular";
    private static final String ALL_UPJONG_KEY = "stock:upjong:all";
    private static final String UPJONG_STOCK_KEY_PREFIX = "stock:upjong:stocks:";
    private static final String INVESTOR_TREND_KEY_PREFIX = "stock:investor:trend:";
    private static final String DEAL_RANK_KEY_PREFIX = "stock:deal-rank:";

    private final NaverStockCrawlerClient naverStockCrawlerClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public StockInfo getStockInfo(String code) {
        String cacheKey = buildKey(code);

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            StockInfo cachedInfo = deserialize(cached);
            if (cachedInfo != null) {
                return cachedInfo;
            }
        }

        StockInfo stockInfo = naverStockCrawlerClient.getStockInfo(code);

        cacheStockInfo(cacheKey, stockInfo);

        return stockInfo;
    }

    private void cacheStockInfo(String cacheKey, StockInfo stockInfo) {
        try {
            String json = objectMapper.writeValueAsString(stockInfo);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("StockInfo 캐시 저장 실패. key={}", cacheKey, e);
        }
    }

    private StockInfo deserialize(String json) {
        try {
            return objectMapper.readValue(json, StockInfo.class);
        } catch (Exception e) {
            log.warn("StockInfo 캐시 역직렬화 실패. 크롤링으로 대체.", e);
            return null;
        }
    }

    private String buildKey(String code) {
        return KEY_PREFIX + code.trim();
    }


    public TradePageResponse getForeignInstitutionTrades(String code, int page) {
        String cacheKey = buildForeignTradeKey(code, page);

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            TradePageResponse cachedResponse = deserializeTradePage(cached);
            if (cachedResponse != null) {
                return cachedResponse;
            }
        }

        TradePageResponse response = naverStockCrawlerClient.getForeignInstitutionTrades(code, page);

        cacheTradePage(cacheKey, response);

        return response;
    }

    public List<PopularStock> getPopularStocks() {

        String cached = redisTemplate.opsForValue().get(POPULAR_KEY);
        if (cached != null) {
            List<PopularStock> cachedList = deserializePopularStocks(cached);
            if (cachedList != null) {
                return cachedList;
            }
        }

        List<PopularStock> popularStocks = naverStockCrawlerClient.getPopularStocks();

        cachePopularStocks(popularStocks);

        return popularStocks;
    }

    private void cachePopularStocks(List<PopularStock> popularStocks) {
        try {
            String json = objectMapper.writeValueAsString(popularStocks);
            redisTemplate.opsForValue().set(POPULAR_KEY, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("PopularStock 캐시 저장 실패", e);
        }
    }

    private List<PopularStock> deserializePopularStocks(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<PopularStock>>() {});
        } catch (Exception e) {
            log.warn("PopularStock 캐시 역직렬화 실패. 크롤링으로 대체.", e);
            return null;
        }
    }

    private void cacheTradePage(String cacheKey, TradePageResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("ForeignInstitutionTrade 캐시 저장 실패. key={}", cacheKey, e);
        }
    }

    private TradePageResponse deserializeTradePage(String json) {
        try {
            return objectMapper.readValue(json, TradePageResponse.class);
        } catch (Exception e) {
            log.warn("ForeignInstitutionTrade 캐시 역직렬화 실패. 크롤링으로 대체.", e);
            return null;
        }
    }

    private String buildForeignTradeKey(String code, int page) {
        return "stock:foreign:" + code.trim() + ":" + page;
    }

    public UpjongResponse getAllUpjongs() {

        String cached = redisTemplate.opsForValue().get(ALL_UPJONG_KEY);
        if (cached != null) {
            UpjongResponse cachedResponse = deserializeUpjongResponse(cached);
            if (cachedResponse != null) {
                return cachedResponse;
            }
        }

        UpjongResponse response = naverStockCrawlerClient.getAllUpjongs();

        cacheAllUpjongs(response);

        return response;
    }

    private void cacheAllUpjongs(UpjongResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(ALL_UPJONG_KEY, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("UpjongResponse 캐시 저장 실패", e);
        }
    }

    private UpjongResponse deserializeUpjongResponse(String json) {
        try {
            return objectMapper.readValue(json, UpjongResponse.class);
        } catch (Exception e) {
            log.warn("UpjongResponse 캐시 역직렬화 실패. 크롤링으로 대체.", e);
            return null;
        }
    }

    public UpjongStockResponse getUpjongStocks(String upjongNo) {
        String cacheKey = buildUpjongStockKey(upjongNo);

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            UpjongStockResponse cachedResponse = deserializeUpjongStockResponse(cached);
            if (cachedResponse != null) {
                return cachedResponse;
            }
        }

        UpjongStockResponse response = naverStockCrawlerClient.getUpjongStocks(upjongNo);

        cacheUpjongStocks(cacheKey, response);

        return response;
    }

    private void cacheUpjongStocks(String cacheKey, UpjongStockResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("UpjongStockResponse 캐시 저장 실패. key={}", cacheKey, e);
        }
    }

    private UpjongStockResponse deserializeUpjongStockResponse(String json) {
        try {
            return objectMapper.readValue(json, UpjongStockResponse.class);
        } catch (Exception e) {
            log.warn("UpjongStockResponse 캐시 역직렬화 실패. 크롤링으로 대체.", e);
            return null;
        }
    }

    private String buildUpjongStockKey(String upjongNo) {
        return UPJONG_STOCK_KEY_PREFIX + upjongNo.trim();
    }

    private static final String DETAIL_EXTRA_KEY_PREFIX = "stock:detail:extra:";

    public StockDetailExtraResponse getStockDetailExtra(String code) {
        String cacheKey = DETAIL_EXTRA_KEY_PREFIX + code.trim();

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            StockDetailExtraResponse cachedResponse = deserializeDetailExtra(cached);
            if (cachedResponse != null) {
                return cachedResponse;
            }
        }

        StockDetailExtraResponse response = naverStockCrawlerClient.getStockDetailExtra(code);
        cacheDetailExtra(cacheKey, response);

        return response;
    }

    private void cacheDetailExtra(String cacheKey, StockDetailExtraResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("StockDetailExtra 캐시 저장 실패. key={}", cacheKey, e);
        }
    }

    private StockDetailExtraResponse deserializeDetailExtra(String json) {
        try {
            return objectMapper.readValue(json, StockDetailExtraResponse.class);
        } catch (Exception e) {
            log.warn("StockDetailExtra 캐시 역직렬화 실패. 크롤링으로 대체.", e);
            return null;
        }
    }

    public TrendResponse getInvestorTrend(MarketType market, TrendType type, int page) {

        String cacheKey = buildInvestorTrendKey(market, type, page);

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            TrendResponse cachedResponse = deserializeTrendResponse(cached);
            if (cachedResponse != null) {
                return cachedResponse;
            }
        }

        List<InvestorTrendDto> current = naverStockCrawlerClient.getInvestorTrend(market, type, page);
        List<InvestorTrendDto> next = naverStockCrawlerClient.getInvestorTrend(market, type, page + 1);

        boolean hasNext = !next.isEmpty() && !isSamePage(current, next);

        TrendResponse response = new TrendResponse(current, hasNext);

        cacheInvestorTrend(cacheKey, response);

        return response;
    }

    private <T> boolean isSamePage(List<T> a, List<T> b) {

        if (a.size() != b.size()) {
            return false;
        }

        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) {
                return false;
            }
        }

        return true;
    }

    private void cacheInvestorTrend(String cacheKey, TrendResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("InvestorTrend 캐시 저장 실패. key={}", cacheKey, e);
        }
    }

    private TrendResponse deserializeTrendResponse(String json) {
        try {
            return objectMapper.readValue(json, TrendResponse.class);
        } catch (Exception e) {
            log.warn("InvestorTrend 캐시 역직렬화 실패. 크롤링으로 대체.", e);
            return null;
        }
    }

    private String buildInvestorTrendKey(MarketType market, TrendType type, int page) {
        return INVESTOR_TREND_KEY_PREFIX + market.name() + ":" + type.name() + ":" + page;
    }

    public DealRankResponse getDealRank(DealRankMarket market, InvestorType investorType, DealType dealType) {
        String cacheKey = buildDealRankKey(market, investorType, dealType);

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            DealRankResponse cachedResponse = deserializeDealRank(cached);
            if (cachedResponse != null) {
                return cachedResponse;
            }
        }

        DealRankResponse response = naverStockCrawlerClient.getDealRank(market, investorType, dealType);

        cacheDealRank(cacheKey, response);

        return response;
    }

    private void cacheDealRank(String cacheKey, DealRankResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("DealRank 캐시 저장 실패. key={}", cacheKey, e);
        }
    }

    private DealRankResponse deserializeDealRank(String json) {
        try {
            return objectMapper.readValue(json, DealRankResponse.class);
        } catch (Exception e) {
            log.warn("DealRank 캐시 역직렬화 실패. 크롤링으로 대체.", e);
            return null;
        }
    }

    private String buildDealRankKey(DealRankMarket market, InvestorType investorType, DealType dealType) {
        return DEAL_RANK_KEY_PREFIX + market.name() + ":" + investorType.name() + ":" + dealType.name();
    }
}