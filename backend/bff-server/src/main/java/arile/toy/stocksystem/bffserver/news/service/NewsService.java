package arile.toy.stocksystem.bffserver.news.service;

import arile.toy.stocksystem.bffserver.news.client.NaverNewsClient;
import arile.toy.stocksystem.bffserver.news.dto.NaverNewsItem;
import arile.toy.stocksystem.bffserver.news.dto.NaverNewsResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private static final String KEY_PREFIX = "news:";

    private final NaverNewsClient naverNewsClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public List<NaverNewsItem> searchNews(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        String cacheKey = buildKey(keyword);

        String cached = readCache(cacheKey);
        if (cached != null) {
            List<NaverNewsItem> cachedItems = deserialize(cached);
            if (cachedItems != null) {
                return cachedItems;
            }
        }

        List<NaverNewsItem> items = fetchAndClean(keyword);

        cacheItems(cacheKey, items);

        return items;
    }

    /** 캐시 조회 실패(Redis 장애 등)는 캐시 미스로 보고 API 조회로 대체 */
    private String readCache(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("News 캐시 조회 실패. API 조회로 대체. key={}", cacheKey, e);
            return null;
        }
    }

    private List<NaverNewsItem> fetchAndClean(String keyword) {
        NaverNewsResponse response = naverNewsClient.search(keyword);

        if (response == null || response.items() == null) {
            return List.of();
        }

        return response.items().stream()
                .map(item -> new NaverNewsItem(
                        cleanText(item.title()),
                        item.originallink(),
                        item.link(),
                        cleanText(item.description()),
                        formatPubDate(item.pubDate())
                ))
                .toList();
    }

    private void cacheItems(String cacheKey, List<NaverNewsItem> items) {
        try {
            String json = objectMapper.writeValueAsString(items);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("News 캐시 저장 실패. key={}", cacheKey, e);
        }
    }

    private List<NaverNewsItem> deserialize(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<NaverNewsItem>>() {});
        } catch (Exception e) {
            log.warn("News 캐시 역직렬화 실패. API 재조회로 대체.", e);
            return null;
        }
    }

    private String buildKey(String keyword) {
        return KEY_PREFIX + keyword.trim().toLowerCase();
    }

    private String cleanText(String text) {
        if (text == null) return null;
        String unescaped = StringEscapeUtils.unescapeHtml4(text);
        return unescaped.replaceAll("<[^>]*>", "");
    }

    private String formatPubDate(String pubDate) {
        if (pubDate == null) return null;

        try {
            ZonedDateTime dateTime = ZonedDateTime.parse(pubDate,
                    DateTimeFormatter.RFC_1123_DATE_TIME);

            return dateTime.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
        } catch (DateTimeParseException e) {
            log.warn("뉴스 발행일 형식 변환 실패. pubDate={}", pubDate);
            return pubDate;
        }
    }
}