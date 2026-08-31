package arile.toy.stocksystem.accountserver.rank.service;

import arile.toy.stocksystem.accountserver.rank.dto.RankHistoryItem;
import arile.toy.stocksystem.accountserver.rank.dto.RankHistoryResponse;
import arile.toy.stocksystem.accountserver.rank.entity.RankHistoryEntity;
import arile.toy.stocksystem.accountserver.rank.repository.RankHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RankHistoryQueryService {

    private final RankHistoryRepository rankHistoryRepository;

    public RankHistoryResponse getHistory(String username, int page, int size) {

        Page<RankHistoryEntity> result = rankHistoryRepository.findByUsernameOrderByRecordDateDesc(
                username, PageRequest.of(page, size));

        var items = result.getContent().stream()
                .map(RankHistoryItem::fromEntity)
                .toList();

        return new RankHistoryResponse(items, result.hasNext());
    }
}
