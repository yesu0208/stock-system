package arile.toy.stocksystem.bffserver.chart.token;

public record ChartApiTokenResponse(
        String access_token,
        String token_type,
        Long expires_in,
        String access_token_token_expired
) {
}
