package com.organizerbot.service;

import com.organizerbot.dao.GiftDao;
import com.organizerbot.dao.JsonGiftDao;
import com.organizerbot.model.GiftRecord;
import com.organizerbot.model.GiftRecord.Gift;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class GiftService {
    private final GiftDao dao = new JsonGiftDao();
    private final Map<Long, GiftRecord> users = new HashMap<>();

    public GiftRecord getUser(Long id) {
        GiftRecord record = users.get(id);
        if (record == null) {
            record = dao.load(id);
            if (record == null) {
                record = new GiftRecord(id);
            }
            users.put(id, record);
        }
        return record;
    }

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

        double totalIfAdded = record.getTotalSpentFor(recipient) + gift.getPrice();
        double limit = record.getBudgetFor(recipient);
        String warning = "";
        if (totalIfAdded > limit) {
            warning = "⚠️ Превышен лимит бюджета для " + recipient + " (" + limit + "₽)!\n";
        }

        record.addGift(recipient, gift);
        dao.save(record);
        users.put(userId, record);

        return warning + "✅ Подарок добавлен для " + recipient + "!";
    }

    public String listGifts(Long userId) {
        GiftRecord record = refreshUser(userId);
        Map<String, List<Gift>> all = record.getAllGifts();

        if (all.isEmpty()) return "📭 Список подарков пуст.";

        StringBuilder sb = new StringBuilder("🎁 Подарки по получателям:\n");
        for (Map.Entry<String, List<Gift>> entry : all.entrySet()) {
            List<Gift> list = entry.getValue();
            if (list == null || list.isEmpty()) continue;

            sb.append("\n👤 ").append(entry.getKey()).append(":\n");
            int index = 1;
            for (Gift g : list) {
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

    public void updateDefaultBudget(Long userId, double budget) {
        GiftRecord record = getUser(userId);
        record.setDefaultBudget((int) budget);
        dao.save(record);
        users.put(userId, record);
    }

    public double getBudget(Long userId, String recipient) {
        return getUser(userId).getBudgetFor(recipient);
    }

    public Map<String, List<Gift>> getAllGifts(Long userId) {
        return refreshUser(userId).getAllGifts();
    }

    public boolean editGift(Long userId, String recipient, int index, Gift newGift) {
        GiftRecord record = getUser(userId);
        boolean result = record.editGift(recipient, index, newGift);
        if (result) {
            dao.save(record);
            users.put(userId, record);
        }
        return result;
    }

    public boolean deleteGift(Long userId, String recipient, int index) {
        GiftRecord record = getUser(userId);
        boolean result = record.removeGift(recipient, index);

        if (result) {
            if (record.getGiftsFor(recipient).isEmpty()) {
                record.getAllGifts().remove(recipient);
            }
            dao.save(record);
            users.put(userId, record);
        }

        return result;
    }

    public Map<Long, GiftRecord> getAllUserGiftRecords() {
        return users;
    }

    public void updateStatusesAutomatically() {
        for (Map.Entry<Long, GiftRecord> entry : users.entrySet()) {
            GiftRecord record = entry.getValue();
            boolean changed = false;

            for (List<Gift> gifts : record.getAllGifts().values()) {
                for (Gift gift : gifts) {
                    if (gift.getEventDate() != null &&
                            gift.getEventDate().isBefore(LocalDate.now()) &&
                            !"Завершено".equalsIgnoreCase(gift.getStatus())) {
                        gift.setStatus("Завершено");
                        changed = true;
                    }
                }
            }

            if (changed) {
                dao.save(record);
            }
        }
    }

    // ✅ NEW: Filter functionality
    public String filterGifts(Long userId, String status, String dateFilter, Double minPrice, Double maxPrice) {
        GiftRecord record = refreshUser(userId);
        LocalDate today = LocalDate.now();

        Map<String, List<Gift>> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, List<Gift>> entry : record.getAllGifts().entrySet()) {
            List<Gift> matched = entry.getValue().stream().filter(gift -> {
                boolean matches = true;
                if (status != null && !status.equalsIgnoreCase("all")) {
                    matches &= gift.getStatus() != null && gift.getStatus().equalsIgnoreCase(status);
                }
                if ("before".equalsIgnoreCase(dateFilter)) {
                    matches &= gift.getEventDate() != null && gift.getEventDate().isBefore(today);
                } else if ("after".equalsIgnoreCase(dateFilter)) {
                    matches &= gift.getEventDate() != null && gift.getEventDate().isAfter(today);
                } else if ("today".equalsIgnoreCase(dateFilter)) {
                    matches &= gift.getEventDate() != null && gift.getEventDate().isEqual(today);
                }
                if (minPrice != null) {
                    matches &= gift.getPrice() >= minPrice;
                }
                if (maxPrice != null) {
                    matches &= gift.getPrice() <= maxPrice;
                }
                return matches;
            }).collect(Collectors.toList());

            if (!matched.isEmpty()) {
                filtered.put(entry.getKey(), matched);
            }
        }

        if (filtered.isEmpty()) return "📭 Ничего не найдено по заданным фильтрам.";

        StringBuilder sb = new StringBuilder("🔍 Результаты поиска:\n");
        for (Map.Entry<String, List<Gift>> entry : filtered.entrySet()) {
            sb.append("\n👤 ").append(entry.getKey()).append(":\n");
            int index = 1;
            for (Gift g : entry.getValue()) {
                sb.append(index++).append(". ").append(g.toString()).append("\n");
            }
        }
        return sb.toString();
    }
}
