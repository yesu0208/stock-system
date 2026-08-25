package arile.toy.stocksystem.bffserver.stockinfo.dto;

import java.util.List;

public record UpjongStockResponse(
        String upjongName,
        List<UpjongStock> items
) {
}
