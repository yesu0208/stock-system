package arile.toy.stocksystem.accountserver.config;

import arile.toy.stocksystem.accountserver.leverage.event.LiquidationExecutedEvent;
import arile.toy.stocksystem.accountserver.leverage.event.MarginCallEvent;
import arile.toy.stocksystem.accountserver.stockprice.dto.StockSummaryTickMessage;
import arile.toy.stocksystem.accountserver.useraccount.event.AccountUpdateEvent;
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

class AccountRedisConfigTest {

    private AccountRedisConfig config;
    private RedisConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        config = new AccountRedisConfig();
        connectionFactory = mock(RedisConnectionFactory.class);
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
    @DisplayName("accountUpdateEventRedisTemplate은 value를 JSON으로 직렬화한다")
    void accountUpdateEventRedisTemplate() {
        RedisTemplate<String, AccountUpdateEvent> template =
                config.accountUpdateEventRedisTemplate(connectionFactory);

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(JacksonJsonRedisSerializer.class);
    }

    @Test
    @DisplayName("stockSummaryTickMessageRedisTemplate은 hash value를 JSON으로 직렬화하고 초기화된다")
    void stockSummaryTickMessageRedisTemplate() {
        RedisTemplate<String, StockSummaryTickMessage> template =
                config.stockSummaryTickMessageRedisTemplate(connectionFactory);

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashValueSerializer()).isInstanceOf(JacksonJsonRedisSerializer.class);
    }

    @Test
    @DisplayName("marginCallEventRedisTemplate은 value를 JSON으로 직렬화한다")
    void marginCallEventRedisTemplate() {
        RedisTemplate<String, MarginCallEvent> template =
                config.marginCallEventRedisTemplate(connectionFactory);

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(JacksonJsonRedisSerializer.class);
    }

    @Test
    @DisplayName("liquidationExecutedEventRedisTemplate은 value를 JSON으로 직렬화한다")
    void liquidationExecutedEventRedisTemplate() {
        RedisTemplate<String, LiquidationExecutedEvent> template =
                config.liquidationExecutedEventRedisTemplate(connectionFactory);

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(JacksonJsonRedisSerializer.class);
    }
}
