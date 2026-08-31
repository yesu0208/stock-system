package arile.toy.stocksystem.accountserver.rank.controller;

import arile.toy.stocksystem.accountserver.rank.dto.RankHistoryResponse;
import arile.toy.stocksystem.accountserver.rank.dto.RankResponse;
import arile.toy.stocksystem.accountserver.rank.repository.UserRankRepository;
import arile.toy.stocksystem.accountserver.rank.service.RankHistoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/ranks")
@RequiredArgsConstructor
public class RankController {

    private final UserRankRepository userRankRepository;
    private final RankHistoryQueryService rankHistoryQueryService;

    @GetMapping("/{username}")
    public RankResponse getRank(@PathVariable String username) {
        var rank = userRankRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Rank not found: " + username));
        return RankResponse.fromEntity(rank);
    }

    @GetMapping("/{username}/history")
    public RankHistoryResponse getRankHistory(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return rankHistoryQueryService.getHistory(username, page, size);
    }
}
