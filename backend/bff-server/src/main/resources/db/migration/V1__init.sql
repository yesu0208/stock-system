CREATE TABLE `discussion_comments` (
                                       `comment_id` bigint NOT NULL AUTO_INCREMENT,
                                       `created_date_time` datetime(6) NOT NULL,
                                       `post_id` bigint NOT NULL,
                                       `updated_date_time` datetime(6) NOT NULL,
                                       `content` varchar(1000) NOT NULL,
                                       `author_id` varchar(255) NOT NULL,
                                       PRIMARY KEY (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `discussion_posts` (
                                    `created_date_time` datetime(6) NOT NULL,
                                    `post_id` bigint NOT NULL AUTO_INCREMENT,
                                    `updated_date_time` datetime(6) NOT NULL,
                                    `content` varchar(2000) NOT NULL,
                                    `author_id` varchar(255) NOT NULL,
                                    `stock_code` varchar(255) NOT NULL,
                                    `stock_name` varchar(255) NOT NULL,
                                    `title` varchar(255) NOT NULL,
                                    PRIMARY KEY (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `discussion_reactions` (
                                        `reaction_id` bigint NOT NULL AUTO_INCREMENT,
                                        `target_id` bigint NOT NULL,
                                        `user_id` varchar(255) NOT NULL,
                                        `reaction_type` enum('DISLIKE','LIKE') NOT NULL,
                                        `target_type` enum('COMMENT','POST') NOT NULL,
                                        PRIMARY KEY (`reaction_id`),
                                        UNIQUE KEY `UK1sc7r3w85tdwbhs991ox6oywx` (`target_type`,`target_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `discussion_scraps` (
                                     `post_id` bigint NOT NULL,
                                     `scrap_id` bigint NOT NULL AUTO_INCREMENT,
                                     `user_id` varchar(255) NOT NULL,
                                     PRIMARY KEY (`scrap_id`),
                                     UNIQUE KEY `UKliaa30l7xd3piy4qu3wotgtdp` (`post_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notices` (
                           `created_date_time` datetime(6) NOT NULL,
                           `notice_id` bigint NOT NULL AUTO_INCREMENT,
                           `updated_date_time` datetime(6) NOT NULL,
                           `content` varchar(4000) NOT NULL,
                           `author_id` varchar(255) NOT NULL,
                           `title` varchar(255) NOT NULL,
                           PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `users` (
                         `created_date_time` datetime(6) NOT NULL,
                         `user_id` bigint NOT NULL AUTO_INCREMENT,
                         `nickname` varchar(255) NOT NULL,
                         `password` varchar(255) NOT NULL,
                         `profile_image_url` varchar(255) DEFAULT NULL,
                         `username` varchar(255) NOT NULL,
                         `role` enum('ADMIN','USER') DEFAULT NULL,
                         PRIMARY KEY (`user_id`),
                         UNIQUE KEY `UK2ty1xmrrgtn89xt7kyxx6ta7h` (`nickname`),
                         UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `watch_lists` (
                               `sort_order` int NOT NULL,
                               `created_date_time` datetime(6) NOT NULL,
                               `watch_list_id` bigint NOT NULL AUTO_INCREMENT,
                               `stock_code` varchar(255) NOT NULL,
                               `stock_name` varchar(255) NOT NULL,
                               `username` varchar(255) NOT NULL,
                               PRIMARY KEY (`watch_list_id`),
                               UNIQUE KEY `UKonvjjwbuhnjceel6ondah33ha` (`username`,`stock_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
