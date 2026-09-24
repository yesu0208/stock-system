package arile.toy.stocksystem.bffserver.news.service;

import arile.toy.stocksystem.bffserver.news.client.NaverNewsClient;
import arile.toy.stocksystem.bffserver.news.dto.NaverNewsItem;
import arile.toy.stocksystem.bffserver.news.dto.NaverNewsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class NewsServiceTest {

    private static final String KEY = "news:삼성전자";
    private static final Duration TTL = Duration.ofSeconds(30);

    private NaverNewsClient naverNewsClient;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private NewsService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        naverNewsClient = mock(NaverNewsClient.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        doReturn(valueOps).when(redisTemplate).opsForValue();
        service = new NewsService(naverNewsClient, redisTemplate, new ObjectMapper());
    }

    private static NaverNewsResponse response(NaverNewsItem... items) {
        return new NaverNewsResponse("x", items.length, 1, items.length, List.of(items));
    }

    private static NaverNewsItem raw(String title, String pubDate) {
        return new NaverNewsItem(title, "https://origin", "https://naver", "<b>설명</b> &amp; 요약", pubDate);
    }

    @Test
    @DisplayName("캐시가 없으면 API로 조회해 HTML 태그·엔티티를 정리하고 날짜를 변환한 뒤 30초간 캐시한다")
    void cacheMiss() {
        given(naverNewsClient.search("삼성전자")).willReturn(
                response(raw("<b>삼성전자</b> &quot;신고가&quot;", "Thu, 24 Sep 2026 09:05:00 +0900")));

        List<NaverNewsItem> items = service.searchNews("삼성전자");

        assertThat(items).containsExactly(new NaverNewsItem("삼성전자 \"신고가\"", "https://origin",
                "https://naver", "설명 & 요약", "2026.09.24 09:05"));
        verify(valueOps).set(eq(KEY), anyString(), eq(TTL));
    }

    @Test
    @DisplayName("캐시가 있으면 API를 호출하지 않는다")
    void cacheHit() {
        given(valueOps.get(KEY)).willReturn("""
                [{"title": "캐시된 뉴스", "originallink": "o", "link": "l", "description": "d", "pubDate": "2026.09.24 09:05"}]
                """);

        List<NaverNewsItem> items = service.searchNews("삼성전자");

        assertThat(items).extracting(NaverNewsItem::title).containsExactly("캐시된 뉴스");
        verifyNoInteractions(naverNewsClient);
    }

    @Test
    @DisplayName("검색어는 앞뒤 공백을 지우고 소문자로 바꿔 같은 캐시 키를 쓴다")
    void keyNormalized() {
        given(valueOps.get("news:samsung")).willReturn("[]");

        assertThat(service.searchNews("  SAMSUNG ")).isEmpty();
        verifyNoInteractions(naverNewsClient);
    }

    @ParameterizedTest(name = "keyword=[{0}]")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("검색어가 비어 있으면 캐시도 API도 조회하지 않고 빈 목록을 반환한다")
    void blankKeyword(String keyword) {
        assertThat(service.searchNews(keyword)).isEmpty();

        verifyNoInteractions(naverNewsClient, redisTemplate);
    }

    @Test
    @DisplayName("캐시가 깨져 있으면 API로 다시 조회한다")
    void brokenCache_refetches() {
        given(valueOps.get(KEY)).willReturn("{broken");
        given(naverNewsClient.search("삼성전자")).willReturn(response());

        assertThat(service.searchNews("삼성전자")).isEmpty();
        verify(naverNewsClient).search("삼성전자");
    }

    @Test
    @DisplayName("캐시 조회가 실패해도(Redis 장애) API로 조회해 결과를 반환한다")
    void cacheReadFails_fallsBackToApi() {
        given(valueOps.get(KEY)).willThrow(new RedisConnectionFailureException("down"));
        given(naverNewsClient.search("삼성전자")).willReturn(response(raw("뉴스", null)));

        assertThat(service.searchNews("삼성전자")).extracting(NaverNewsItem::title).containsExactly("뉴스");
    }

    @Test
    @DisplayName("캐시 저장이 실패해도 결과는 그대로 반환한다")
    void cacheWriteFails() {
        given(naverNewsClient.search("삼성전자")).willReturn(response(raw("뉴스", null)));
        willThrow(new RedisConnectionFailureException("down"))
                .given(valueOps).set(anyString(), anyString(), eq(TTL));

        assertThat(service.searchNews("삼성전자")).hasSize(1);
    }

    @Test
    @DisplayName("날짜 형식이 다른 기사는 원문 날짜를 그대로 두고, 나머지 기사는 정상 처리한다")
    void unparsableDate_keepsOriginal() {
        given(naverNewsClient.search("삼성전자")).willReturn(response(
                raw("정상", "Thu, 24 Sep 2026 09:05:00 +0900"),
                raw("이상한 날짜", "2026-09-24 09:05")));

        List<NaverNewsItem> items = service.searchNews("삼성전자");

        assertThat(items).extracting(NaverNewsItem::pubDate)
                .containsExactly("2026.09.24 09:05", "2026-09-24 09:05");
    }

    @Test
    @DisplayName("API 응답이 비어 있거나 기사 목록이 없으면 빈 목록을 반환한다")
    void emptyResponse() {
        given(naverNewsClient.search("삼성전자")).willReturn(null);
        given(naverNewsClient.search("카카오")).willReturn(new NaverNewsResponse("x", 0, 1, 0, null));

        assertThat(service.searchNews("삼성전자")).isEmpty();
        assertThat(service.searchNews("카카오")).isEmpty();
    }
}
