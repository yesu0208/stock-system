package arile.toy.stocksystem.stockserver.exception.market;

public class PastMarketHolidayDateException extends IllegalArgumentException {
    public PastMarketHolidayDateException() {
        super("휴장일은 오늘보다 미래 날짜만 등록할 수 있습니다.");
    }
}
