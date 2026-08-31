package arile.toy.stocksystem.bffserver.watchlist.dto;

import arile.toy.stocksystem.bffserver.watchlist.entity.WatchListEntity;

public record WatchListItemResponse(
        String stockCode,
        String stockName,
        Integer sortOrder
) {
    public static WatchListItemResponse fromEntity(WatchListEntity entity) {
        return new WatchListItemResponse(
                entity.getStockCode(),
                entity.getStockName(),
                entity.getSortOrder()
        );
    }
}
