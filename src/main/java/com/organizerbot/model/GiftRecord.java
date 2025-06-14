package com.organizerbot.model;

import com.organizerbot.model.GiftRecord.Gift;

import java.time.LocalDate;
import java.util.*;

public class GiftRecord {
    private Long userId;
    private int defaultBudget = 0;
    private Map<String, Integer> individualBudgets = new HashMap<>();
    private Map<String, List<Gift>> gifts = new HashMap<>();
    private int remindBeforeDays = 3;
    private int reminderHour = 9;
    private int reminderMinute = 0;

    public GiftRecord(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public int getDefaultBudget() {
        return defaultBudget;
    }

    public void setDefaultBudget(int defaultBudget) {
        this.defaultBudget = defaultBudget;
    }

    public void setIndividualBudget(String recipient, double budget) {
        individualBudgets.put(recipient, (int) budget);
    }

    public int getBudgetFor(String recipient) {
        return individualBudgets.getOrDefault(recipient, defaultBudget);
    }

    public Map<String, List<Gift>> getAllGifts() {
        if (gifts == null) gifts = new HashMap<>();
        return gifts;
    }

    public List<Gift> getGiftsFor(String recipient) {
        return getAllGifts().computeIfAbsent(recipient, k -> new ArrayList<>());
    }

    public void addGift(String recipient, Gift gift) {
        getGiftsFor(recipient).add(gift);
    }

    public boolean editGift(String recipient, int index, Gift newGift) {
        List<Gift> list = getGiftsFor(recipient);
        if (index >= 0 && index < list.size()) {
            list.set(index, newGift);
            return true;
        }
        return false;
    }

    public boolean removeGift(String recipient, int index) {
        List<Gift> list = getGiftsFor(recipient);
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            return true;
        }
        return false;
    }

    public double getTotalSpentFor(String recipient) {
        return getGiftsFor(recipient).stream()
                .mapToDouble(Gift::getPrice)
                .sum();
    }

    public int getRemindBeforeDays() {
        return remindBeforeDays;
    }

    public void setRemindBeforeDays(int remindBeforeDays) {
        this.remindBeforeDays = remindBeforeDays;
    }

    public int getReminderHour() {
        return reminderHour;
    }

    public void setReminderHour(int reminderHour) {
        this.reminderHour = reminderHour;
    }

    public int getReminderMinute() {
        return reminderMinute;
    }

    public void setReminderMinute(int reminderMinute) {
        this.reminderMinute = reminderMinute;
    }

    public Map<String, Integer> getIndividualBudgets() {
        if (individualBudgets == null) individualBudgets = new HashMap<>();
        return individualBudgets;
    }

    public void setIndividualBudgets(Map<String, Integer> individualBudgets) {
        this.individualBudgets = individualBudgets;
    }

    // ✅ Inner class with status
    public static class Gift {
        private String giftName;
        private double price;
        private LocalDate eventDate;
        private String comment;
        private String status = "Запланирован";

        public Gift(String giftName, double price, LocalDate eventDate, String comment) {
            this.giftName = giftName;
            this.price = price;
            this.eventDate = eventDate;
            this.comment = comment;
            this.status = "Запланирован";
        }

        // ✅ New constructor to set status
        public Gift(String giftName, double price, LocalDate eventDate, String comment, String status) {
            this.giftName = giftName;
            this.price = price;
            this.eventDate = eventDate;
            this.comment = comment;
            this.status = status != null ? status : "Запланирован";
        }

        public String getGiftName() {
            return giftName;
        }

        public double getPrice() {
            return price;
        }

        public LocalDate getEventDate() {
            return eventDate;
        }

        public String getComment() {
            return comment;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return giftName + " — " + price + "₽ — " + eventDate +
                    (comment != null && !comment.isEmpty() ? " — " + comment : "") +
                    " — Статус: " + status;
        }
    }
}
