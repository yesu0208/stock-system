package arile.toy.stocksystem.bffserver.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BffRedisConfigTest {

    private BffRedisConfig config;
    private RedisConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        config = new BffRedisConfig();
        connectionFactory = mock(RedisConnectionFactory.class);
    }

    /** 키는 문자열, 값(value)은 JSON — Pub/Sub 발행용 */
    private void assertValueJsonTemplate(RedisTemplate<String, ?> template) {
        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(JacksonJsonRedisSerializer.class);
    }

    /** 키·해시 키는 문자열, 해시 값은 JSON — Hash 저장용 */
    private void assertHashJsonTemplate(RedisTemplate<String, ?> template) {
        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashValueSerializer()).isInstanceOf(JacksonJsonRedisSerializer.class);
    }

    @Test
    @DisplayName("host/port로 Lettuce standalone 커넥션 팩토리를 생성한다")
    void redisConnectionFactory() {
        RedisConnectionFactory factory = config.redisConnectionFactory("redis-host", 6380);

        assertThat(factory).isInstanceOf(LettuceConnectionFactory.class);
        LettuceConnectionFactory lettuce = (LettuceConnectionFactory) factory;
        assertThat(lettuce.getStandaloneConfiguration().getHostName()).isEqualTo("redis-host");
        assertThat(lettuce.getStandaloneConfiguration().getPort()).isEqualTo(6380);
    }

    @Test
    @DisplayName("streamRedisTemplate은 key/hashKey/hashValue 모두 String 직렬화를 사용한다")
    void streamRedisTemplate() {
        RedisTemplate<String, Object> template = config.streamRedisTemplate(connectionFactory);

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashValueSerializer()).isInstanceOf(StringRedisSerializer.class);
    }

    @Test
    @DisplayName("체결가·호가 틱 템플릿은 값을 JSON으로 직렬화한다")
    void tickTemplates() {
        assertValueJsonTemplate(config.bffServerTradePriceTickMessageRedisTemplate(connectionFactory));
        assertValueJsonTemplate(config.bffServerBidAskPriceTickMessageRedisTemplate(connectionFactory));
    }

    @Test
    @DisplayName("종목 요약·주문 계열 응답 템플릿은 해시 값을 JSON으로 직렬화한다")
    void hashTemplates() {
        assertHashJsonTemplate(config.bffServerStockSummaryTickMessageRedisTemplate(connectionFactory));
        assertHashJsonTemplate(config.orderResponseMessageRedisTemplate(connectionFactory));
        assertHashJsonTemplate(config.autoOrderResponseMessageRedisTemplate(connectionFactory));
        assertHashJsonTemplate(config.trailingStopResponseMessageRedisTemplate(connectionFactory));
        assertHashJsonTemplate(config.otocoResponseMessageRedisTemplate(connectionFactory));
        assertHashJsonTemplate(config.alertResponseMessageRedisTemplate(connectionFactory));
    }
}
