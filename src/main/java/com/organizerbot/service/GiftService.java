package com.organizerbot.service;

import com.organizerbot.dao.GiftDao;
import com.organizerbot.dao.JsonGiftDao;
import com.organizerbot.model.GiftRecord;
import com.organizerbot.model.GiftRecord.Gift;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GiftService {
    private final GiftDao dao = new JsonGiftDao();
    private final Map<Long, GiftRecord> users = new HashMap<>();

    public GiftRecord getUser(Long id) {
        return users.computeIfAbsent(id, i -> dao.load(id));
    }

    public String addGift(Long userId, String recipient, Gift gift) {
        GiftRecord record = getUser(userId);
        List<Gift> gifts = record.getGiftsFor(recipient);
        for (Gift g : gifts) {
            if (g.getGiftName().equalsIgnoreCase(gift.getGiftName()) &&
                    g.getEventDate().getYear() == gift.getEventDate().getYear()) {
                return "⛔ Такой подарок уже был для " + recipient + " в этом году.";
            }
        }

        record.addGift(recipient, gift);
        dao.save(record);

        double total = record.getTotalSpentFor(recipient);
        double limit = record.getBudgetFor(recipient);
        if (total > limit) {
            return "🎁 Подарок добавлен, но превышен лимит для " + recipient + ": " + limit + "₽!";
        }

        return "✅ Подарок добавлен для " + recipient + "!";
    }

    public String listGifts(Long userId) {
        GiftRecord record = getUser(userId);
        if (record.getAllGifts().isEmpty()) return "📭 Список подарков пуст.";

        StringBuilder sb = new StringBuilder("🎁 Подарки по получателям:\n");
        for (Map.Entry<String, List<Gift>> entry : record.getAllGifts().entrySet()) {
            sb.append("\n👤 ").append(entry.getKey()).append(":\n");
            int index = 1;
            for (Gift g : entry.getValue()) {
                sb.append(index++).append(". ").append(g.toString()).append("\n");
            }
        }
        return sb.toString();
    }

    public void updateBudget(Long userId, String recipient, double budget) {
        GiftRecord record = getUser(userId);
        record.setIndividualBudget(recipient, budget);
        dao.save(record);
    }

    public double getBudget(Long userId, String recipient) {
        return getUser(userId).getBudgetFor(recipient);
    }

    // ✅ Получить все подарки по пользователю
    public Map<String, List<Gift>> getAllGifts(Long userId) {
        return getUser(userId).getAllGifts();
    }

    // ✅ Редактировать подарок
    public boolean editGift(Long userId, String recipient, int index, Gift newGift) {
        GiftRecord record = getUser(userId);
        boolean result = record.editGift(recipient, index, newGift);
        if (result) dao.save(record);
        return result;
    }

    // ✅ Удалить подарок (если решишь добавить)
    public boolean deleteGift(Long userId, String recipient, int index) {
        GiftRecord record = getUser(userId);
        boolean result = record.removeGift(recipient, index);
        if (result) dao.save(record);
        return result;
    }
}
