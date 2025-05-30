package com.organizerbot.controller;

import com.organizerbot.model.GiftRecord.Gift;
import com.organizerbot.service.GiftService;
import com.organizerbot.service.ReminderService;
import com.organizerbot.util.DateUtil;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

public class BotController extends TelegramLongPollingBot {
    private final GiftService service = new GiftService();
    private final EditHandler editHandler = new EditHandler();
    private final ReminderService reminderService = new ReminderService();

    public BotController() {
        reminderService.setBot(this);
        reminderService.startReminderScheduler();
    }

    @Override
    public String getBotUsername() {
        return "ОРГАНАЙЗЕР ПОДАРКОВ";
    }

    @Override
    public String getBotToken() {
        return "7686364592:AAHo3LeMoaZwUYsPhZZdasOZEm-642kcPDE";
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                String data = update.getCallbackQuery().getData();
                Long userId = update.getCallbackQuery().getMessage().getChatId();

                if (data.startsWith("edit_recipient:")) {
                    String recipient = data.substring("edit_recipient:".length());
                    execute(editHandler.buildGiftButtons(userId, recipient));
                    return;
                }

                if (data.startsWith("edit_gift:")) {
                    String[] parts = data.split(":");
                    if (parts.length == 3) {
                        String recipient = parts[1];
                        int index = Integer.parseInt(parts[2]);
                        execute(editHandler.buildEditFieldButtons(userId, recipient, index));
                        return;
                    }
                }

                if (data.startsWith("edit_field:")) {
                    String[] parts = data.split(":");
                    if (parts.length == 4) {
                        String field = parts[1];
                        String recipient = parts[2];
                        int index = Integer.parseInt(parts[3]);
                        editHandler.buildEditFieldButtons(userId, recipient, index);
                        sendMsg(userId, editHandler.handleGiftFieldSelection(userId, field));
                        return;
                    }
                }

                if (data.startsWith("delete_gift:")) {
                    String[] parts = data.split(":");
                    if (parts.length == 3) {
                        String recipient = parts[1];
                        int index = Integer.parseInt(parts[2]);
                        boolean success = service.deleteGift(userId, recipient, index);
                        if (success) {
                            sendMsg(userId, "🗑️ Подарок удалён.");
                            execute(editHandler.buildGiftButtons(userId, recipient));
                        } else {
                            sendMsg(userId, "❌ Не удалось удалить подарок.");
                        }
                        return;
                    }
                }

                if (data.equals("edit_cancel")) {
                    editHandler.clear(userId);
                    sendMsg(userId, "❌ Редактирование отменено.");
                    return;
                }
            }

            if (update.hasMessage() && update.getMessage().hasText()) {
                Message message = update.getMessage();
                String text = message.getText();
                Long userId = message.getChatId();

                if (message.getReplyToMessage() != null) {
                    String original = message.getReplyToMessage().getText();
                    if (original.contains("Введите сумму бюджета")) {
                        double amount = Double.parseDouble(text);
                        service.updateBudget(userId, "Общий", amount);
                        sendMsg(userId, "💰 Бюджет установлен: " + amount + "₽");
                        return;
                    } else if (original.contains("Введите количество дней")) {
                        int days = Integer.parseInt(text);
                        service.getUser(userId).setRemindBeforeDays(days);
                        sendMsg(userId, "🔔 Напоминания установлены: за " + days + " дней.");
                        return;
                    }
                }

                if (text.equals("✏️ Редактировать список")) {
                    execute(editHandler.buildRecipientButtons(userId));
                    return;
                }

                if (editHandler.awaitingFieldInput(userId)) {
                    sendMsg(userId, editHandler.handleGiftFieldInput(userId, text));
                    return;
                }

                if (text.equals("/start")) {
                    sendMsg(userId, "👋 Добро пожаловать в Gift Organizer Bot!\n" +
                            "Команды:\n" +
                            "/add — добавить подарок\n" +
                            "/list — список подарков\n" +
                            "/budget Имя 5000 — бюджет на получателя\n" +
                            "/reminddays 3 — напомнить за N дней\n\n" +
                            "Формат подарка:\nИмя - Подарок - Сумма - ГГГГ-ММ-ДД - Комментарий");
                } else if (text.equals("/list") || text.equals("📜 Список")) {
                    sendMsg(userId, service.listGifts(userId));
                } else if (text.equals("/add") || text.equals("➕ Добавить")) {
                    sendMsg(userId, "Введите подарок в формате:\nИмя - Подарок - Сумма - Дата - Комментарий");
                } else if (text.equals("💰 Бюджет")) {
                    sendForceReply(userId, "Введите сумму бюджета (например 5000):");
                } else if (text.equals("🔔 Напомнить за N дней")) {
                    sendForceReply(userId, "Введите количество дней до напоминания:");
                } else if (text.contains(" - ")) {
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
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (update.hasMessage()) {
                    sendMsg(update.getMessage().getChatId(), "⚠️ Ошибка: " + e.getMessage());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void sendMsg(Long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(getMainMenuKeyboard());
        execute(message);
    }

    private void sendForceReply(Long chatId, String prompt) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(prompt);
        message.setReplyMarkup(new ForceReplyKeyboard(true));
        execute(message);
    }

    private ReplyKeyboardMarkup getMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("➕ Добавить");
        row1.add("📜 Список");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("💰 Бюджет");
        row2.add("🔔 Напомнить за N дней");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("✏️ Редактировать список");

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        keyboard.setKeyboard(rows);
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        return keyboard;
    }
}
