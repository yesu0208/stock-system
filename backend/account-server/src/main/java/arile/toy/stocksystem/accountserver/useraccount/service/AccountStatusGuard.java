package arile.toy.stocksystem.accountserver.useraccount.service;

import arile.toy.stocksystem.accountserver.useraccount.dto.AccountStatus;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountStatusGuard {

    private final UserAccountRepository userAccountRepository;

    /** 매수(현금 예약) 허용 여부. NEGATIVE/SUSPENDED 모두 매수 금지. */
    public boolean allowBuy(String username) {
        AccountStatus status = resolveStatus(username);
        return status == AccountStatus.NORMAL;
    }

    /** 매도(재고 예약) 허용 여부. NEGATIVE는 매도 허용(부족분 해소 유도), SUSPENDED만 금지. */
    public boolean allowSell(String username) {
        AccountStatus status = resolveStatus(username);
        return status != AccountStatus.SUSPENDED;
    }

    private AccountStatus resolveStatus(String username) {
        return userAccountRepository.findByUsername(username)
                .map(account -> account.getAccountStatus())
                .orElse(AccountStatus.NORMAL); // 계좌가 없으면(신규 유저 등) 기본 정상 취급 — 다른 검증 단계에서 걸러짐
    }
}
