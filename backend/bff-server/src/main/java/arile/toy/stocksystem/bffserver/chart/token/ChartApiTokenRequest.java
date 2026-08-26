package arile.toy.stocksystem.bffserver.chart.token;

public record ChartApiTokenRequest(
        String grant_type,
        String appkey,
        String appsecret
) {
    public static ChartApiTokenRequest of(String appkey, String appsecret) {
        return new ChartApiTokenRequest("client_credentials", appkey, appsecret);
    }
}
