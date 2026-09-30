CREATE INDEX `idx_orders_stock_code_status` ON `orders` (`stock_code`, `order_status`);
CREATE INDEX `idx_orders_username_order_time` ON `orders` (`username`, `order_time`);

CREATE INDEX `idx_auto_orders_stock_code_status` ON `auto_orders` (`stock_code`, `auto_order_status`);
CREATE INDEX `idx_auto_orders_username_order_time` ON `auto_orders` (`username`, `order_time`);

CREATE INDEX `idx_trailing_stops_stock_code_status` ON `trailing_stops` (`stock_code`, `trailing_stop_status`);
CREATE INDEX `idx_trailing_stops_username_order_time` ON `trailing_stops` (`username`, `order_time`);

CREATE INDEX `idx_otocos_stock_code_status` ON `otocos` (`stock_code`, `otoco_status`);
CREATE INDEX `idx_otocos_username_order_time` ON `otocos` (`username`, `order_time`);

CREATE INDEX `idx_alerts_stock_code_status` ON `alerts` (`stock_code`, `status`);

CREATE INDEX `idx_trades_username_executed_at` ON `trades` (`username`, `executed_at`);

CREATE INDEX `idx_trade_outbox_events_status_outbox_id` ON `trade_outbox_events` (`status`, `outbox_id`);
