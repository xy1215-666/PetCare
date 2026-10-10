package io.petcare.backend.reminder;

import io.petcare.backend.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
public class UpcomingReminderController {

    private final UpcomingReminderService reminderService;

    public UpcomingReminderController(UpcomingReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping("/upcoming")
    public ApiResponse<List<UpcomingReminderVO>> upcoming(@AuthenticationPrincipal Jwt jwt) {
        long userId = Long.parseLong(jwt.getSubject());
        return ApiResponse.success(HttpStatus.OK.value(), "success", reminderService.list(userId));
    }
}
