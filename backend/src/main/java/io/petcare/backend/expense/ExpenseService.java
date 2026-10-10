package io.petcare.backend.expense;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.petcare.backend.common.BusinessDate;
import io.petcare.backend.common.exception.BadRequestException;
import io.petcare.backend.common.exception.NotFoundException;
import io.petcare.backend.pet.PetService;
import io.petcare.backend.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ExpenseService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999.99");
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_TREND_MONTHS = 24;

    private final ExpenseMapper expenseMapper;
    private final PetService petService;
    private final UserService userService;
    private final Clock clock;

    public ExpenseService(ExpenseMapper expenseMapper, PetService petService, UserService userService, Clock clock) {
        this.expenseMapper = expenseMapper;
        this.petService = petService;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional
    public ExpenseView create(long userId, ExpenseRequest request) {
        validateOwnerAndRequest(userId, request);
        ExpenseRecord record = new ExpenseRecord();
        record.setUserId(userId);
        record.setDeleted(0);
        apply(record, request);
        expenseMapper.insert(record);
        return requireOwnedExpense(userId, record.getId());
    }

    @Transactional(readOnly = true)
    public ExpensePageVO list(
            long userId,
            Long petId,
            String categoryText,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size) {
        validateCurrentUserAndPet(userId, petId);
        validateDateRange(startDate, endDate);
        if (page < 1 || page > 1_000_000) {
            throw new BadRequestException("page must be between 1 and 1000000");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and 100");
        }
        String category = parseCategory(categoryText);
        long total = expenseMapper.countPage(userId, petId, category, startDate, endDate);
        long offset = Math.multiplyExact((long) page - 1, size);
        List<ExpenseView> records = expenseMapper.selectPage(
                userId, petId, category, startDate, endDate, offset, size);
        long totalPages = total == 0 ? 0 : (total + size - 1) / size;
        return new ExpensePageVO(records, total, page, size, totalPages);
    }

    @Transactional(readOnly = true)
    public ExpenseView get(long userId, long expenseId) {
        userService.getCurrentUser(userId);
        return requireOwnedExpense(userId, expenseId);
    }

    @Transactional
    public ExpenseView update(long userId, long expenseId, ExpenseRequest request) {
        ExpenseRecord record = findEntity(userId, expenseId);
        validateOwnerAndRequest(userId, request);
        apply(record, request);
        expenseMapper.updateById(record);
        return requireOwnedExpense(userId, expenseId);
    }

    @Transactional
    public void delete(long userId, long expenseId) {
        int updated = expenseMapper.update(null, new UpdateWrapper<ExpenseRecord>()
                .eq("id", expenseId)
                .eq("user_id", userId)
                .eq("deleted", 0)
                .set("deleted", 1));
        if (updated != 1) {
            throw new NotFoundException("expense was not found");
        }
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryVO monthlySummary(long userId, String monthText, Long petId) {
        validateCurrentUserAndPet(userId, petId);
        YearMonth month = parseMonth(monthText);
        ExpenseRangeTotal current = expenseMapper.sumRange(
                userId, month.atDay(1), month.plusMonths(1).atDay(1), petId);
        YearMonth previousMonth = month.minusMonths(1);
        ExpenseRangeTotal previous = expenseMapper.sumRange(
                userId, previousMonth.atDay(1), month.atDay(1), petId);
        BigDecimal changeRate = previous.getTotalAmount().compareTo(BigDecimal.ZERO) == 0
                ? null
                : current.getTotalAmount().subtract(previous.getTotalAmount())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(previous.getTotalAmount(), 2, RoundingMode.HALF_UP);
        return new ExpenseSummaryVO(
                month.toString(), money(current.getTotalAmount()), current.getRecordCount(),
                money(previous.getTotalAmount()), changeRate);
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategoryVO> categoryStatistics(
            long userId, LocalDate startDate, LocalDate endDate, Long petId) {
        validateCurrentUserAndPet(userId, petId);
        requireDateRange(startDate, endDate);
        return expenseMapper.sumByCategory(userId, startDate, endDate, petId).stream()
                .map(total -> new ExpenseCategoryVO(total.getCategory(), money(total.getAmount())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExpenseMonthlyTrendVO> monthlyTrend(long userId, int months, Long petId) {
        validateCurrentUserAndPet(userId, petId);
        if (months < 1 || months > MAX_TREND_MONTHS) {
            throw new BadRequestException("months must be between 1 and 24");
        }
        YearMonth currentMonth = YearMonth.from(BusinessDate.today(clock));
        YearMonth firstMonth = currentMonth.minusMonths(months - 1L);
        Map<String, BigDecimal> amountsByMonth = new HashMap<>();
        for (ExpenseMonthTotal total : expenseMapper.sumByMonth(
                userId, firstMonth.atDay(1), currentMonth.plusMonths(1).atDay(1), petId)) {
            amountsByMonth.put(total.getMonth(), money(total.getAmount()));
        }
        List<ExpenseMonthlyTrendVO> trend = new ArrayList<>(months);
        for (int index = 0; index < months; index++) {
            YearMonth month = firstMonth.plusMonths(index);
            trend.add(new ExpenseMonthlyTrendVO(month.toString(), amountsByMonth.getOrDefault(month.toString(), ZERO)));
        }
        return List.copyOf(trend);
    }

    @Transactional(readOnly = true)
    public List<ExpensePetStatisticsVO> petStatistics(long userId) {
        userService.getCurrentUser(userId);
        return expenseMapper.sumByPet(userId).stream()
                .map(total -> new ExpensePetStatisticsVO(
                        total.getPetId(), total.getPetName(), money(total.getAmount())))
                .toList();
    }

    private void validateOwnerAndRequest(long userId, ExpenseRequest request) {
        validateCurrentUserAndPet(userId, request.petId());
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0
                || request.amount().compareTo(MAX_AMOUNT) > 0 || request.amount().scale() > 2) {
            throw new BadRequestException("amount must be between 0.01 and 999999.99 with at most 2 decimals");
        }
        if (request.expenseDate() == null || BusinessDate.isFuture(request.expenseDate(), clock)) {
            throw new BadRequestException("expenseDate cannot be in the future");
        }
    }

    private void validateCurrentUserAndPet(long userId, Long petId) {
        if (petId == null) {
            userService.getCurrentUser(userId);
            return;
        }
        if (petId <= 0) {
            throw new BadRequestException("petId must be positive");
        }
        petService.requireOwnedPet(userId, petId);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate cannot be after endDate");
        }
    }

    private void requireDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("startDate and endDate are required");
        }
        validateDateRange(startDate, endDate);
    }

    private String parseCategory(String categoryText) {
        if (categoryText == null || categoryText.isBlank()) {
            return null;
        }
        try {
            return ExpenseCategory.valueOf(categoryText.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("category is invalid");
        }
    }

    private YearMonth parseMonth(String monthText) {
        if (monthText == null || monthText.isBlank()) {
            throw new BadRequestException("month is required in yyyy-MM format");
        }
        try {
            YearMonth month = YearMonth.parse(monthText);
            if (!month.toString().equals(monthText)) {
                throw new BadRequestException("month must use yyyy-MM format");
            }
            return month;
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("month must use yyyy-MM format");
        }
    }

    private void apply(ExpenseRecord record, ExpenseRequest request) {
        record.setPetId(request.petId());
        record.setCategory(request.category().name());
        record.setAmount(request.amount().setScale(2, RoundingMode.UNNECESSARY));
        record.setExpenseDate(request.expenseDate());
        record.setMerchant(emptyToNull(request.merchant()));
        record.setDescription(emptyToNull(request.description()));
        record.setRemark(emptyToNull(request.remark()));
    }

    private ExpenseRecord findEntity(long userId, long expenseId) {
        ExpenseRecord record = expenseMapper.selectOne(new QueryWrapper<ExpenseRecord>()
                .eq("id", expenseId)
                .eq("user_id", userId)
                .eq("deleted", 0));
        if (record == null) {
            throw new NotFoundException("expense was not found");
        }
        return record;
    }

    private ExpenseView requireOwnedExpense(long userId, long expenseId) {
        ExpenseView record = expenseMapper.selectOneByOwner(userId, expenseId);
        if (record == null) {
            throw new NotFoundException("expense was not found");
        }
        return record;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal money(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }
}
