package com.organizerbot.service;

import com.organizerbot.dao.GiftDao;
import com.organizerbot.dao.JsonGiftDao;
import com.organizerbot.model.GiftRecord;
import com.organizerbot.model.GiftRecord.Gift;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ReminderService {
    private final GiftDao dao = new JsonGiftDao();
    private final AbsSender bot;

    public ReminderService(AbsSender bot) {
        this.bot = bot;
    }

    public void checkAllUsers(List<Long> userIds) {
        for (Long userId : userIds) {
            GiftRecord record = dao.load(userId);
            if (!record.isRemindersEnabled()) continue;

            int daysBefore = record.getRemindBeforeDays();
            LocalDate targetDate = LocalDate.now().plusDays(daysBefore);
            StringBuilder reminder = new StringBuilder();

            for (Map.Entry<String, List<Gift>> entry : record.getAllGifts().entrySet()) {
                String name = entry.getKey();
                for (Gift g : entry.getValue()) {
                    if (g.getEventDate().isEqual(targetDate)) {
                        reminder.append("📌 Напоминание: ").append(name)
                                .append(" — ").append(g.getGiftName())
                                .append(" (").append(g.getEventDate()).append(")").append("\n");
                    }
                }
            }

            if (reminder.length() > 0) {
                SendMessage msg = new SendMessage();
                msg.setChatId(userId.toString());
                msg.setText(reminder.toString());

                try {
                    bot.execute(msg);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            }
        }
    }

