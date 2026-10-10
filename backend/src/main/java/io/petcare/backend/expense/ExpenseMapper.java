package io.petcare.backend.expense;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ExpenseMapper extends BaseMapper<ExpenseRecord> {

    @Select("""
            <script>
            SELECT e.id, e.pet_id AS petId,
                   CASE WHEN e.pet_id IS NULL THEN NULL
                        WHEN p.id IS NULL THEN '已移除宠物'
                        ELSE p.name END AS petName,
                   e.category, e.amount, e.expense_date AS expenseDate,
                   e.merchant, e.description, e.remark,
                   e.create_time AS createTime, e.update_time AS updateTime
            FROM expense_record e
            LEFT JOIN pet p ON p.id = e.pet_id AND p.user_id = e.user_id AND p.deleted = 0
            WHERE e.user_id = #{userId} AND e.deleted = 0
            <if test="petId != null">AND e.pet_id = #{petId}</if>
            <if test="category != null">AND e.category = #{category}</if>
            <if test="startDate != null">AND e.expense_date &gt;= #{startDate}</if>
            <if test="endDate != null">AND e.expense_date &lt;= #{endDate}</if>
            ORDER BY e.expense_date DESC, e.id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    @Results(id = "expenseViewResult", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "petId", column = "petId"),
            @Result(property = "petName", column = "petName"),
            @Result(property = "category", column = "category"),
            @Result(property = "amount", column = "amount"),
            @Result(property = "expenseDate", column = "expenseDate"),
            @Result(property = "merchant", column = "merchant"),
            @Result(property = "description", column = "description"),
            @Result(property = "remark", column = "remark"),
            @Result(property = "createTime", column = "createTime"),
            @Result(property = "updateTime", column = "updateTime")
    })
    List<ExpenseView> selectPage(
            @Param("userId") long userId,
            @Param("petId") Long petId,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("offset") long offset,
            @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM expense_record
            WHERE user_id = #{userId} AND deleted = 0
            <if test="petId != null">AND pet_id = #{petId}</if>
            <if test="category != null">AND category = #{category}</if>
            <if test="startDate != null">AND expense_date &gt;= #{startDate}</if>
            <if test="endDate != null">AND expense_date &lt;= #{endDate}</if>
            </script>
            """)
    long countPage(
            @Param("userId") long userId,
            @Param("petId") Long petId,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT e.id, e.pet_id AS petId,
                   CASE WHEN e.pet_id IS NULL THEN NULL
                        WHEN p.id IS NULL THEN '已移除宠物'
                        ELSE p.name END AS petName,
                   e.category, e.amount, e.expense_date AS expenseDate,
                   e.merchant, e.description, e.remark,
                   e.create_time AS createTime, e.update_time AS updateTime
            FROM expense_record e
            LEFT JOIN pet p ON p.id = e.pet_id AND p.user_id = e.user_id AND p.deleted = 0
            WHERE e.user_id = #{userId} AND e.id = #{expenseId} AND e.deleted = 0
            """)
    @org.apache.ibatis.annotations.ResultMap("expenseViewResult")
    ExpenseView selectOneByOwner(@Param("userId") long userId, @Param("expenseId") long expenseId);

    @Select("""
            <script>
            SELECT COALESCE(SUM(amount), 0.00) AS totalAmount, COUNT(*) AS recordCount
            FROM expense_record
            WHERE user_id = #{userId} AND deleted = 0
              AND expense_date &gt;= #{startDate} AND expense_date &lt; #{endDateExclusive}
            <if test="petId != null">AND pet_id = #{petId}</if>
            </script>
            """)
    ExpenseRangeTotal sumRange(
            @Param("userId") long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDateExclusive") LocalDate endDateExclusive,
            @Param("petId") Long petId);

    @Select("""
            <script>
            SELECT category, SUM(amount) AS amount
            FROM expense_record
            WHERE user_id = #{userId} AND deleted = 0
              AND expense_date &gt;= #{startDate} AND expense_date &lt;= #{endDate}
              <if test="petId != null">AND pet_id = #{petId}</if>
            GROUP BY category
            ORDER BY SUM(amount) DESC, category ASC
            </script>
            """)
    List<ExpenseCategoryTotal> sumByCategory(
            @Param("userId") long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("petId") Long petId);

    @Select("""
            <script>
            SELECT DATE_FORMAT(expense_date, '%Y-%m') AS month, SUM(amount) AS amount
            FROM expense_record
            WHERE user_id = #{userId} AND deleted = 0
              AND expense_date &gt;= #{startDate} AND expense_date &lt; #{endDateExclusive}
            <if test="petId != null">AND pet_id = #{petId}</if>
            GROUP BY DATE_FORMAT(expense_date, '%Y-%m')
            ORDER BY month ASC
            </script>
            """)
    List<ExpenseMonthTotal> sumByMonth(
            @Param("userId") long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDateExclusive") LocalDate endDateExclusive,
            @Param("petId") Long petId);

    @Select("""
            SELECT petId, petName, amount FROM (
                SELECT p.id AS petId, p.name AS petName, COALESCE(SUM(e.amount), 0.00) AS amount
                FROM pet p
                LEFT JOIN expense_record e
                  ON e.pet_id = p.id AND e.user_id = #{userId} AND e.deleted = 0
                WHERE p.user_id = #{userId} AND p.deleted = 0
                GROUP BY p.id, p.name
                UNION ALL
                SELECT NULL AS petId, '未指定宠物' AS petName, SUM(amount) AS amount
                FROM expense_record
                WHERE user_id = #{userId} AND deleted = 0 AND pet_id IS NULL
                HAVING COUNT(*) > 0
                UNION ALL
                SELECT NULL AS petId, '已移除宠物' AS petName, SUM(e.amount) AS amount
                FROM expense_record e
                LEFT JOIN pet p ON p.id = e.pet_id AND p.user_id = e.user_id AND p.deleted = 0
                WHERE e.user_id = #{userId} AND e.deleted = 0 AND e.pet_id IS NOT NULL AND p.id IS NULL
                HAVING COUNT(*) > 0
            ) totals
            ORDER BY amount DESC, petName ASC
            """)
    List<ExpensePetTotal> sumByPet(@Param("userId") long userId);
}
