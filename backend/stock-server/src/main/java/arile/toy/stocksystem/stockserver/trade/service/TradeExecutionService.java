package arile.toy.stocksystem.stockserver.trade.service;

import arile.toy.stocksystem.stockserver.order.dto.OrderDto;
import arile.toy.stocksystem.stockserver.order.dto.OrderStatus;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.repository.OrderRepository;
import arile.toy.stocksystem.stockserver.trade.dto.TradeResult;
import arile.toy.stocksystem.stockserver.trade.dto.TradeType;
import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;
import arile.toy.stocksystem.stockserver.trade.repository.TradeRepository;
import arile.toy.stocksystem.stockserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.stockserver.useraccount.repository.UserAccountRepository;
import arile.toy.stocksystem.stockserver.userstock.entity.UserStockEntity;
import arile.toy.stocksystem.stockserver.userstock.repository.UserStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeExecutionService {

    private final UserAccountRepository userAccountRepository;
    private final UserStockRepository userStockRepository;
    private final TradeRepository tradeRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public TradeResult executeBuyTrade(OrderDto buyOrderDto, int tradePrice, int executable) {

        OrderEntity orderEntity = orderRepository.findByIdForUpdate(buyOrderDto.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));

        if (orderEntity.getOrderStatus() != OrderStatus.OPEN &&
        orderEntity.getOrderStatus() != OrderStatus.PARTIAL) {
            return null;
        }

        long tradeAmount = (long) tradePrice * executable;

        UserAccountEntity account = userAccountRepository
                .findByUsernameForUpdate(buyOrderDto.username())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (account.getBalance() < tradeAmount) {
            throw new IllegalStateException(
                    "DB/Redis balance inconsistency detected during trade execution."
            );
        }

        account.setBalance(account.getBalance() - tradeAmount);
        userAccountRepository.save(account);

        UserStockEntity userStock = userStockRepository
                .findByUsernameAndStockCode(buyOrderDto.username(), buyOrderDto.stockCode())
                .orElseGet(() ->
                        UserStockEntity.of(
                                buyOrderDto.username(),
                                buyOrderDto.stockCode(),
                                0L,
                                0
                        )
                );

        int prevQuantity = userStock.getQuantity();
        userStock.setQuantity(prevQuantity + executable);

        long prevAmount = userStock.getAmount();
        userStock.setAmount(prevAmount + tradeAmount);

        userStockRepository.save(userStock);

        TradeEntity tradeEntity = tradeRepository.save(
                TradeEntity.of(buyOrderDto.orderId(), buyOrderDto.username(),
                        buyOrderDto.stockCode(), TradeType.BUY, tradePrice, executable)
        );

        int remainingQuantity = buyOrderDto.remainingQuantity() - executable;
        orderEntity.setOrderStatus(remainingQuantity > 0 ? OrderStatus.PARTIAL : OrderStatus.FILLED);
        orderEntity.setRemainingQuantity(remainingQuantity);
        orderRepository.save(orderEntity);

        return TradeResult.of(tradeEntity, prevAmount + tradeAmount, prevQuantity + executable);
    }

    @Transactional
    public TradeResult executeSellTrade(OrderDto sellOrderDto, int tradePrice, int executable) {

        OrderEntity orderEntity = orderRepository.findByIdForUpdate(sellOrderDto.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));

        if (orderEntity.getOrderStatus() != OrderStatus.OPEN &&
                orderEntity.getOrderStatus() != OrderStatus.PARTIAL) {
            return null;
        }

        long tradeAmount = (long) tradePrice * executable;

        UserAccountEntity account = userAccountRepository
                .findByUsernameForUpdate(sellOrderDto.username())
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));

        account.setBalance(account.getBalance() + tradeAmount);
        userAccountRepository.save(account);

        UserStockEntity userStock = userStockRepository
                .findByUsernameAndStockCode(sellOrderDto.username(), sellOrderDto.stockCode())
                .orElseThrow(() ->
                        new RuntimeException("No stocks owned.")
                );

        if (userStock.getQuantity() < sellOrderDto.remainingQuantity()) {
            throw new IllegalStateException(
                    "DB/Redis stock quantity inconsistency detected during trade execution."
            );
        }

        int prevQuantity = userStock.getQuantity();
        long prevAmount = userStock.getAmount();
        long avgPrice = prevAmount / prevQuantity;

        userStock.setQuantity(prevQuantity - executable);
        if (prevQuantity - executable == 0) {
            userStockRepository.delete(userStock);
        } else {
            userStock.setAmount(userStock.getAmount() - avgPrice * executable);
            userStockRepository.save(userStock);
        }

        TradeEntity tradeEntity = tradeRepository.save(
                TradeEntity.of(sellOrderDto.orderId(), sellOrderDto.username(),
                        sellOrderDto.stockCode(), TradeType.SELL, tradePrice, executable)
        );

        int remainingQuantity = sellOrderDto.remainingQuantity() - executable;
        orderEntity.setOrderStatus(remainingQuantity > 0 ? OrderStatus.PARTIAL : OrderStatus.FILLED);
        orderEntity.setRemainingQuantity(remainingQuantity);
        orderRepository.save(orderEntity);

        return TradeResult.of(tradeEntity, prevAmount - tradeAmount, prevQuantity - executable);
    }
}
