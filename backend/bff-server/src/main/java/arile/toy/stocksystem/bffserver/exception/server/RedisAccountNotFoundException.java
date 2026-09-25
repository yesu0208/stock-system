package arile.toy.stocksystem.bffserver.exception.server;

public class RedisAccountNotFoundException extends RuntimeException {
    public RedisAccountNotFoundException(String message) {
        super(message);
    }
}
