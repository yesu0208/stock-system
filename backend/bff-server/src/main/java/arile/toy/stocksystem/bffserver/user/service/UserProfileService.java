package arile.toy.stocksystem.bffserver.user.service;

import arile.toy.stocksystem.bffserver.user.dto.UserProfile;
import arile.toy.stocksystem.bffserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfile getProfile(String username) {
        return userRepository.findByUsername(username)
                .map(entity -> new UserProfile(
                        entity.getUsername(), entity.getNickname(), entity.getProfileImageUrl()))
                .orElseGet(() -> UserProfile.empty(username));
    }

    public Map<String, UserProfile> getProfiles(Collection<String> usernames) {
        if (usernames.isEmpty()) {
            return Map.of();
        }

        Set<String> distinctUsernames = new HashSet<>(usernames);

        return userRepository.findByUsernameIn(distinctUsernames).stream()
                .collect(Collectors.toMap(
                        UserRepository.UserProfileProjection::getUsername,
                        p -> new UserProfile(p.getUsername(), p.getNickname(), p.getProfileImageUrl())
                ));
    }
}
