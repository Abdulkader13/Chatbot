package com.organizerbot.dao;

public class DaoFactory {
    private static String currentType = "json"; // default

    // ✅ Old-style method (uses internal type)
    public static GiftDao getDao() {
        return getDao(currentType);
    }

    // ✅ New-style method (explicit type)
    public static GiftDao getDao(String type) {
        if ("json".equalsIgnoreCase(type)) {
            return new JsonGiftDao();
        } else if ("memory".equalsIgnoreCase(type)) {
            return new InMemoryGiftDao();
        } else {
            throw new IllegalArgumentException("❌ Unknown DAO type: " + type);
        }
    }

    public static void setDaoType(String type) {
        if ("json".equalsIgnoreCase(type) || "memory".equalsIgnoreCase(type)) {
            currentType = type;
        } else {
            throw new IllegalArgumentException("❌ Invalid DAO type: " + type);
        }
    }

    public static String getDaoType() {
        return currentType;
    }
}
