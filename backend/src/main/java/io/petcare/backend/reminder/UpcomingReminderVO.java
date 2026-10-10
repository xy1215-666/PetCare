package io.petcare.backend.reminder;

import java.time.LocalDate;

public record UpcomingReminderVO(
        Long petId,
        String petName,
        ReminderType type,
        String title,
        LocalDate dueDate,
        int daysRemaining) {
}
