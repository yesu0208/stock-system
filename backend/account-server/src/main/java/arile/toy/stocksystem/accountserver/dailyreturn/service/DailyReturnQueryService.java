package arile.toy.stocksystem.accountserver.dailyreturn.service;

import arile.toy.stocksystem.accountserver.dailyreturn.dto.DailyReturnHistoryItem;
import arile.toy.stocksystem.accountserver.dailyreturn.dto.DailyReturnHistoryResponse;
import arile.toy.stocksystem.accountserver.dailyreturn.entity.DailyReturnHistoryEntity;
import arile.toy.stocksystem.accountserver.dailyreturn.repository.DailyReturnHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DailyReturnQueryService {

    private final DailyReturnHistoryRepository dailyReturnHistoryRepository;

    public DailyReturnHistoryResponse getHistory(String username, LocalDate from, LocalDate to, int page, int size) {

        Page<DailyReturnHistoryEntity> result =
                dailyReturnHistoryRepository.search(username, from, to, PageRequest.of(page, size));

        var items = result.getContent().stream()
                .map(DailyReturnHistoryItem::fromEntity)
                .toList();

        return new DailyReturnHistoryResponse(items, result.hasNext());
    }
}
