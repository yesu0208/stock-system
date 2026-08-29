package arile.toy.stocksystem.stockserver.lua.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisLuaConfig {

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
        
                stocks[ARGV[4]] = {
                    quantity = tonumber(ARGV[1]),
                    buyPrice = tonumber(ARGV[2]),
                    availableQuantity = tonumber(ARGV[1])
                }
        
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
        
                local newQty = tonumber(ARGV[1])
                local stockCode = ARGV[4]
        
                if stocks[stockCode] then
                
                    local oldStock = stocks[stockCode]
       
                    if newQty > 0 then
                        stocks[stockCode] = {
                            quantity = newQty,
                            buyPrice = tonumber(ARGV[2]),
                            availableQuantity = oldStock.availableQuantity
                        }
                    else
                        stocks[stockCode] = nil
                    end
                end
        
                redis.call('HSET', KEYS[1], 'stocks', cjson.encode(stocks))
                return 1
                """,
                Long.class
        );
    }
}
