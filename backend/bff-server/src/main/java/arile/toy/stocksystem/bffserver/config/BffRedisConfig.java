package arile.toy.stocksystem.bffserver.config;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponseMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerBidAskPriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerStockSummaryTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceTickMessage;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResponseMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class BffRedisConfig {

    @Bean
    RedisConnectionFactory redisConnectionFactory(
            @Value("${redis.host}") String redisHost,
            @Value("${redis.port}") int redisPort
    ) {
        var config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> streamRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public RedisTemplate<String, BffServerTradePriceTickMessage> bffServerTradePriceTickMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, BffServerTradePriceTickMessage>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(BffServerTradePriceTickMessage.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, BffServerBidAskPriceTickMessage> bffServerBidAskPriceTickMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, BffServerBidAskPriceTickMessage>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(BffServerBidAskPriceTickMessage.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, BffServerStockSummaryTickMessage> bffServerStockSummaryTickMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ) {
        var template = new RedisTemplate<String, BffServerStockSummaryTickMessage>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new JacksonJsonRedisSerializer<>(BffServerStockSummaryTickMessage.class));
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, OrderResponseMessage> orderResponseMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ) {
        var template = new RedisTemplate<String, OrderResponseMessage>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new JacksonJsonRedisSerializer<>(OrderResponseMessage.class));
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, AutoOrderResponseMessage> autoOrderResponseMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ) {
        var template = new RedisTemplate<String, AutoOrderResponseMessage>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new JacksonJsonRedisSerializer<>(AutoOrderResponseMessage.class));
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, TrailingStopResponseMessage> trailingStopResponseMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ) {
        var template = new RedisTemplate<String, TrailingStopResponseMessage>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new JacksonJsonRedisSerializer<>(TrailingStopResponseMessage.class));
        template.afterPropertiesSet();
        return template;
    }
}