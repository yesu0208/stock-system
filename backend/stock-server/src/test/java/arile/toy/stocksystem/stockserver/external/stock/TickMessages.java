package arile.toy.stocksystem.stockserver.external.stock;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class TickMessages {

    private TickMessages() {
    }

    public static String[] tradeRecord(String stockCode, String curPrice, int prevCloseDiff) {
        String[] f = new String[47];
        Arrays.fill(f, "0");
        f[0] = stockCode;
        f[1] = "093000";
        f[2] = curPrice;
        f[4] = String.valueOf(prevCloseDiff);
        f[7] = "69000";
        f[8] = "71000";
        f[9] = "68500";
        f[12] = "10";
        f[13] = "1000";
        f[14] = "70000000";
        f[19] = "400";
        f[20] = "600";
        f[21] = "1";
        f[41] = "900";
        return f;
    }

    public static String[] bidAskRecord(String stockCode) {
        String[] f = new String[60];
        Arrays.fill(f, "0");
        f[0] = stockCode;
        for (int i = 0; i < 20; i++) {
            f[3 + i] = String.valueOf(70_000 + i * 100);
            f[23 + i] = String.valueOf(i + 1);
        }
        f[43] = "210";
        f[44] = "0";
        return f;
    }

    // List.of(String[]) 한 건은 가변인자로 풀려 List<String>이 되므로 가변인자로 받음
    public static String message(String trId, String[]... records) {
        String payload = Arrays.stream(records).map(r -> String.join("^", r)).collect(Collectors.joining("^"));
        return "0|" + trId + "|" + records.length + "|" + payload;
    }
}
