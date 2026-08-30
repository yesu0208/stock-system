package arile.toy.stocksystem.accountserver.useraccount.service;

import arile.toy.stocksystem.accountserver.rank.entity.UserRankEntity;
import arile.toy.stocksystem.accountserver.rank.repository.UserRankRepository;
import arile.toy.stocksystem.accountserver.useraccount.dto.UserAccountMessage;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRedisRepository;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccountService {

    private static final long INITIAL_BALANCE = 1_000_000_000L;

    private final UserAccountRepository userAccountRepository;
    private final UserAccountRedisRepository userAccountRedisRepository;
    private final UserRankRepository userRankRepository;
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
        userRankRepository.save(UserRankEntity.of(username, INITIAL_BALANCE));

        log.info("UserAccount created. username={}, cash={}", username, INITIAL_BALANCE);

        userAccountRedisRepository.save(userAccountEntity.getUsername(),
                UserAccountMessage.of(userAccountEntity.getUsername(), INITIAL_BALANCE, 0L,
                        new HashMap<>()));
    }

    @Transactional
    public void settleAccounts(Set<String> usernames) {

        for (String username : usernames) {
            Optional<UserAccountEntity> optionalAccount = userAccountRepository.findByUsername(username);

            optionalAccount.ifPresentOrElse(account -> {

                Long availableCash = userAccountRedisRepository.getAvailableCash(username);
                Long reservedCash = userAccountRedisRepository.getReservedCash(username);

                if (availableCash == null || reservedCash == null) {
                    log.error("Redis balance is null. username={}, availableCash={}, reservedCash={}",
                            username, availableCash, reservedCash);

                    // null 안전 처리 (0으로 간주)
                    availableCash = availableCash == null ? 0L : availableCash;
                    reservedCash = reservedCash == null ? 0L : reservedCash;
                }

                Long balance = availableCash + reservedCash;

                if (!account.getBalance().equals(balance) || reservedCash != 0) {
                    log.warn("Redis balance out of sync for user {}. DB: {}, Redis: {}",
                            username, account.getBalance(), balance);

                    userAccountRedisRepository.updateAccountAfterClose(username, balance);
                    accountUpdateEventPublisher.publish(username);
                } else {
                    log.info("Redis balance in sync for user {}", username);
                }
            }, () -> log.warn("Account not found for user {}", username));
        }

        log.info("Account settlement completed for {} users.", usernames.size());
    }

    @Transactional
    public void settleAllAccounts() {
        Set<String> allUsernames = new HashSet<>(userAccountRepository.findAllUsernames());
        settleAccounts(allUsernames);
    }
}
