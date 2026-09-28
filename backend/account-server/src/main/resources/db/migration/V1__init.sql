CREATE TABLE `applied_trades` (
                                  `applied_at` datetime(6) NOT NULL,
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `trade_id` bigint NOT NULL,
                                  `stock_code` varchar(255) NOT NULL,
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_applied_trade` (`stock_code`,`trade_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `daily_return_histories` (
                                          `cumulative_profit_rate` double NOT NULL,
                                          `daily_profit_rate` double NOT NULL,
                                          `record_date` date NOT NULL,
                                          `cumulative_profit_amount` bigint NOT NULL,
                                          `daily_profit_amount` bigint NOT NULL,
                                          `daily_return_history_id` bigint NOT NULL AUTO_INCREMENT,
                                          `daily_trade_amount` bigint NOT NULL,
                                          `previous_day_total_asset` bigint NOT NULL,
                                          `total_asset` bigint NOT NULL,
                                          `username` varchar(255) NOT NULL,
                                          PRIMARY KEY (`daily_return_history_id`),
                                          UNIQUE KEY `UK8tymbq8ubkea25kjod82wtht8` (`username`,`record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `leverage_liquidations` (
                                         `liquidated_quantity` int NOT NULL,
                                         `liquidated_at` datetime(6) NOT NULL,
                                         `liquidation_id` bigint NOT NULL AUTO_INCREMENT,
                                         `proceeds` bigint NOT NULL,
                                         `repaid_loan_amount` bigint NOT NULL,
                                         `settlement_price` bigint NOT NULL,
                                         `shortfall` bigint NOT NULL,
                                         `stock_code` varchar(255) NOT NULL,
                                         `username` varchar(255) NOT NULL,
                                         `leverage_ratio` enum('SPOT','X1_5','X2','X2_5') NOT NULL,
                                         PRIMARY KEY (`liquidation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `leverage_positions` (
                                      `available_quantity` int NOT NULL,
                                      `last_interest_charged_date` date NOT NULL,
                                      `margin_call_date` date DEFAULT NULL,
                                      `quantity` int NOT NULL,
                                      `cost_amount` bigint NOT NULL,
                                      `created_date_time` datetime(6) NOT NULL,
                                      `leverage_position_id` bigint NOT NULL AUTO_INCREMENT,
                                      `loan_amount` bigint NOT NULL,
                                      `purchase_amount` bigint NOT NULL,
                                      `updated_date_time` datetime(6) NOT NULL,
                                      `stock_code` varchar(255) NOT NULL,
                                      `username` varchar(255) NOT NULL,
                                      `leverage_ratio` enum('SPOT','X1_5','X2','X2_5') NOT NULL,
                                      `margin_status` enum('LIQUIDATION_PENDING','MARGIN_CALL','NORMAL') NOT NULL,
                                      PRIMARY KEY (`leverage_position_id`),
                                      UNIQUE KEY `UK28s0jl9dwh6p8f126mhe1p9yg` (`username`,`stock_code`,`leverage_ratio`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `rank_histories` (
                                  `record_date` date NOT NULL,
                                  `rank_history_id` bigint NOT NULL AUTO_INCREMENT,
                                  `rp` bigint NOT NULL,
                                  `rp_change` bigint NOT NULL,
                                  `username` varchar(255) NOT NULL,
                                  `rank_level` enum('BRONZE_1','BRONZE_2','BRONZE_3','BRONZE_4','BRONZE_5','DIAMOND_1','DIAMOND_2','DIAMOND_3','DIAMOND_4','DIAMOND_5','GOLD_1','GOLD_2','GOLD_3','GOLD_4','GOLD_5','MASTER','PLATINUM_1','PLATINUM_2','PLATINUM_3','PLATINUM_4','PLATINUM_5','SILVER_1','SILVER_2','SILVER_3','SILVER_4','SILVER_5','UNRANKED') NOT NULL,
                                  PRIMARY KEY (`rank_history_id`),
                                  UNIQUE KEY `UKbnuo0q3j0xuyaufp1f3jwrfmt` (`username`,`record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_accounts` (
                                 `negative_balance_start_date` date DEFAULT NULL,
                                 `balance` bigint NOT NULL,
                                 `created_date_time` datetime(6) NOT NULL,
                                 `updated_date_time` datetime(6) NOT NULL,
                                 `user_account_id` bigint NOT NULL AUTO_INCREMENT,
                                 `username` varchar(255) NOT NULL,
                                 `account_status` enum('NEGATIVE','NORMAL','SUSPENDED') NOT NULL,
                                 PRIMARY KEY (`user_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_ranks` (
                              `entered` bit(1) NOT NULL,
                              `created_date_time` datetime(6) NOT NULL,
                              `daily_trade_amount` bigint NOT NULL,
                              `previous_day_total_asset` bigint NOT NULL,
                              `rp` bigint NOT NULL,
                              `updated_date_time` datetime(6) NOT NULL,
                              `user_rank_id` bigint NOT NULL AUTO_INCREMENT,
                              `username` varchar(255) NOT NULL,
                              `current_level` enum('BRONZE_1','BRONZE_2','BRONZE_3','BRONZE_4','BRONZE_5','DIAMOND_1','DIAMOND_2','DIAMOND_3','DIAMOND_4','DIAMOND_5','GOLD_1','GOLD_2','GOLD_3','GOLD_4','GOLD_5','MASTER','PLATINUM_1','PLATINUM_2','PLATINUM_3','PLATINUM_4','PLATINUM_5','SILVER_1','SILVER_2','SILVER_3','SILVER_4','SILVER_5','UNRANKED') NOT NULL,
                              `highest_tier_reached` enum('BRONZE_1','BRONZE_2','BRONZE_3','BRONZE_4','BRONZE_5','DIAMOND_1','DIAMOND_2','DIAMOND_3','DIAMOND_4','DIAMOND_5','GOLD_1','GOLD_2','GOLD_3','GOLD_4','GOLD_5','MASTER','PLATINUM_1','PLATINUM_2','PLATINUM_3','PLATINUM_4','PLATINUM_5','SILVER_1','SILVER_2','SILVER_3','SILVER_4','SILVER_5','UNRANKED') NOT NULL,
                              PRIMARY KEY (`user_rank_id`),
                              UNIQUE KEY `UK6xjkwx6cwxk6tfsj19p49sov0` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_stocks` (
                               `quantity` int NOT NULL,
                               `amount` bigint NOT NULL,
                               `cost_amount` bigint NOT NULL,
                               `created_date_time` datetime(6) NOT NULL,
                               `updated_date_time` datetime(6) NOT NULL,
                               `user_stock_id` bigint NOT NULL AUTO_INCREMENT,
                               `stock_code` varchar(255) NOT NULL,
                               `username` varchar(255) NOT NULL,
                               PRIMARY KEY (`user_stock_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
