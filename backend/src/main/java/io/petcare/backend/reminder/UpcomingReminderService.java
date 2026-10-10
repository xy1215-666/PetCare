package io.petcare.backend.reminder;

import io.petcare.backend.common.BusinessDate;
import io.petcare.backend.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class UpcomingReminderService {

    private static final long WINDOW_DAYS = 30;
    private final UpcomingReminderMapper reminderMapper;
    private final UserService userService;
    private final Clock clock;

    public UpcomingReminderService(UpcomingReminderMapper reminderMapper, UserService userService, Clock clock) {
        this.reminderMapper = reminderMapper;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<UpcomingReminderVO> list(long userId) {
        userService.getCurrentUser(userId);
        LocalDate today = BusinessDate.today(clock);
        return reminderMapper.findUpcoming(userId, today, today.plusDays(WINDOW_DAYS)).stream()
                .map(row -> new UpcomingReminderVO(
                        row.getPetId(),
                        row.getPetName(),
                        ReminderType.valueOf(row.getReminderType()),
                        row.getTitle(),
                        row.getDueDate(),
                        Math.toIntExact(ChronoUnit.DAYS.between(today, row.getDueDate()))))
                .toList();
    }
}
