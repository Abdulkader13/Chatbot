package com.organizerbot;

import com.organizerbot.controller.BotController;
import com.organizerbot.service.ReminderService;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            BotController bot = new BotController();
            botsApi.registerBot(bot);

            // Запуск напоминаний
            ReminderService reminderService = new ReminderService(bot);
            List<Long> userIds = Arrays.asList(
                    5073905349L
            );

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> {
                System.out.println(" Проверка напоминаний...");
                reminderService.checkAllUsers(userIds);
            }, 0, 24, TimeUnit.HOURS);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

