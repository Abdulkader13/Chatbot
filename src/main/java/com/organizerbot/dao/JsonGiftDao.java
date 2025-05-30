package com.organizerbot.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.organizerbot.model.GiftRecord;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JsonGiftDao implements GiftDao {
    private static final String DIR = "src/main/resources/data/";
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(java.time.LocalDate.class, new LocalDateAdapter())
            .create();

    public JsonGiftDao() {
        File folder = new File(DIR);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Optional: Register a shutdown hook to save all cached users
        /*
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use GiftService.getAllUserGiftRecords() if needed
            System.out.println("🧾 Завершение работы. Все данные будут сохранены.");
        }));
        */
    }

    @Override
    public void save(GiftRecord record) {
        String path = DIR + record.getTelegramId() + ".json";
        try (Writer writer = new FileWriter(path)) {
            gson.toJson(record, writer);
            System.out.println("✅ Данные сохранены: " + path);
        } catch (IOException e) {
            System.err.println("❌ Ошибка при сохранении файла " + path + ": " + e.getMessage());
        }
    }

    @Override
    public GiftRecord load(Long telegramId) {
        String path = DIR + telegramId + ".json";
        File file = new File(path);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                GiftRecord record = gson.fromJson(reader, GiftRecord.class);
                System.out.println("📂 Загружены данные из файла: " + path);
                return record;
            } catch (IOException e) {
                System.err.println("❌ Ошибка при чтении файла " + path + ": " + e.getMessage());
            }
        } else {
            System.out.println("📄 Файл не найден, создаю новый GiftRecord для пользователя " + telegramId);
        }

        return new GiftRecord(telegramId);
    }
}
