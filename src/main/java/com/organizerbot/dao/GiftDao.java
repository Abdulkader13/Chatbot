package com.organizerbot.dao;

import com.organizerbot.model.GiftRecord;

public interface GiftDao {
    void save(GiftRecord user);
    GiftRecord load(Long telegramId);
}
