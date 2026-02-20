package arile.toy.stocksystem.bffserver.user.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import arile.toy.stocksystem.bffserver.user.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    private final String username = "user1";

    private UserEntity mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new UserEntity();
        mockUser.setUserId(1L);
        mockUser.setUsername(username);
        mockUser.setPassword("password123");
    }

    @Test
    @DisplayName("존재하는 username으로 조회하면 User 반환")
    void givenExistingUsername_whenFindByUsername_thenReturnsUser() {
        // given
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

        // when
        Optional<UserEntity> result = userRepository.findByUsername(username);

        // then
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("존재하지 않는 username으로 조회하면 Optional.empty 반환")
    void givenNonExistingUsername_whenFindByUsername_thenReturnsEmpty() {
        // given
        when(userRepository.findByUsername("user2")).thenReturn(Optional.empty());

        // when
        Optional<UserEntity> result = userRepository.findByUsername("user2");

        // then
        assertFalse(result.isPresent());
        verify(userRepository).findByUsername("user2");
    }
}
