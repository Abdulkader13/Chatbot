package com.organizerbot.controller;


import com.organizerbot.model.GiftRecord.Gift;
import com.organizerbot.service.GiftService;
import com.organizerbot.util.DateUtil;

import java.util.*;

public class EditHandler {
    private final GiftService service = new GiftService();

    private final Map<Long, String> awaitingRecipient = new HashMap<>();
    private final Map<Long, Integer> awaitingGiftIndex = new HashMap<>();
    private final Set<Long> awaitingNewGift = new HashSet<>();

    public boolean isEditing(Long userId) {
        return awaitingRecipient.containsKey(userId) || awaitingNewGift.contains(userId);
    }

    public String startRecipientSelection(Long userId) {
        Map<String, List<Gift>> all = service.getAllGifts(userId);
        if (all.isEmpty()) {
            return "📭 Нет получателей с подарками для редактирования.";
        }
        StringBuilder sb = new StringBuilder("👤 Выберите получателя:\n");
        for (String name : all.keySet()) {
            sb.append("• ").append(name).append("\n");
        }
        sb.append("\n✏️ Ответьте с именем получателя (в точности как в списке):");
        return sb.toString();
    }

    public String handleRecipientInput(Long userId, String name) {
        Map<String, List<Gift>> all = service.getAllGifts(userId);
        if (!all.containsKey(name)) {
            return "❌ Такого получателя нет. Попробуйте ещё раз.";
        }

        List<Gift> gifts = all.get(name);
        if (gifts.isEmpty()) {
            return "📭 У этого получателя пока нет подарков.";
        }

        awaitingRecipient.put(userId, name);
        StringBuilder sb = new StringBuilder("🎁 Подарки для ").append(name).append(":\n");
        for (int i = 0; i < gifts.size(); i++) {
            sb.append(i + 1).append(". ").append(gifts.get(i).toString()).append("\n");
        }
        sb.append("\n✏️ Введите номер подарка, который хотите отредактировать:");
        return sb.toString();
    }

    public String handleGiftIndexInput(Long userId, String input) {
        try {
            int index = Integer.parseInt(input.trim()) - 1;
            String name = awaitingRecipient.get(userId);
            List<Gift> gifts = service.getAllGifts(userId).get(name);

            if (index < 0 || index >= gifts.size()) {
                return "❌ Неверный номер. Попробуйте ещё раз.";
            }

            awaitingGiftIndex.put(userId, index);
            awaitingNewGift.add(userId);
            return "✍️ Введите новый подарок в формате:\nПодарок - Сумма - Дата - Комментарий";

        } catch (Exception e) {
            return "❌ Введите корректный номер.";
        }
    }

    public String handleGiftEditInput(Long userId, String input) {
        if (!awaitingRecipient.containsKey(userId) || !awaitingGiftIndex.containsKey(userId)) {
            return "⚠️ Сначала выберите получателя и номер.";
        }

        String[] parts = input.split(" - ");
        if (parts.length < 4) {
            return "❌ Формат: Подарок - Сумма - Дата - Комментарий";
        }

        try {
            String recipient = awaitingRecipient.get(userId);
            int index = awaitingGiftIndex.get(userId);
            Gift newGift = new Gift(
                    parts[0].trim(),
                    Double.parseDouble(parts[1].trim()),
                    DateUtil.parse(parts[2].trim()),
                    parts[3].trim()
            );

            boolean result = service.editGift(userId, recipient, index, newGift);
            clear(userId);
            return result ? "✅ Подарок успешно обновлён!" : "⚠️ Не удалось обновить.";
        } catch (Exception e) {
            return "❌ Ошибка: " + e.getMessage();
        }
    }

    private void clear(Long userId) {
        awaitingRecipient.remove(userId);
        awaitingGiftIndex.remove(userId);
        awaitingNewGift.remove(userId);
    }

    public boolean awaitingRecipientOnly(Long userId) {
        return awaitingRecipient.containsKey(userId) && !awaitingGiftIndex.containsKey(userId);
    }

    public boolean awaitingFinalGift(Long userId) {
        return awaitingNewGift.contains(userId);
    }
}
