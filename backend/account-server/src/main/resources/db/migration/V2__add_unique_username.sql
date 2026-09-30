ALTER TABLE `user_accounts` ADD CONSTRAINT `uk_user_accounts_username` UNIQUE (`username`);
ALTER TABLE `user_stocks` ADD CONSTRAINT `uk_user_stocks_username_stock_code` UNIQUE (`username`, `stock_code`);
