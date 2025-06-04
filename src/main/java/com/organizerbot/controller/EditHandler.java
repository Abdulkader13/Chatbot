package com.organizerbot.controller;

import com.organizerbot.model.GiftRecord.Gift;
import com.organizerbot.service.GiftService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

public class EditHandler {
    private final GiftService service = new GiftService();

    private final Map<Long, String> awaitingRecipient = new HashMap<>();
    private final Map<Long, Integer> awaitingGiftIndex = new HashMap<>();
    private final Set<Long> awaitingNewGift = new HashSet<>();
    private final Map<Long, String> awaitingField = new HashMap<>();
    private final Map<Long, String> awaitingBudgetRecipient = new HashMap<>();

    public boolean isEditing(Long userId) {
        return awaitingRecipient.containsKey(userId) || awaitingNewGift.contains(userId);
    }

    public SendMessage buildRecipientButtons(Long userId) {
        Map<String, List<Gift>> all = service.getAllGifts(userId);
        SendMessage msg = new SendMessage();
        msg.setChatId(userId.toString());

        if (all.isEmpty()) {
            msg.setText("📭 Нет получателей с подарками.");
            return msg;
        }

        msg.setText("👤 Выберите получателя:");
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (String recipient : all.keySet()) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(recipient);
            button.setCallbackData("edit_recipient:" + recipient);
            rows.add(Collections.singletonList(button));
        }

        msg.setReplyMarkup(new InlineKeyboardMarkup(rows));
        return msg;
    }

    public SendMessage buildGiftButtons(Long userId, String recipient) {
        List<Gift> gifts = service.getAllGifts(userId).get(recipient);
        SendMessage msg = new SendMessage();
        msg.setChatId(userId.toString());

        if (gifts == null || gifts.isEmpty()) {
            msg.setText("📭 У этого получателя нет подарков.");
            return msg;
        }

        msg.setText("🎁 Подарки для " + recipient + ":");
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < gifts.size(); i++) {
            Gift gift = gifts.get(i);

            InlineKeyboardButton editButton = new InlineKeyboardButton("✏️ " + gift.getGiftName());
            editButton.setCallbackData("edit_gift:" + recipient + ":" + i);

            InlineKeyboardButton deleteButton = new InlineKeyboardButton("🗑️");
            deleteButton.setCallbackData("delete_gift:" + recipient + ":" + i);

            rows.add(Arrays.asList(editButton, deleteButton));
        }

        msg.setReplyMarkup(new InlineKeyboardMarkup(rows));
        return msg;
    }

    public SendMessage buildEditFieldButtons(Long userId, String recipient, int index) {
        awaitingRecipient.put(userId, recipient);
        awaitingGiftIndex.put(userId, index);

        SendMessage msg = new SendMessage();
        msg.setChatId(userId.toString());
        msg.setText("✏️ Что вы хотите отредактировать?");

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(Collections.singletonList(createButton("🎁 Название подарка", "edit_field:name:" + recipient + ":" + index)));
        rows.add(Collections.singletonList(createButton("💰 Сумма", "edit_field:amount:" + recipient + ":" + index)));
        rows.add(Collections.singletonList(createButton("💬 Комментарий", "edit_field:comment:" + recipient + ":" + index)));
        rows.add(Collections.singletonList(createButton("📊 Бюджет", "edit_budget:" + recipient)));
        rows.add(Collections.singletonList(createButton("✏️ Статус", "edit_field:status:" + recipient + ":" + index)));
        rows.add(Collections.singletonList(createButton("❌ Отмена", "edit_cancel")));

        msg.setReplyMarkup(new InlineKeyboardMarkup(rows));
        return msg;
    }

    private InlineKeyboardButton createButton(String text, String data) {
        InlineKeyboardButton button = new InlineKeyboardButton(text);
        button.setCallbackData(data);
        return button;
    }

    public String handleGiftFieldSelection(Long userId, String field) {
        awaitingField.put(userId, field);
        switch (field) {
            case "name":
                return "✏️ Введите новое название подарка:";
            case "amount":
                return "💰 Введите новую сумму:";
            case "comment":
                return "💬 Введите новый комментарий:";
            case "status":
                return "📌 Введите новый статус (Запланирован / В процессе / Завершено):";
            default:
                return "⚠️ Неизвестное поле.";
        }
    }

    public String handleGiftFieldInput(Long userId, String input) {
        if (!awaitingRecipient.containsKey(userId) || !awaitingGiftIndex.containsKey(userId) || !awaitingField.containsKey(userId)) {
            return "⚠️ Сначала выберите поле для редактирования.";
        }

        String recipient = awaitingRecipient.get(userId);
        int index = awaitingGiftIndex.get(userId);
        String field = awaitingField.get(userId);
        Gift oldGift = service.getAllGifts(userId).get(recipient).get(index);

        Gift newGift;
        try {
            switch (field) {
                case "name":
                    newGift = new Gift(input.trim(), oldGift.getPrice(), oldGift.getEventDate(), oldGift.getComment(), oldGift.getStatus());
                    break;
                case "amount":
                    newGift = new Gift(oldGift.getGiftName(), Double.parseDouble(input.trim()), oldGift.getEventDate(), oldGift.getComment(), oldGift.getStatus());
                    break;
                case "comment":
                    newGift = new Gift(oldGift.getGiftName(), oldGift.getPrice(), oldGift.getEventDate(), input.trim(), oldGift.getStatus());
                    break;
                case "status":
                    String newStatus = input.trim();
                    if (!newStatus.equalsIgnoreCase("Запланирован") &&
                            !newStatus.equalsIgnoreCase("В процессе") &&
                            !newStatus.equalsIgnoreCase("Завершено")) {
                        return "❌ Недопустимый статус. Используйте: Запланирован, В процессе или Завершено.";
                    }
                    newGift = new Gift(oldGift.getGiftName(), oldGift.getPrice(), oldGift.getEventDate(), oldGift.getComment(), newStatus);
                    break;
                default:
                    return "⚠️ Неизвестное поле.";
            }

            boolean result = service.editGift(userId, recipient, index, newGift);
            clear(userId);
            return result ? "✅ Подарок обновлён!" : "⚠️ Не удалось обновить.";
        } catch (Exception e) {
            return "❌ Ошибка: " + e.getMessage();
        }
    }

    public boolean isAwaitingBudgetInput(Long userId) {
        return awaitingBudgetRecipient.containsKey(userId);
    }

    public String handleBudgetInput(Long userId, String input) {
        try {
            double newBudget = Double.parseDouble(input);
            String recipient = awaitingBudgetRecipient.remove(userId);
            service.updateBudget(userId, recipient, newBudget);
            return "📊 Бюджет обновлён для " + recipient + ": " + newBudget + "₽";
        } catch (Exception e) {
            return "❌ Ошибка при вводе бюджета: " + e.getMessage();
        }
    }

    public void startBudgetEdit(Long userId, String recipient) {
        awaitingBudgetRecipient.put(userId, recipient);
    }

    public String handleGiftDeleteCallback(Long userId, String recipient, int index) {
        boolean result = service.deleteGift(userId, recipient, index);
        return result ? "🗑️ Подарок удалён." : "⚠️ Не удалось удалить подарок.";
    }

    public void clear(Long userId) {
        awaitingRecipient.remove(userId);
        awaitingGiftIndex.remove(userId);
        awaitingNewGift.remove(userId);
        awaitingField.remove(userId);
    }

    public boolean awaitingRecipientOnly(Long userId) {
        return awaitingRecipient.containsKey(userId) && !awaitingGiftIndex.containsKey(userId);
    }

    public boolean awaitingFinalGift(Long userId) {
        return awaitingNewGift.contains(userId);
    }

    public boolean awaitingFieldInput(Long userId) {
        return awaitingField.containsKey(userId);
    }
}
