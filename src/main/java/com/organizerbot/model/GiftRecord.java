package com.organizerbot.model;

import java.time.LocalDate;
import java.util.*;

public class GiftRecord {

    public static class Gift {
        private String giftName;
        private double price;
        private LocalDate eventDate;
        private String comment;

        public Gift(String giftName, double price, LocalDate eventDate, String comment) {
            this.giftName = giftName;
            this.price = price;
            this.eventDate = eventDate;
            this.comment = comment;
        }

        public String getGiftName() { return giftName; }
        public double getPrice() { return price; }
        public LocalDate getEventDate() { return eventDate; }
        public String getComment() { return comment; }

        public void setGiftName(String name) { this.giftName = name; }
        public void setPrice(double price) { this.price = price; }
        public void setEventDate(LocalDate date) { this.eventDate = date; }
        public void setComment(String comment) { this.comment = comment; }

        // Aliases for other services
        public String getName() { return getGiftName(); }
        public LocalDate getDate() { return getEventDate(); }
        public void setAmount(double amount) { this.price = amount; }

        @Override
        public String toString() {
            return giftName + " (" + price + "₽, " + eventDate + ") " +
                    (comment != null && !comment.isEmpty() ? "// " + comment : "");
        }
    }

    private Long telegramId;
    private boolean remindersEnabled = true;
    private int remindBeforeDays = 7;
    private double defaultBudget = 10000;
    private Map<String, List<Gift>> giftsByRecipient = new HashMap<>();
    private Map<String, Double> individualBudgets = new HashMap<>();

    public GiftRecord(Long telegramId) {
        this.telegramId = telegramId;
    }

    public Long getTelegramId() { return telegramId; }

    public int getRemindBeforeDays() { return remindBeforeDays; }
    public void setRemindBeforeDays(int days) { this.remindBeforeDays = days; }

    public boolean isRemindersEnabled() { return remindersEnabled; }
    public void setRemindersEnabled(boolean enabled) { this.remindersEnabled = enabled; }

    public void addGift(String recipient, Gift gift) {
        giftsByRecipient.computeIfAbsent(recipient, r -> new ArrayList<>()).add(gift);
    }

    public List<Gift> getGiftsFor(String recipient) {
        return giftsByRecipient.getOrDefault(recipient, new ArrayList<>());
    }

    public Map<String, List<Gift>> getAllGifts() {
        return giftsByRecipient;
    }

    public boolean removeGift(String recipient, int index) {
        List<Gift> list = getGiftsFor(recipient);
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            return true;
        }
        return false;
    }

    public boolean editGift(String recipient, int index, Gift newGift) {
        List<Gift> list = getGiftsFor(recipient);
        if (index >= 0 && index < list.size()) {
            list.set(index, newGift);
            return true;
        }
        return false;
    }

    public double getBudgetFor(String recipient) {
        return individualBudgets.getOrDefault(recipient, defaultBudget);
    }

    public void setIndividualBudget(String recipient, double budget) {
        individualBudgets.put(recipient, budget);
    }

    public double getTotalSpentFor(String recipient) {
        return getGiftsFor(recipient).stream().mapToDouble(Gift::getPrice).sum();
    }

    public Map<String, List<Gift>> filterGiftsByMonth(int year, int month) {
        Map<String, List<Gift>> result = new HashMap<>();
        for (Map.Entry<String, List<Gift>> entry : giftsByRecipient.entrySet()) {
            List<Gift> filtered = new ArrayList<>();
            for (Gift g : entry.getValue()) {
                if (g.getEventDate().getYear() == year && g.getEventDate().getMonthValue() == month) {
                    filtered.add(g);
                }
            }
            if (!filtered.isEmpty()) result.put(entry.getKey(), filtered);
        }
        return result;
    }
}
