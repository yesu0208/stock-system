package arile.toy.stocksystem.bffserver.stockinfo.dto;

import java.util.List;

public record UpjongResponse(
        List<UpjongInfo> items
) {
}
