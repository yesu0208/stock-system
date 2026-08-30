package arile.toy.stocksystem.accountserver.rank.controller;

import arile.toy.stocksystem.accountserver.rank.dto.RankResponse;
import arile.toy.stocksystem.accountserver.rank.repository.UserRankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ranks")
@RequiredArgsConstructor
public class RankController {

    private final UserRankRepository userRankRepository;

    @GetMapping("/{username}")
    public RankResponse getRank(@PathVariable String username) {
        var rank = userRankRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Rank not found: " + username));
        return RankResponse.fromEntity(rank);
    }
}
