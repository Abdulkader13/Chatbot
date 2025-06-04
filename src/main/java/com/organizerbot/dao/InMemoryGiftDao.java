package com.organizerbot.dao;

import com.organizerbot.model.GiftRecord;

import java.util.HashMap;
import java.util.Map;

public class InMemoryGiftDao implements GiftDao {
    private final Map<Long, GiftRecord> storage = new HashMap<>();

    @Override
    public GiftRecord load(Long userId) {
        return storage.get(userId);
    }

    @Override
    public void save(GiftRecord record) {
        storage.put(record.getUserId(), record);
    }
}
