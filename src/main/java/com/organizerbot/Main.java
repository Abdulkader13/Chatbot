package com.organizerbot;

import com.organizerbot.controller.BotController;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            BotController bot = new BotController();
            botsApi.registerBot(bot);  // will no longer crash due to webhook error
            System.out.println("✅ Bot is running.");
        } catch (Exception e) {
            System.err.println("❌ Failed to start bot:");
            e.printStackTrace();
        }
    }
}
