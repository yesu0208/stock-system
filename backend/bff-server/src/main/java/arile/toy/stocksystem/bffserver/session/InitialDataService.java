package arile.toy.stocksystem.bffserver.session;

import arile.toy.stocksystem.bffserver.account.dto.AccountResponse;
import arile.toy.stocksystem.bffserver.account.service.AccountCalculator;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponseMessage;
import arile.toy.stocksystem.bffserver.autoorder.repository.BffServerAutoOrderResponseRepository;
import arile.toy.stocksystem.bffserver.exception.server.RedisAccountNotFoundException;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerBidAskPriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerBidAskPriceRepository;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerTradePriceRepository;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import arile.toy.stocksystem.bffserver.order.repository.BffServerOrderResponseRepository;
import arile.toy.stocksystem.bffserver.stockinfo.dto.StockDetailTickMessage;
import arile.toy.stocksystem.bffserver.stockinfo.repository.StockDetailSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InitialDataService {

    private final AccountCalculator accountCalculator;
    private final BffServerOrderResponseRepository bffServerOrderResponseRepository;
    private final BffServerAutoOrderResponseRepository bffServerAutoOrderResponseRepository;
    private final BffServerTradePriceRepository bffServerTradePriceRepository;
    private final BffServerBidAskPriceRepository bffServerBidAskPriceRepository;
    private final StockDetailSnapshotRepository stockDetailSnapshotRepository;

    public Optional<AccountResponse> getAccountData(String username) {

        try {
            var accountResponse = accountCalculator.calculate(username);
            return Optional.ofNullable(accountResponse);

        } catch (RedisAccountNotFoundException e) {
            log.debug("No account data found for username={}", username);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Unexpected error while getting account data for username={}", username, e);
            return Optional.empty();
        }
    }


    public Optional<List<OrderResponseMessage>> getOrderData(String username) {

        try {
            List<OrderResponseMessage> responses =
                    bffServerOrderResponseRepository.findAll(username);

            return Optional.ofNullable(responses);

        } catch (Exception e) {
            log.error("Unexpected error while getting order data for username={}", username, e);
            return Optional.empty();
        }
    }


    public Optional<List<AutoOrderResponseMessage>> getAutoOrderData(String username) {

        try {
            List<AutoOrderResponseMessage> responses =
                    bffServerAutoOrderResponseRepository.findAll(username);

            return Optional.ofNullable(responses);

        } catch (Exception e) {
            log.error("Unexpected error while getting auto order data for username={}", username, e);
            return Optional.empty();
        }
    }


    public Optional<BffServerTradePriceTickMessage> getTradePriceData(String stockCode) {

        try {
            var message = bffServerTradePriceRepository.findByStockCode(stockCode);
            return Optional.ofNullable(message);

        } catch (Exception e) {
            log.error("Unexpected error while getting trade price data for stockCode={}", stockCode, e);
            return Optional.empty();
        }
    }


    public Optional<BffServerBidAskPriceTickMessage> getBidAskPriceData(String stockCode) {

        try {
            var message = bffServerBidAskPriceRepository.findByStockCode(stockCode);
            return Optional.ofNullable(message);

        } catch (Exception e) {
            log.error("Unexpected error while getting bid/ask data for stockCode={}", stockCode, e);
            return Optional.empty();
        }
    }

    public Optional<StockDetailTickMessage> getStockDetailData(String stockCode) {

        try {
            var message = stockDetailSnapshotRepository.getLatest(stockCode);
            return Optional.ofNullable(message);

        } catch (Exception e) {
            log.error("Unexpected error while getting stock detail data for stockCode={}", stockCode, e);
            return Optional.empty();
        }
    }
}
