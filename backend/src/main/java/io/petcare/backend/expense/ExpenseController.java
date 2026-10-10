package io.petcare.backend.expense;

import io.petcare.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseView>> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ExpenseRequest request) {
        ExpenseView expense = expenseService.create(userId(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "created", expense));
    }

    @GetMapping
    public ApiResponse<ExpensePageVO> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(HttpStatus.OK.value(), "success",
                expenseService.list(userId(jwt), petId, category, startDate, endDate, page, size));
    }

    @GetMapping("/summary/monthly")
    public ApiResponse<ExpenseSummaryVO> monthlySummary(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String month,
            @RequestParam(required = false) Long petId) {
        return ApiResponse.success(HttpStatus.OK.value(), "success",
                expenseService.monthlySummary(userId(jwt), month, petId));
    }

    @GetMapping("/statistics/categories")
    public ApiResponse<List<ExpenseCategoryVO>> categoryStatistics(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long petId) {
        return ApiResponse.success(HttpStatus.OK.value(), "success",
                expenseService.categoryStatistics(userId(jwt), startDate, endDate, petId));
    }

    @GetMapping("/statistics/monthly-trend")
    public ApiResponse<List<ExpenseMonthlyTrendVO>> monthlyTrend(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(required = false) Long petId) {
        return ApiResponse.success(HttpStatus.OK.value(), "success",
                expenseService.monthlyTrend(userId(jwt), months, petId));
    }

    @GetMapping("/statistics/by-pet")
    public ApiResponse<List<ExpensePetStatisticsVO>> petStatistics(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(HttpStatus.OK.value(), "success", expenseService.petStatistics(userId(jwt)));
    }

    @GetMapping("/{expenseId}")
    public ApiResponse<ExpenseView> get(@AuthenticationPrincipal Jwt jwt, @PathVariable long expenseId) {
        return ApiResponse.success(HttpStatus.OK.value(), "success", expenseService.get(userId(jwt), expenseId));
    }

    @PutMapping("/{expenseId}")
    public ApiResponse<ExpenseView> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long expenseId,
            @Valid @RequestBody ExpenseRequest request) {
        return ApiResponse.success(HttpStatus.OK.value(), "updated",
                expenseService.update(userId(jwt), expenseId, request));
    }

    @DeleteMapping("/{expenseId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long expenseId) {
        expenseService.delete(userId(jwt), expenseId);
        return ApiResponse.success(HttpStatus.OK.value(), "deleted", null);
    }

    private long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
