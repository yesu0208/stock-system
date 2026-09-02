package arile.toy.stocksystem.bffserver.exception.leverage;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class LeverageNotAllowedException extends ClientErrorException {

    public LeverageNotAllowedException(String requiredTierName) {
        super(HttpStatus.FORBIDDEN, "해당 레버리지를 사용하려면 " + requiredTierName + " 등급 이상이어야 합니다.");
    }

    public LeverageNotAllowedException() {
        super(HttpStatus.FORBIDDEN, "등급 정보를 확인할 수 없어 레버리지 주문이 제한됩니다. 잠시 후 다시 시도해주세요.");
    }
}
