package com.tht.hatirlatik.model;

public enum RepeatType {
    NONE,       // Tekrar yok
    DAILY,      // Her gün
    WEEKLY,     // Her hafta
    MONTHLY,    // Her ay
    WEEKDAYS,   // Hafta içi (Pazartesi-Cuma)
    WEEKENDS,   // Hafta sonu (Cumartesi-Pazar)
    CUSTOM      // Özel tekrarlama (özel günler, saatler vb.)
} 