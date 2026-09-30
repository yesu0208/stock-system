CREATE INDEX `idx_discussion_posts_stock_code_post_id` ON `discussion_posts` (`stock_code`, `post_id`);
CREATE INDEX `idx_discussion_posts_author_id_post_id` ON `discussion_posts` (`author_id`, `post_id`);

CREATE INDEX `idx_discussion_comments_post_id_comment_id` ON `discussion_comments` (`post_id`, `comment_id`);
CREATE INDEX `idx_discussion_comments_author_id` ON `discussion_comments` (`author_id`);

CREATE INDEX `idx_discussion_scraps_user_id` ON `discussion_scraps` (`user_id`);
