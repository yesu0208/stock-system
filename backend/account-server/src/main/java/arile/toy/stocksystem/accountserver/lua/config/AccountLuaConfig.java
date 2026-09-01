package arile.toy.stocksystem.accountserver.lua.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class AccountLuaConfig {

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
    public DefaultRedisScript<Long> reserveStockScript() {
        return new DefaultRedisScript<>(
                """
                local stockCode = ARGV[2]
                local qtyToReserve = tonumber(ARGV[1])
        
                local stocksJson = redis.call('HGET', KEYS[1], 'stocks')
                if not stocksJson or stocksJson == '' then
                    return 0
                end
        
                local stocks = cjson.decode(stocksJson)
                local stock = stocks[stockCode]
        
                if not stock then
                    return 0
                end
        
                local available = tonumber(stock.availableQuantity or stock.quantity or 0)
        
                if available < qtyToReserve then
                    return 0
                end
        
                stock.availableQuantity = available - qtyToReserve
        
                stocks[stockCode] = stock
                redis.call('HSET', KEYS[1], 'stocks', cjson.encode(stocks))
                return 1
                """,
                Long.class
        );
    }

    @Bean
    public DefaultRedisScript<Long> refundStockScript() {
        return new DefaultRedisScript<>(
                """
                local stockCode = ARGV[2]
                local qtyToRefund = tonumber(ARGV[1])
        
                local stocksJson = redis.call('HGET', KEYS[1], 'stocks')
                if not stocksJson or stocksJson == '' then
                    return 0
                end
        
                local stocks = cjson.decode(stocksJson)
                local stock = stocks[stockCode]
        
                if not stock then
                    return 0
                end
        
                local available = tonumber(stock.availableQuantity or stock.quantity or 0)
        
                stock.availableQuantity = available + qtyToRefund
        
                stocks[stockCode] = stock
                redis.call('HSET', KEYS[1], 'stocks', cjson.encode(stocks))
                return 1
                """,
                Long.class
        );
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
        
                stocks[ARGV[4]] = {
                    quantity = tonumber(ARGV[1]),
                    totalAmount = tonumber(ARGV[2]),
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
                            totalAmount = tonumber(ARGV[2]),
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

    @Bean
    public DefaultRedisScript<Long> settleLeverageBuyScript() {
        return new DefaultRedisScript<>("""
            local reservedDecrease = tonumber(ARGV[1])
            local availableIncrease = tonumber(ARGV[2])

            local reserved = tonumber(redis.call('HGET', KEYS[1], 'reservedCash') or '0')
            if reserved < reservedDecrease then
                return 0
            end

            redis.call('HINCRBY', KEYS[1], 'reservedCash', -reservedDecrease)
            redis.call('HINCRBY', KEYS[1], 'availableCash', availableIncrease)
            return 1
        """, Long.class);
    }

    @Bean
    public DefaultRedisScript<Long> creditAvailableCashScript() {
        return new DefaultRedisScript<>("""
            local amount = tonumber(ARGV[1])
            redis.call('HINCRBY', KEYS[1], 'availableCash', amount)
            return 1
        """, Long.class);
    }
}
