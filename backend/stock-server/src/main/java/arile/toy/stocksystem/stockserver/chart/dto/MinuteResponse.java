package arile.toy.stocksystem.stockserver.chart.dto;

import java.util.List;

public record MinuteResponse(
        String rt_cd,
        String msg_cd,
        String msg1,
        List<MinuteItem> output2
) {
}
