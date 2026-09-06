package arile.toy.stocksystem.stockserver.config;

import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelResponseEvent;
import arile.toy.stocksystem.stockserver.autoorder.dto.StockServerAutoOrderResponseMessage;
import arile.toy.stocksystem.stockserver.autoorder.event.AutoOrderResponseEvent;
import arile.toy.stocksystem.stockserver.cancel.event.CancelResponseEvent;
import arile.toy.stocksystem.stockserver.chart.event.DailyCandleUpdateEvent;
import arile.toy.stocksystem.stockserver.chart.event.MinuteCandleUpdateEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.BidAskPriceTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.StockSummaryTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.TradePriceTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.message.BidAskPriceTickMessage;
import arile.toy.stocksystem.stockserver.external.stock.message.StockSummaryTickMessage;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.order.dto.StockServerOrderResponseMessage;
import arile.toy.stocksystem.stockserver.order.event.OrderResponseEvent;
import arile.toy.stocksystem.stockserver.otoco.dto.StockServerOtocoResponseMessage;
import arile.toy.stocksystem.stockserver.otoco.event.OtocoResponseEvent;
import arile.toy.stocksystem.stockserver.otococancel.event.OtocoCancelResponseEvent;
import arile.toy.stocksystem.stockserver.trade.event.TradeResponseEvent;
import arile.toy.stocksystem.stockserver.trailingstop.dto.StockServerTrailingStopResponseMessage;
import arile.toy.stocksystem.stockserver.trailingstop.event.TrailingStopResponseEvent;
import arile.toy.stocksystem.stockserver.trailingstopcancel.event.TrailingStopCancelResponseEvent;
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

    @Bean
    public RedisTemplate<String, DailyCandleUpdateEvent> dailyCandleUpdateEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, DailyCandleUpdateEvent>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(DailyCandleUpdateEvent.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, MinuteCandleUpdateEvent> minuteCandleUpdateEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, MinuteCandleUpdateEvent>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(MinuteCandleUpdateEvent.class));
        return template;
    }
    
    @Bean
    public RedisTemplate<String, TrailingStopResponseEvent> trailingStopResponseEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, TrailingStopResponseEvent>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(TrailingStopResponseEvent.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, TrailingStopCancelResponseEvent> trailingStopCancelResponseEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, TrailingStopCancelResponseEvent>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(TrailingStopCancelResponseEvent.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, StockServerTrailingStopResponseMessage> stockServerTrailingStopResponseMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ) {
        var template = new RedisTemplate<String, StockServerTrailingStopResponseMessage>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new JacksonJsonRedisSerializer<>(StockServerTrailingStopResponseMessage.class));
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, OtocoResponseEvent> otocoResponseEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, OtocoResponseEvent>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(OtocoResponseEvent.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, OtocoCancelResponseEvent> otocoCancelResponseEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        var template = new RedisTemplate<String, OtocoCancelResponseEvent>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(OtocoCancelResponseEvent.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, StockServerOtocoResponseMessage> stockServerOtocoResponseMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ) {
        var template = new RedisTemplate<String, StockServerOtocoResponseMessage>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new JacksonJsonRedisSerializer<>(StockServerOtocoResponseMessage.class));
        template.afterPropertiesSet();
        return template;
    }
}
