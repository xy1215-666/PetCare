package io.petcare.backend.reminder;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UpcomingReminderMapper {

    @Select("""
            SELECT reminder.pet_id, reminder.pet_name, reminder.reminder_type, reminder.title, reminder.due_date
            FROM (
                SELECT p.id AS pet_id, p.name AS pet_name, 'VACCINATION' AS reminder_type,
                       v.vaccine_name AS title, v.next_date AS due_date
                FROM vaccination_record v
                JOIN pet p ON p.id = v.pet_id
                WHERE p.user_id = #{userId} AND p.deleted = 0 AND v.deleted = 0
                  AND v.next_date >= #{fromDate} AND v.next_date <= #{toDate}
                UNION ALL
                SELECT p.id AS pet_id, p.name AS pet_name, 'DEWORMING' AS reminder_type,
                       CASE d.type
                           WHEN 'INTERNAL' THEN '体内驱虫'
                           WHEN 'EXTERNAL' THEN '体外驱虫'
                           WHEN 'BOTH' THEN '体内外驱虫'
                       END AS title,
                       d.next_date AS due_date
                FROM deworming_record d
                JOIN pet p ON p.id = d.pet_id
                WHERE p.user_id = #{userId} AND p.deleted = 0 AND d.deleted = 0
                  AND d.next_date >= #{fromDate} AND d.next_date <= #{toDate}
                UNION ALL
                SELECT p.id AS pet_id, p.name AS pet_name, 'MEDICAL_REVISIT' AS reminder_type,
                       CONCAT(COALESCE(NULLIF(TRIM(m.hospital), ''), m.reason), '复诊') AS title,
                       m.next_visit_date AS due_date
                FROM medical_record m
                JOIN pet p ON p.id = m.pet_id
                WHERE p.user_id = #{userId} AND p.deleted = 0 AND m.deleted = 0
                  AND m.next_visit_date >= #{fromDate} AND m.next_visit_date <= #{toDate}
            ) reminder
            ORDER BY reminder.due_date ASC, reminder.pet_name ASC,
                     reminder.reminder_type ASC, reminder.title ASC
            LIMIT 100
            """)
    @Results(id = "upcomingReminderRow", value = {
            @Result(property = "petId", column = "pet_id", id = true),
            @Result(property = "petName", column = "pet_name"),
            @Result(property = "reminderType", column = "reminder_type"),
            @Result(property = "title", column = "title"),
            @Result(property = "dueDate", column = "due_date")
    })
    List<UpcomingReminderRow> findUpcoming(
            @Param("userId") long userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
