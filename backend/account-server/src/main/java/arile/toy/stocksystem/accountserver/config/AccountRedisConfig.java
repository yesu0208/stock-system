package arile.toy.stocksystem.accountserver.config;

import arile.toy.stocksystem.accountserver.useraccount.dto.UserAccountMessage;
import arile.toy.stocksystem.accountserver.useraccount.event.AccountUpdateEvent;
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
public class AccountRedisConfig {

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
    public RedisTemplate<String, UserAccountMessage> userAccountMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ) {
        var template = new RedisTemplate<String, UserAccountMessage>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new JacksonJsonRedisSerializer<>(UserAccountMessage.class));
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
}
