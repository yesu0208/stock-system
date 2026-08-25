package arile.toy.stocksystem.stockserver.config;

import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelResponseEvent;
import arile.toy.stocksystem.stockserver.autoorder.dto.StockServerAutoOrderResponseMessage;
import arile.toy.stocksystem.stockserver.autoorder.event.AutoOrderResponseEvent;
import arile.toy.stocksystem.stockserver.cancel.event.CancelResponseEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.BidAskPriceTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.StockSummaryTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.TradePriceTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.message.BidAskPriceTickMessage;
import arile.toy.stocksystem.stockserver.external.stock.message.StockSummaryTickMessage;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.order.dto.StockServerOrderResponseMessage;
import arile.toy.stocksystem.stockserver.order.event.OrderResponseEvent;
import arile.toy.stocksystem.stockserver.trade.event.TradeResponseEvent;
import arile.toy.stocksystem.stockserver.useraccount.dto.StockServerAccountMessage;
import arile.toy.stocksystem.stockserver.useraccount.event.AccountUpdateEvent;
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
public class StockRedisConfig {

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
    public RedisTemplate<String, StockSummaryTickEvent> stockSummaryTickEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, StockSummaryTickEvent>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(StockSummaryTickEvent.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, TradePriceTickEvent> tradePriceTickEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, TradePriceTickEvent>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(TradePriceTickEvent.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, BidAskPriceTickEvent> bidAskPriceTickEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, BidAskPriceTickEvent>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(BidAskPriceTickEvent.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, TradePriceTickMessage> tradePriceTickMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, TradePriceTickMessage>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(TradePriceTickMessage.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, BidAskPriceTickMessage> bidAskPriceTickMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, BidAskPriceTickMessage>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(BidAskPriceTickMessage.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, StockSummaryTickMessage> stockSummaryTickMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ) {
        var template = new RedisTemplate<String, StockSummaryTickMessage>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new JacksonJsonRedisSerializer<>(StockSummaryTickMessage.class));
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, OrderResponseEvent> orderResponseEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, OrderResponseEvent>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(OrderResponseEvent.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, StockServerAccountMessage> stockServerAccountMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ) {
        var template = new RedisTemplate<String, StockServerAccountMessage>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new JacksonJsonRedisSerializer<>(StockServerAccountMessage.class));
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, AccountUpdateEvent> accountUpdateEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, AccountUpdateEvent>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(AccountUpdateEvent.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, AutoCancelResponseEvent> autoCancelResponseEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, AutoCancelResponseEvent>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(AutoCancelResponseEvent.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, AutoOrderResponseEvent> autoOrderResponseEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ) {
        var template = new RedisTemplate<String, AutoOrderResponseEvent>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(AutoOrderResponseEvent.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, CancelResponseEvent> cancelResponseEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, CancelResponseEvent>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(CancelResponseEvent.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, TradeResponseEvent> tradeResponseEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, TradeResponseEvent>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(TradeResponseEvent.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, StockServerOrderResponseMessage> stockServerOrderResponseMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ) {
        var template = new RedisTemplate<String, StockServerOrderResponseMessage>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new JacksonJsonRedisSerializer<>(StockServerOrderResponseMessage.class));
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, StockServerAutoOrderResponseMessage> stockServerAutoOrderResponseMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ) {
        var template = new RedisTemplate<String, StockServerAutoOrderResponseMessage>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new JacksonJsonRedisSerializer<>(StockServerAutoOrderResponseMessage.class));
        template.afterPropertiesSet();
        return template;
    }
}