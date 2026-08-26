package arile.toy.stocksystem.bffserver.chart.dto;

import java.util.List;

public record ChartResponse(
        String rt_cd,
        String msg_cd,
        String msg1,
        List<ChartItem> output2
) {
}
