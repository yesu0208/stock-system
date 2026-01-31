package arile.toy.stocksystem.stockserver.useraccount;

import arile.toy.stocksystem.stockserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.stockserver.useraccount.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccountService {

    private static final long INITIAL_BALANCE = 500_000_000L;

    private final UserAccountRepository userAccountRepository;

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
    }
}
