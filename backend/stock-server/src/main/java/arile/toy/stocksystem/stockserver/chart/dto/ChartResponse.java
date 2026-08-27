package arile.toy.stocksystem.stockserver.chart.dto;

import java.util.List;

public record ChartResponse(
        String rt_cd,
        String msg_cd,
        String msg1,
        List<ChartItem> output2
) {
}
