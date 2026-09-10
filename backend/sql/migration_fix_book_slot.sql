-- 修正预约唯一键：纳入状态，退约后的记录不再占用该时段
ALTER TABLE booking DROP INDEX uk_book_slot;
ALTER TABLE booking ADD CONSTRAINT uk_book_slot UNIQUE (room_id, book_date, start_min, status);
