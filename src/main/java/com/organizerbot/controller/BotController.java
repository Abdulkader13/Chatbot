package com.organizerbot.controller;

import com.organizerbot.model.GiftRecord.Gift;
import com.organizerbot.service.GiftService;
import com.organizerbot.util.DateUtil;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

public class BotController extends TelegramLongPollingBot {
    private final GiftService service = new GiftService();

    @Override
    public String getBotUsername() {
        return "GiftChatBot";
    }

    @Override
    public String getBotToken() {
        return "7686364592:AAHo3LeMoaZwUYsPhZZdasOZEm-642kcPDE";
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                Message message = update.getMessage();
                Long userId = message.getChatId();
                String text = message.getText();

                switch (text) {
                    case "/start":
                        sendMsg(userId, "👋 Добро пожаловать в Gift ChatBot!\n" +
                                "Вы можете добавлять подарки и просматривать список.\n" +
                                "Формат добавления:\nИмя - Подарок - Сумма - Дата - Комментарий");
                        break;
                    case "📜 Список":
                        sendMsg(userId, service.listGifts(userId));
                        break;
                    case "➕ Добавить":
                        sendMsg(userId, "Введите данные в формате:\nИмя - Подарок - Сумма - Дата - Комментарий");
                        break;
                    default:
                        if (text.contains(" - ")) {
                            String[] parts = text.split(" - ");
                            if (parts.length >= 5) {
                                String recipient = parts[0].trim();
                                Gift gift = new Gift(
                                        parts[1].trim(),
                                        Double.parseDouble(parts[2].trim()),
                                        DateUtil.parse(parts[3].trim()),
                                        parts[4].trim()
                                );
                                String response = service.addGift(userId, recipient, gift);
                                sendMsg(userId, response);
                            } else {
                                sendMsg(userId, "❌ Неверный формат. Пример: Имя - Подарок - Сумма - Дата - Комментарий");
                            }
                        } else {
                            sendMsg(userId, "⚠️ Неизвестная команда. Используйте /start.");
                        }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMsg(Long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(getMainMenuKeyboard());
        execute(message);
    }

    private ReplyKeyboardMarkup getMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("➕ Добавить");
        row1.add("📜 Список");

        rows.add(row1);

        keyboard.setKeyboard(rows);
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        return keyboard;
    }
}
