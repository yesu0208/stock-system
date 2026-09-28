CREATE TABLE `alert_cancels` (
                                 `alert_cancel_id` bigint NOT NULL AUTO_INCREMENT,
                                 `alert_id` bigint NOT NULL,
                                 `cancel_time` datetime(6) NOT NULL,
                                 PRIMARY KEY (`alert_cancel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `alerts` (
                          `trigger_price` int NOT NULL,
                          `alert_id` bigint NOT NULL AUTO_INCREMENT,
                          `registered_time` datetime(6) NOT NULL,
                          `stock_code` varchar(255) NOT NULL,
                          `username` varchar(255) NOT NULL,
                          `direction` enum('ABOVE','BELOW') NOT NULL,
                          `status` enum('ACTIVE','CANCELED','FIRED') NOT NULL,
                          PRIMARY KEY (`alert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `auto_cancels` (
                                `auto_cancel_id` bigint NOT NULL AUTO_INCREMENT,
                                `auto_order_id` bigint NOT NULL,
                                `cancel_time` datetime(6) NOT NULL,
                                PRIMARY KEY (`auto_cancel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `auto_orders` (
                               `order_price` int NOT NULL,
                               `order_quantity` int NOT NULL,
                               `trigger_price` int NOT NULL,
                               `auto_order_id` bigint NOT NULL AUTO_INCREMENT,
                               `order_time` datetime(6) NOT NULL,
                               `stock_code` varchar(255) NOT NULL,
                               `username` varchar(255) NOT NULL,
                               `auto_order_status` enum('ACTIVE','CANCELED','TRIGGERED') NOT NULL,
                               `auto_order_type` enum('BUY','SELL') NOT NULL,
                               `leverage_ratio` enum('SPOT','X1_5','X2','X2_5') NOT NULL,
                               PRIMARY KEY (`auto_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `cancels` (
                           `cancel_id` bigint NOT NULL AUTO_INCREMENT,
                           `cancel_time` datetime(6) NOT NULL,
                           `order_id` bigint NOT NULL,
                           PRIMARY KEY (`cancel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `market_holiday` (
                                  `holiday_date` date NOT NULL,
                                  `memo` varchar(255) DEFAULT NULL,
                                  PRIMARY KEY (`holiday_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `orders` (
                          `order_price` int NOT NULL,
                          `order_quantity` int NOT NULL,
                          `remaining_quantity` int NOT NULL,
                          `order_id` bigint NOT NULL AUTO_INCREMENT,
                          `order_time` datetime(6) NOT NULL,
                          `origin_id` bigint DEFAULT NULL,
                          `remaining_reserved_fee` bigint DEFAULT NULL,
                          `remaining_reserved_margin` bigint DEFAULT NULL,
                          `stock_code` varchar(255) NOT NULL,
                          `username` varchar(255) NOT NULL,
                          `leverage_ratio` enum('SPOT','X1_5','X2','X2_5') NOT NULL,
                          `order_execution_type` enum('LIMIT','MARKET') NOT NULL,
                          `order_status` enum('CANCELED','FILLED','OPEN','PARTIAL') NOT NULL,
                          `order_type` enum('BUY','SELL') NOT NULL,
                          `origin` enum('AUTO_ORDER','MANUAL','OTOCO_ENTRY','OTOCO_STOP_LOSS','OTOCO_TAKE_PROFIT','TRAILING_STOP') NOT NULL,
                          PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `otoco_cancels` (
                                 `cancel_time` datetime(6) NOT NULL,
                                 `otoco_cancel_id` bigint NOT NULL AUTO_INCREMENT,
                                 `otoco_id` bigint NOT NULL,
                                 PRIMARY KEY (`otoco_cancel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `otocos` (
                          `entry_trigger_price` int NOT NULL,
                          `order_quantity` int NOT NULL,
                          `sl_pct` double DEFAULT NULL,
                          `sl_price` int DEFAULT NULL,
                          `sl_trigger_price` int NOT NULL,
                          `tp_pct` double DEFAULT NULL,
                          `tp_price` int DEFAULT NULL,
                          `tp_trigger_price` int NOT NULL,
                          `entry_order_id` bigint DEFAULT NULL,
                          `order_time` datetime(6) NOT NULL,
                          `otoco_id` bigint NOT NULL AUTO_INCREMENT,
                          `stock_code` varchar(255) NOT NULL,
                          `username` varchar(255) NOT NULL,
                          `completed_leg` enum('STOP_LOSS','TAKE_PROFIT') DEFAULT NULL,
                          `entry_direction` enum('ABOVE','BELOW') NOT NULL,
                          `leverage_ratio` enum('SPOT','X1_5','X2','X2_5') NOT NULL,
                          `otoco_status` enum('CANCELED','COMPLETED','ENTRY_ORDER_PLACED','WAITING_ENTRY','WAITING_EXIT') NOT NULL,
                          `sl_mode` enum('PCT','PRICE') NOT NULL,
                          `tp_mode` enum('PCT','PRICE') NOT NULL,
                          PRIMARY KEY (`otoco_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `trade_outbox_events` (
                                       `created_date_time` datetime(6) NOT NULL,
                                       `outbox_id` bigint NOT NULL AUTO_INCREMENT,
                                       `published_date_time` datetime(6) DEFAULT NULL,
                                       `event_type` varchar(255) NOT NULL,
                                       `payload` text NOT NULL,
                                       `status` enum('PENDING','PUBLISHED') NOT NULL,
                                       PRIMARY KEY (`outbox_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `trades` (
                          `trade_price` int NOT NULL,
                          `trade_quantity` int NOT NULL,
                          `executed_at` datetime(6) NOT NULL,
                          `order_id` bigint NOT NULL,
                          `origin_id` bigint DEFAULT NULL,
                          `trade_id` bigint NOT NULL AUTO_INCREMENT,
                          `stock_code` varchar(255) NOT NULL,
                          `username` varchar(255) NOT NULL,
                          `leverage_ratio` enum('SPOT','X1_5','X2','X2_5') NOT NULL,
                          `origin` enum('AUTO_ORDER','MANUAL','OTOCO_ENTRY','OTOCO_STOP_LOSS','OTOCO_TAKE_PROFIT','TRAILING_STOP') NOT NULL,
                          `trade_type` enum('BUY','SELL') NOT NULL,
                          PRIMARY KEY (`trade_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `trailing_stop_cancels` (
                                         `cancel_time` datetime(6) NOT NULL,
                                         `trailing_stop_cancel_id` bigint NOT NULL AUTO_INCREMENT,
                                         `trailing_stop_id` bigint NOT NULL,
                                         PRIMARY KEY (`trailing_stop_cancel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `trailing_stops` (
                                  `base_price` int NOT NULL,
                                  `current_base_price` int DEFAULT NULL,
                                  `current_trigger_price` int DEFAULT NULL,
                                  `order_quantity` int NOT NULL,
                                  `stop_percent` double NOT NULL,
                                  `trigger_price` int NOT NULL,
                                  `order_time` datetime(6) NOT NULL,
                                  `trailing_stop_id` bigint NOT NULL AUTO_INCREMENT,
                                  `stock_code` varchar(255) NOT NULL,
                                  `username` varchar(255) NOT NULL,
                                  `leverage_ratio` enum('SPOT','X1_5','X2','X2_5') NOT NULL,
                                  `trailing_stop_status` enum('ACTIVE','CANCELED','TRIGGERED') NOT NULL,
                                  `trailing_stop_type` enum('BUY','SELL') NOT NULL,
                                  PRIMARY KEY (`trailing_stop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
