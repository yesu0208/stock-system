package arile.toy.stocksystem.stockserver.useraccount.repository;

import arile.toy.stocksystem.stockserver.useraccount.entity.UserAccountEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountRepositoryTest {

    @Spy
    private UserAccountRepository userAccountRepository;

    @Test
    @DisplayName("저장된 사용자를 username으로 조회하면 반환된다")
    void givenSavedUser_whenFindByUsername_thenReturnUser() {
        // given
        UserAccountEntity user = new UserAccountEntity();
        user.setUsername("user1");

        when(userAccountRepository.findByUsername("user1")).thenReturn(Optional.of(user));

        // when
        Optional<UserAccountEntity> found = userAccountRepository.findByUsername("user1");

        // then
        assertTrue(found.isPresent());
        assertEquals(user, found.get());
        verify(userAccountRepository).findByUsername("user1");
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 username으로 조회하면 empty 반환")
    void givenNoUser_whenFindByUsername_thenReturnEmpty() {
        // given
        when(userAccountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // when
        Optional<UserAccountEntity> found = userAccountRepository.findByUsername("nonexistent");

        // then
        assertFalse(found.isPresent());
        verify(userAccountRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("사용자가 존재하면 existsByUsername 호출 시 true 반환")
    void givenUsernameExists_whenExistsByUsername_thenReturnTrue() {
        // given
        when(userAccountRepository.existsByUsername("user2")).thenReturn(true);

        // when
        boolean exists = userAccountRepository.existsByUsername("user2");

        // then
        assertTrue(exists);
        verify(userAccountRepository).existsByUsername("user2");
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 조회할 때 findByUsernameForUpdate는 empty 반환")
    void givenNoUser_whenFindByUsernameForUpdate_thenReturnEmpty() {
        // given
        when(userAccountRepository.findByUsernameForUpdate("nonexistent")).thenReturn(Optional.empty());

        // when
        Optional<UserAccountEntity> found = userAccountRepository.findByUsernameForUpdate("nonexistent");

        // then
        assertFalse(found.isPresent());
        verify(userAccountRepository).findByUsernameForUpdate("nonexistent");
    }

    @Test
    @DisplayName("저장된 사용자를 조회할 때 findByUsernameForUpdate는 사용자 반환")
    void givenSavedUser_whenFindByUsernameForUpdate_thenReturnUser() {
        // given
        UserAccountEntity user = new UserAccountEntity();
        user.setUsername("user3");

        when(userAccountRepository.findByUsernameForUpdate("user3")).thenReturn(Optional.of(user));

        // when
        Optional<UserAccountEntity> found = userAccountRepository.findByUsernameForUpdate("user3");

        // then
        assertTrue(found.isPresent());
        assertEquals(user, found.get());
        verify(userAccountRepository).findByUsernameForUpdate("user3");
    }
}
