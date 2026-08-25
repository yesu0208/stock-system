package arile.toy.stocksystem.bffserver.stockinfo.service;

import arile.toy.stocksystem.bffserver.stockinfo.client.NaverStockCrawlerClient;
import arile.toy.stocksystem.bffserver.stockinfo.dto.StockInfo;
import arile.toy.stocksystem.bffserver.stockinfo.dto.TradePageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockInfoService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final String KEY_PREFIX = "stock:info:";

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
}