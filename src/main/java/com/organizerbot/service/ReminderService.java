package com.organizerbot.service;

import com.organizerbot.model.GiftRecord;
import com.organizerbot.model.GiftRecord.Gift;
import com.organizerbot.util.DateUtil;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReminderService {

    private final GiftService giftService = new GiftService();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private AbsSender bot;

    public void setBot(AbsSender bot) {
        this.bot = bot;
    }

    public void startReminderScheduler() {
        // ⏱ Run every 1 minute to allow precise reminder testing
        scheduler.scheduleAtFixedRate(this::checkReminders, 0, 1, TimeUnit.MINUTES);
    }

    private void checkReminders() {
        Map<Long, GiftRecord> allUsersData = giftService.getAllUserGiftRecords();
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<Long, GiftRecord> entry : allUsersData.entrySet()) {
            Long userId = entry.getKey();
            GiftRecord record = entry.getValue();

            int daysBefore = record.getRemindBeforeDays();
            int targetHour = record.getReminderHour();     // 🕐 New
            int targetMinute = record.getReminderMinute(); // 🕐 New

            LocalDate today = now.toLocalDate();
            LocalTime nowTime = now.toLocalTime();
            LocalTime userTime = LocalTime.of(targetHour, targetMinute);

            if (nowTime.getHour() == userTime.getHour() && nowTime.getMinute() == userTime.getMinute()) {
                for (Map.Entry<String, List<Gift>> recipientEntry : record.getAllGifts().entrySet()) {
                    String recipient = recipientEntry.getKey();

                    for (Gift gift : recipientEntry.getValue()) {
                        if (gift.getEventDate() == null) continue;

                        LocalDate giftDate = gift.getEventDate();
                        LocalDate reminderDate = giftDate.minusDays(daysBefore);

                        if (reminderDate.isEqual(today)) {
                            sendReminder(userId, recipient, gift);
                        }
                    }
                }
            }
        }
    }

    private void sendReminder(Long userId, String recipient, Gift gift) {
        if (bot == null) return;

        String text = String.format("🔔 Напоминание! Приближается дата подарка:\n\n" +
                        "🎁 Получатель: %s\n🎉 Подарок: %s\n💰 Цена: %.2f\n📅 Дата: %s\n📝 Комментарий: %s",
                recipient,
                gift.getGiftName(),
                gift.getPrice(),
                DateUtil.format(gift.getEventDate()),
                gift.getComment() != null ? gift.getComment() : "—"
        );

        SendMessage message = new SendMessage();
        message.setChatId(userId.toString());
        message.setText(text);

        try {
            bot.execute(message);
        } catch (Exception e) {
            System.err.println("Ошибка при отправке напоминания: " + e.getMessage());
        }
    }

    public void shutdownScheduler() {
        scheduler.shutdown();
    }
}
