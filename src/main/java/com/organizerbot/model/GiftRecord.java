package com.organizerbot.model;

import com.organizerbot.model.GiftRecord.Gift;

import java.time.LocalDate;
import java.util.*;

public class GiftRecord {
    private Long userId;
    private int defaultBudget = 0;
    private Map<String, Integer> individualBudgets;
    private Map<String, List<Gift>> gifts;
    private int remindBeforeDays = 3;

    public GiftRecord(Long userId) {
        this.userId = userId;
        this.individualBudgets = new HashMap<>();
        this.gifts = new HashMap<>();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getDefaultBudget() {
        return defaultBudget;
    }

    public void setDefaultBudget(int defaultBudget) {
        this.defaultBudget = defaultBudget;
    }

    public void setIndividualBudget(String recipient, double budget) {
        getIndividualBudgets().put(recipient, (int) budget);
    }

    public int getBudgetFor(String recipient) {
        return getIndividualBudgets().getOrDefault(recipient, defaultBudget);
    }

    public Map<String, List<Gift>> getAllGifts() {
        if (gifts == null) {
            gifts = new HashMap<>();
        }
        return gifts;
    }

    public void setAllGifts(Map<String, List<Gift>> gifts) {
        this.gifts = gifts;
    }

    public Map<String, Integer> getIndividualBudgets() {
        if (individualBudgets == null) {
            individualBudgets = new HashMap<>();
        }
        return individualBudgets;
    }

    public void setIndividualBudgets(Map<String, Integer> individualBudgets) {
        this.individualBudgets = individualBudgets;
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

    // Inner Gift class
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

        @Override
        public String toString() {
            return giftName + " — " + price + "₽ — " + eventDate +
                    (comment != null && !comment.isEmpty() ? " — " + comment : "");
        }
    }
}
