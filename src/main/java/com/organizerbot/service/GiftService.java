package com.organizerbot.service;

import com.organizerbot.dao.DaoFactory;
import com.organizerbot.dao.GiftDao;
import com.organizerbot.model.GiftRecord;
import com.organizerbot.model.GiftRecord.Gift;

import java.util.List;
import java.util.Map;

public class GiftService {
    private final GiftDao dao = DaoFactory.getInstance();

    public String addGift(Long userId, String recipient, Gift gift) {
        GiftRecord record = dao.getUserData(userId);
        record.getGifts().computeIfAbsent(recipient, k -> new java.util.ArrayList<>()).add(gift);
        dao.saveUserData(userId, record);
        return "🎁 Подарок для " + recipient + " добавлен!";
    }

    public String listGifts(Long userId) {
        GiftRecord record = dao.getUserData(userId);
        Map<String, List<Gift>> giftsByRecipient = record.getGifts();

        if (giftsByRecipient.isEmpty()) {
            return "🗒️ Список подарков пуст.";
        }

        StringBuilder sb = new StringBuilder("📜 Список подарков:\n");
        for (Map.Entry<String, List<Gift>> entry : giftsByRecipient.entrySet()) {
            sb.append("👤 ").append(entry.getKey()).append(":\n");
            List<Gift> gifts = entry.getValue();
            for (int i = 0; i < gifts.size(); i++) {
                Gift g = gifts.get(i);
                sb.append("  ").append(i + 1).append(") ")
                        .append(g.getName()).append(", ")
                        .append(g.getAmount()).append("₽, ")
                        .append(g.getDate()).append(", ")
                        .append(g.getComment()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
