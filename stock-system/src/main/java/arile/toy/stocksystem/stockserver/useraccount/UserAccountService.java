package arile.toy.stocksystem.stockserver.useraccount;

import arile.toy.stocksystem.stockserver.useraccount.dto.StockServerAccountMessage;
import arile.toy.stocksystem.stockserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.stockserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.stockserver.useraccount.repository.StockServerAccountRepository;
import arile.toy.stocksystem.stockserver.useraccount.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccountService {

    private static final long INITIAL_BALANCE = 500_000_000L;

    private final UserAccountRepository userAccountRepository;
    private final StockServerAccountRepository stockServerAccountRepository;
    private final AccountUpdateEventPublisher accountUpdateEventPublisher;

    @Transactional
    public void createAccountIfAbsent(String username) {

        if (userAccountRepository.existsByUsername(username)) {
            log.warn("UserAccount already exists. userId={}", username);
            return;
        }

        var userAccountEntity =
                UserAccountEntity.of(username, INITIAL_BALANCE);

        userAccountRepository.save(userAccountEntity);

        log.info("UserAccount created. username={}, cash={}", username, INITIAL_BALANCE);

        stockServerAccountRepository.save(userAccountEntity.getUsername(),
                    StockServerAccountMessage.of(userAccountEntity.getUsername(), INITIAL_BALANCE, 0L,
                            new HashMap<>()));
    }

    @Transactional
    public void settleAccounts(Set<String> usernames) {

        for (String username : usernames) {
            Optional<UserAccountEntity> optionalAccount = userAccountRepository.findByUsername(username);

            optionalAccount.ifPresentOrElse(account -> {

                Long availableCash = stockServerAccountRepository.getAvailableCash(username);
                Long reservedCash = stockServerAccountRepository.getReservedCash(username);
                Long balance = availableCash + reservedCash;

                if (!account.getBalance().equals(balance) || reservedCash != 0) {
                    log.warn("Redis balance out of sync for user {}. DB: {}, Redis: {}",
                            username, account.getBalance(), balance);

                    stockServerAccountRepository.updateAccountAfterClose(username, balance);
                    accountUpdateEventPublisher.publish(username);
                } else {
                    log.info("Redis balance in sync for user {}", username);
                }
            }, () -> log.warn("Account not found for user {}", username));
        }

        log.info("Account settlement completed for {} users.", usernames.size());
    }
}
