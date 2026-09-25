package arile.toy.stocksystem.stockserver.otoco;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.otoco.dto.*;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;

import java.time.Instant;

/** OTOCO 테스트 공통 픽스처: 진입 70,000 / TP 73,500 / SL 67,900 / 10주 */
public final class OtocoFixtures {

    public static final String USERNAME = "user";
    public static final String STOCK_CODE = "005930";

    private OtocoFixtures() {
    }

    public static OtocoEntity entity(Long id, OtocoStatus status, LeverageRatio ratio) {
        OtocoEntity entity = OtocoEntity.of(USERNAME, STOCK_CODE, OtocoEntryDirection.BELOW, ratio, 10, 70_000,
                OtocoExitMode.PRICE, 73_500, null, 73_500,
                OtocoExitMode.PRICE, 67_900, null, 67_900);
        entity.setOtocoId(id);
        entity.setOtocoStatus(status);
        return entity;
    }

    public static OtocoEntity entity(OtocoStatus status) {
        return entity(1L, status, LeverageRatio.SPOT);
    }

    public static OtocoDto dto(Long id, OtocoEntryDirection direction, OtocoStatus status) {
        return new OtocoDto(id, USERNAME, STOCK_CODE, direction, LeverageRatio.SPOT, 10,
                70_000, 73_500, 67_900, null, status, Instant.now(), null);
    }

    public static OtocoDto dto(OtocoStatus status) {
        return dto(1L, OtocoEntryDirection.BELOW, status);
    }
}
