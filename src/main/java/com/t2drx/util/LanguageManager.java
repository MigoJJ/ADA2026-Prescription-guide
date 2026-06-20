package com.t2drx.util;

import java.util.Locale;
import java.util.ResourceBundle;

public class LanguageManager {
    private static LanguageManager instance;
    private ResourceBundle messages;
    private Locale currentLocale;

    private static final String BUNDLE_NAME = "messages";
    private static final Locale ENGLISH = new Locale("en", "US");
    private static final Locale KOREAN = new Locale("ko", "KR");

    private LanguageManager() {
        setLocale(ENGLISH);
    }

    public static LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    public void setLocale(Locale locale) {
        try {
            this.currentLocale = locale;
            this.messages = ResourceBundle.getBundle(BUNDLE_NAME, locale);
        } catch (Exception e) {
            System.err.println("Failed to load locale: " + locale + ". Defaulting to English.");
            this.currentLocale = ENGLISH;
            this.messages = ResourceBundle.getBundle(BUNDLE_NAME, ENGLISH);
        }
    }

    public String getString(String key) {
        try {
            return messages.getString(key);
        } catch (Exception e) {
            System.err.println("Missing translation key: " + key);
            return key;
        }
    }

    public Locale getCurrentLocale() {
        return currentLocale;
    }

    public void setEnglish() {
        setLocale(ENGLISH);
    }

    public void setKorean() {
        setLocale(KOREAN);
    }

    public boolean isKorean() {
        return currentLocale.getLanguage().equals("ko");
    }

    public boolean isEnglish() {
        return currentLocale.getLanguage().equals("en");
    }
}
