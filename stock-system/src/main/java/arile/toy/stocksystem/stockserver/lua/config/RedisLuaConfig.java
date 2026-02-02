package arile.toy.stocksystem.stockserver.lua.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisLuaConfig {

    @Bean
    public DefaultRedisScript<Long> reserveCashScript() {
        return new DefaultRedisScript<>("""
            local available = tonumber(redis.call('HGET', KEYS[1], 'availableCash') or '0')
            local amount = tonumber(ARGV[1])

            if available < amount then
                return 0
            end

            redis.call('HINCRBY', KEYS[1], 'availableCash', -amount)
            redis.call('HINCRBY', KEYS[1], 'reservedCash', amount)
            return 1
        """, Long.class);
    }

    @Bean
    public DefaultRedisScript<Long> refundCashScript() {
        return new DefaultRedisScript<>("""
            local reserved = tonumber(redis.call('HGET', KEYS[1], 'reservedCash') or '0')
            local refund = tonumber(ARGV[1])

            if reserved < refund then
                return 0
            end

            redis.call('HINCRBY', KEYS[1], 'reservedCash', -refund)
            redis.call('HINCRBY', KEYS[1], 'availableCash', refund)
            return 1
        """, Long.class);
    }
}
