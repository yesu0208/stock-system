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

    @Bean
    public DefaultRedisScript<Long> buyTradeScript() {
        return new DefaultRedisScript<>(
                """
                local reserved = tonumber(redis.call('HGET', KEYS[1], 'reservedCash') or '0')
                if reserved < tonumber(ARGV[3]) then
                    return 0
                end
    
                redis.call('HINCRBY', KEYS[1], 'reservedCash', -tonumber(ARGV[3]))
    
                redis.call('HINCRBY', KEYS[1], 'availableCash', tonumber(ARGV[5]))
    
                local stocksJson = redis.call('HGET', KEYS[1], 'stocks')
                local stocks = {}
                if stocksJson and stocksJson ~= '' then
                    stocks = cjson.decode(stocksJson)
                end
    
                stocks[ARGV[4]] = {quantity=tonumber(ARGV[1]), buyPrice=tonumber(ARGV[2])}
    
                redis.call('HSET', KEYS[1], 'stocks', cjson.encode(stocks))
                return 1
                """,
                Long.class
        );
    }

    @Bean
    public DefaultRedisScript<Long> sellTradeScript() {
        return new DefaultRedisScript<>(
                """
                redis.call('HINCRBY', KEYS[1], 'availableCash', tonumber(ARGV[3]))
    
                redis.call('HINCRBY', KEYS[1], 'availableCash', tonumber(ARGV[5]))
    
                local stocksJson = redis.call('HGET', KEYS[1], 'stocks')
                local stocks = {}
                if stocksJson and stocksJson ~= '' then
                    stocks = cjson.decode(stocksJson)
                end
    
                local qty = tonumber(ARGV[1])
    
                if qty > 0 then
                    stocks[ARGV[4]] = {
                        quantity = qty,
                        buyPrice = tonumber(ARGV[2])
                    }
                else
                    stocks[ARGV[4]] = nil
                end
    
                redis.call('HSET', KEYS[1], 'stocks', cjson.encode(stocks))
                return 1
                """,
                Long.class
        );
    }
}
