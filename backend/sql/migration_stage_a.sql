-- 阶段 A 索引补强，对已建库执行一次
ALTER TABLE booking ADD INDEX idx_book_user_date (user_id, book_date);
ALTER TABLE watch ADD INDEX idx_watch_user (user_id);
ALTER TABLE watch ADD INDEX idx_watch_room_date (room_id, book_date);
