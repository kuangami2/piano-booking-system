-- 预约唯一约束修正：只对进行中预约生效，退约与完成后不再占用槽位
ALTER TABLE booking DROP INDEX uk_book_slot;
ALTER TABLE booking ADD COLUMN active_slot VARCHAR(64)
    GENERATED ALWAYS AS (IF(status = 'booked', CONCAT(room_id, '-', book_date, '-', start_min), NULL)) STORED
    COMMENT '进行中预约的唯一槽位，退约或完成后为空';
ALTER TABLE booking ADD UNIQUE KEY uk_book_active_slot (active_slot);
