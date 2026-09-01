package arile.toy.stocksystem.accountserver.useraccount.entity;

import arile.toy.stocksystem.accountserver.useraccount.dto.AccountStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "user_accounts")
public class UserAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userAccountId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Long balance;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;

    /** 계좌 마이너스 전환일 (부족분 해소 3영업일 유예의 기준일). NORMAL이면 null */
    private LocalDate negativeBalanceStartDate;

    @Column(nullable = false)
    private Instant createdDateTime;

    @Column(nullable = false)
    private Instant updatedDateTime;

    public static UserAccountEntity of(String username, Long balance) {
        var userAccountEntity = new UserAccountEntity();
        userAccountEntity.setUsername(username);
        userAccountEntity.setBalance(balance);
        userAccountEntity.setAccountStatus(AccountStatus.NORMAL);
        return userAccountEntity;
    }

    public void changeAccountStatus(AccountStatus status, LocalDate negativeBalanceStartDate) {
        this.accountStatus = status;
        this.negativeBalanceStartDate = negativeBalanceStartDate;
    }

    @PrePersist
    private void prePersist() {
        this.createdDateTime = Instant.now();
        this.updatedDateTime = Instant.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedDateTime = Instant.now();
    }
}
