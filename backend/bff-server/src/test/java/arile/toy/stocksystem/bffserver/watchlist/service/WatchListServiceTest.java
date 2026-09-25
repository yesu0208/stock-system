package arile.toy.stocksystem.bffserver.watchlist.service;

import arile.toy.stocksystem.bffserver.exception.watchlist.WatchListAlreadyExistsException;
import arile.toy.stocksystem.bffserver.exception.watchlist.WatchListNotFoundException;
import arile.toy.stocksystem.bffserver.watchlist.entity.WatchListEntity;
import arile.toy.stocksystem.bffserver.watchlist.repository.WatchListRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WatchListServiceTest {

    @Mock
    private WatchListRepository watchListRepository;

    @InjectMocks
    private WatchListService service;

    @Test
    @DisplayName("관심종목을 순서대로 조회한다")
    void getAll() {
        List<WatchListEntity> items = List.of(WatchListEntity.of("user1", "005930", "삼성전자", 0));
        given(watchListRepository.findByUsernameOrderBySortOrderAsc("user1")).willReturn(items);

        assertThat(service.getAll("user1")).isSameAs(items);
    }

    @Test
    @DisplayName("추가: 가장 큰 순서 + 1로 저장한다 (중간 삭제 후에도 순서가 겹치지 않음)")
    void add_nextOrderAfterMax() {
        given(watchListRepository.existsByUsernameAndStockCode("user1", "000660")).willReturn(false);
        given(watchListRepository.findMaxSortOrder("user1")).willReturn(2);   // A(0), C(2) — B(1) 삭제된 상태
        given(watchListRepository.save(any(WatchListEntity.class))).willAnswer(i -> i.getArgument(0));

        WatchListEntity saved = service.add("user1", "000660", "SK하이닉스");

        assertThat(saved.getSortOrder()).isEqualTo(3);
        assertThat(saved.getUsername()).isEqualTo("user1");
        assertThat(saved.getStockCode()).isEqualTo("000660");
        assertThat(saved.getStockName()).isEqualTo("SK하이닉스");
    }

    @Test
    @DisplayName("추가: 관심종목이 없으면 순서 0부터 시작한다")
    void add_firstItem() {
        given(watchListRepository.existsByUsernameAndStockCode("user1", "005930")).willReturn(false);
        given(watchListRepository.findMaxSortOrder("user1")).willReturn(-1);
        given(watchListRepository.save(any(WatchListEntity.class))).willAnswer(i -> i.getArgument(0));

        assertThat(service.add("user1", "005930", "삼성전자").getSortOrder()).isZero();
    }

    @Test
    @DisplayName("추가: 이미 있는 종목이면 409 예외이고 저장하지 않는다")
    void add_alreadyExists() {
        given(watchListRepository.existsByUsernameAndStockCode("user1", "005930")).willReturn(true);

        assertThatThrownBy(() -> service.add("user1", "005930", "삼성전자"))
                .isInstanceOfSatisfying(WatchListAlreadyExistsException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getMessage()).contains("005930");
                });
        verify(watchListRepository, never()).save(any());
    }

    @Test
    @DisplayName("추가: 동시 추가로 유니크 제약에 걸리면 500이 아니라 409 예외로 바꾼다")
    void add_concurrentDuplicate() {
        given(watchListRepository.existsByUsernameAndStockCode("user1", "005930")).willReturn(false);
        given(watchListRepository.findMaxSortOrder("user1")).willReturn(-1);
        given(watchListRepository.save(any(WatchListEntity.class)))
                .willThrow(new DataIntegrityViolationException("uk_watch_lists"));

        assertThatThrownBy(() -> service.add("user1", "005930", "삼성전자"))
                .isInstanceOf(WatchListAlreadyExistsException.class);
    }

    @Test
    @DisplayName("삭제: 관심종목에 있으면 삭제한다")
    void remove() {
        given(watchListRepository.existsByUsernameAndStockCode("user1", "005930")).willReturn(true);

        service.remove("user1", "005930");

        verify(watchListRepository).deleteByUsernameAndStockCode("user1", "005930");
    }

    @Test
    @DisplayName("삭제: 관심종목에 없으면 404 예외이고 삭제하지 않는다")
    void remove_notFound() {
        given(watchListRepository.existsByUsernameAndStockCode("user1", "005930")).willReturn(false);

        assertThatThrownBy(() -> service.remove("user1", "005930"))
                .isInstanceOfSatisfying(WatchListNotFoundException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getMessage()).contains("005930");
                });
        verify(watchListRepository, never()).deleteByUsernameAndStockCode(any(), any());
    }
}
