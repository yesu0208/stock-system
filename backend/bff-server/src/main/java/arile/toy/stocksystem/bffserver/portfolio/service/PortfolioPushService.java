package arile.toy.stocksystem.bffserver.portfolio.service;

import arile.toy.stocksystem.bffserver.exception.server.RedisAccountNotFoundException;
import arile.toy.stocksystem.bffserver.portfolio.dto.PortfolioResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final PortfolioCalculator portfolioCalculator;

    public void push(String username) {
        try {
            PortfolioResponse response = portfolioCalculator.calculate(username);
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/sub/portfolio",
                    response
            );
        } catch (RedisAccountNotFoundException e) {
            log.debug("No account data found for username={}. Skip portfolio push.", username);
        } catch (Exception e) {
            log.error("Failed to push portfolio for username={}", username, e);
        }
    }
}
