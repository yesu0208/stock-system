package arile.toy.stocksystem.bffserver.security.repository;

public interface RefreshTokenRepository {
    void save(String jti, String username, long ttlMillis);
    boolean exists(String jti);
    void delete(String jti);
}
