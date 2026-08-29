package arile.toy.stocksystem.accountserver.useraccount.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

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
    private Instant createdDateTime;

    @Column(nullable = false)
    private Instant updatedDateTime;

    public static UserAccountEntity of(String username, Long balance) {
        var userAccountEntity = new UserAccountEntity();
        userAccountEntity.setUsername(username);
        userAccountEntity.setBalance(balance);
        return userAccountEntity;
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
