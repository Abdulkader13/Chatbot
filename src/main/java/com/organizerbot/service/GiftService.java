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

    // ⚠️ Always loads from disk if not in cache
    public GiftRecord getUser(Long id) {
        return users.computeIfAbsent(id, i -> {
            GiftRecord record = dao.load(id);
            if (record == null) record = new GiftRecord(i);
            return record;
        });
    }

    // 🔁 Reloads from disk forcibly
    public GiftRecord refreshUser(Long id) {
        GiftRecord record = dao.load(id);
        if (record == null) record = new GiftRecord(id);
        users.put(id, record);
        return record;
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

        // 🔍 Budget check BEFORE saving
        double totalIfAdded = record.getTotalSpentFor(recipient) + gift.getPrice();
        double limit = record.getBudgetFor(recipient);
        String warning = "";
        if (totalIfAdded > limit) {
            warning = "⚠️ Превышен лимит бюджета для " + recipient + " (" + limit + "₽)!\n";
        }

        record.addGift(recipient, gift);
        dao.save(record);
        users.put(userId, record); // update in-memory cache too

        return warning + "✅ Подарок добавлен для " + recipient + "!";
    }

    public String listGifts(Long userId) {
        GiftRecord record = refreshUser(userId);
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
        users.put(userId, record);
    }

    public double getBudget(Long userId, String recipient) {
        return getUser(userId).getBudgetFor(recipient);
    }

    public Map<String, List<Gift>> getAllGifts(Long userId) {
        return refreshUser(userId).getAllGifts(); // always up-to-date
    }

    public boolean editGift(Long userId, String recipient, int index, Gift newGift) {
        GiftRecord record = getUser(userId);
        boolean result = record.editGift(recipient, index, newGift);
        if (result) {
            dao.save(record); // 💾 Persist immediately
            users.put(userId, record); // 🧠 Update memory
        }
        return result;
    }

    public boolean deleteGift(Long userId, String recipient, int index) {
        GiftRecord record = getUser(userId);
        boolean result = record.removeGift(recipient, index);

        if (result) {
            // 🧹 Remove recipient if no more gifts
            if (record.getGiftsFor(recipient).isEmpty()) {
                record.getAllGifts().remove(recipient);
            }
            dao.save(record);  // 💾 Always save changes
            users.put(userId, record);
        }

        return result;
    }

    // ✅ For reminder system
    public Map<Long, GiftRecord> getAllUserGiftRecords() {
        return users;
    }
}
