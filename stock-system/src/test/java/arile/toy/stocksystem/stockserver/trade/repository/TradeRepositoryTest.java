package arile.toy.stocksystem.stockserver.trade.repository;

import arile.toy.stocksystem.stockserver.trade.dto.TradeType;
import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TradeRepositoryTest {

    @Autowired
    private TradeRepository tradeRepository;

    @Test
    @DisplayName("거래(trade) 엔티티가 주어졌을 때, 저장하면 조회 가능")
    void givenTradeEntity_whenSaved_thenCanBeFound() {
        // given
        TradeEntity trade = new TradeEntity();
        trade.setStockCode("005930");
        trade.setOrderId(1L);
        trade.setUsername("user");
        trade.setTradeType(TradeType.BUY);
        trade.setTradePrice(50000);
        trade.setTradeQuantity(50);
        trade.setExecutedAt(Instant.now());

        // when
        TradeEntity saved = tradeRepository.save(trade);
        Optional<TradeEntity> found = tradeRepository.findById(saved.getTradeId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getStockCode()).isEqualTo("005930");
        assertThat(found.get().getOrderId()).isEqualTo(1L);
        assertThat(found.get().getUsername()).isEqualTo("user");
        assertThat(found.get().getTradeType()).isEqualTo(TradeType.BUY);
        assertThat(found.get().getTradePrice()).isEqualTo(50000);
        assertThat(found.get().getTradeQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("저장된 거래(trade)가 없을 때, findById는 빈(Optional)을 반환한다")
    void whenNoTradeSaved_findByIdReturnsEmpty() {
        Optional<TradeEntity> found = tradeRepository.findById(999L);
        assertThat(found).isEmpty();
    }
}
