package arile.toy.stocksystem.bffserver.user.dto;

public record UserProfile(
        String username,
        String nickname,
        String profileImageUrl
) {
    public static UserProfile empty(String username) {
        return new UserProfile(username, username, null);
    }
}
