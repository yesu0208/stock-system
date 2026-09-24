package arile.toy.stocksystem.bffserver.admin.config;

import arile.toy.stocksystem.bffserver.user.dto.Role;
import arile.toy.stocksystem.bffserver.user.entity.UserEntity;
import arile.toy.stocksystem.bffserver.user.event.UserCreatedEvent;
import arile.toy.stocksystem.bffserver.user.event.publisher.UserCreatedEventPublisher;
import arile.toy.stocksystem.bffserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountInitializer implements ApplicationRunner {

    private final AdminAccountProperties adminAccountProperties;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserCreatedEventPublisher userCreatedEventPublisher;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = adminAccountProperties.getUsername();
        String password = adminAccountProperties.getPassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("관리자 계정 설정(admin.username/admin.password)이 비어있어 초기화를 건너뜁니다.");
            return;
        }

        userRepository.findByUsername(username).ifPresentOrElse(
                this::ensureAdminRole,
                () -> createAdminAccount(username, password)
        );

        requestTradingAccount(username);
    }

    private void createAdminAccount(String username, String password) {
        var admin = UserEntity.of(username, bCryptPasswordEncoder.encode(password),
                adminAccountProperties.getNickname());
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("관리자 계정을 생성했습니다. username={}", username);
    }

    private void ensureAdminRole(UserEntity existing) {
        if (existing.getRole() != Role.ADMIN) {
            existing.setRole(Role.ADMIN);
            userRepository.save(existing);
            log.info("기존 계정을 관리자로 승격했습니다. username={}", existing.getUsername());
        }
    }

    /**
     * 관리자 계정의 거래 계좌 생성을 account-server에 요청.
     */
    private void requestTradingAccount(String username) {
        try {
            userCreatedEventPublisher.publishUserCreatedEvent(UserCreatedEvent.of(username));
            log.info("관리자 계좌 생성을 요청했습니다. username={}", username);
        } catch (Exception e) {
            log.error("관리자 계좌 생성 요청에 실패했습니다. 다음 재시작 때 다시 요청합니다. username={}", username, e);
        }
    }
}
