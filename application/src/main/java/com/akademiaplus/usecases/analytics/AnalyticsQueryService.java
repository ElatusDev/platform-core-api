/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.analytics;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Encapsulates cross-domain native queries for analytics aggregation.
 * All queries enforce tenant isolation via {@code tenant_id} and soft-delete via {@code deleted_at IS NULL}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class AnalyticsQueryService {

    @PersistenceContext
    private EntityManager entityManager;

    public long countAdultStudents(Long tenantId) {
        return ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM adult_students WHERE tenant_id = :tid AND deleted_at IS NULL")
                .setParameter("tid", tenantId)
                .getSingleResult()).longValue();
    }

    public long countMinorStudents(Long tenantId) {
        return ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM minor_students WHERE tenant_id = :tid AND deleted_at IS NULL")
                .setParameter("tid", tenantId)
                .getSingleResult()).longValue();
    }

    public long countNewStudentsThisMonth(Long tenantId) {
        return ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM ("
                + "  SELECT adult_student_id FROM adult_students "
                + "  WHERE tenant_id = :tid AND deleted_at IS NULL "
                + "    AND YEAR(entry_date) = YEAR(CURRENT_DATE) AND MONTH(entry_date) = MONTH(CURRENT_DATE) "
                + "  UNION ALL "
                + "  SELECT minor_student_id FROM minor_students "
                + "  WHERE tenant_id = :tid AND deleted_at IS NULL "
                + "    AND YEAR(entry_date) = YEAR(CURRENT_DATE) AND MONTH(entry_date) = MONTH(CURRENT_DATE) "
                + ") t")
                .setParameter("tid", tenantId)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getEnrollmentTrend(Long tenantId) {
        return entityManager.createNativeQuery(
                "SELECT m.month_label, COALESCE(cnt, 0) FROM ("
                + "  SELECT DATE_FORMAT(DATE_SUB(CURRENT_DATE, INTERVAL n MONTH), '%Y-%m') AS month_label,"
                + "         DATE_FORMAT(DATE_SUB(CURRENT_DATE, INTERVAL n MONTH), '%Y-%m-01') AS month_start "
                + "  FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) nums"
                + ") m LEFT JOIN ("
                + "  SELECT DATE_FORMAT(entry_date, '%Y-%m') AS ym, COUNT(*) AS cnt FROM ("
                + "    SELECT entry_date FROM adult_students WHERE tenant_id = :tid AND deleted_at IS NULL "
                + "    UNION ALL "
                + "    SELECT entry_date FROM minor_students WHERE tenant_id = :tid AND deleted_at IS NULL "
                + "  ) all_students GROUP BY ym"
                + ") s ON m.month_label = s.ym ORDER BY m.month_label")
                .setParameter("tid", tenantId)
                .getResultList();
    }

    public long countEmployees(Long tenantId) {
        return ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM employees WHERE tenant_id = :tid AND deleted_at IS NULL")
                .setParameter("tid", tenantId)
                .getSingleResult()).longValue();
    }

    public long countTutors(Long tenantId) {
        return ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM tutors WHERE tenant_id = :tid AND deleted_at IS NULL")
                .setParameter("tid", tenantId)
                .getSingleResult()).longValue();
    }

    public long countCollaborators(Long tenantId) {
        return ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM collaborators WHERE tenant_id = :tid AND deleted_at IS NULL")
                .setParameter("tid", tenantId)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getEmployeeDistributionByRole(Long tenantId) {
        return entityManager.createNativeQuery(
                "SELECT employee_type, COUNT(*) FROM employees "
                + "WHERE tenant_id = :tid AND deleted_at IS NULL GROUP BY employee_type")
                .setParameter("tid", tenantId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getCourseEnrollments(Long tenantId) {
        return entityManager.createNativeQuery(
                "SELECT c.course_id, c.course_name, "
                + "  (SELECT COUNT(*) FROM adult_student_courses asc2 "
                + "   WHERE asc2.tenant_id = c.tenant_id AND asc2.course_id = c.course_id AND asc2.deleted_at IS NULL) + "
                + "  (SELECT COUNT(*) FROM minor_student_courses msc "
                + "   WHERE msc.tenant_id = c.tenant_id AND msc.course_id = c.course_id AND msc.deleted_at IS NULL) AS enrolled "
                + "FROM courses c WHERE c.tenant_id = :tid AND c.deleted_at IS NULL")
                .setParameter("tid", tenantId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getCourseCapacity(Long tenantId) {
        return entityManager.createNativeQuery(
                "SELECT c.course_id, c.course_name, c.max_capacity, "
                + "  (SELECT COUNT(*) FROM adult_student_courses asc2 "
                + "   WHERE asc2.tenant_id = c.tenant_id AND asc2.course_id = c.course_id AND asc2.deleted_at IS NULL) + "
                + "  (SELECT COUNT(*) FROM minor_student_courses msc "
                + "   WHERE msc.tenant_id = c.tenant_id AND msc.course_id = c.course_id AND msc.deleted_at IS NULL) AS enrolled "
                + "FROM courses c WHERE c.tenant_id = :tid AND c.deleted_at IS NULL")
                .setParameter("tid", tenantId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getScheduleByDayOfWeek(Long tenantId) {
        return entityManager.createNativeQuery(
                "SELECT schedule_day, COUNT(*) FROM schedules "
                + "WHERE tenant_id = :tid AND deleted_at IS NULL GROUP BY schedule_day")
                .setParameter("tid", tenantId)
                .getResultList();
    }

    public BigDecimal getRevenueMTD(Long tenantId) {
        Object result = entityManager.createNativeQuery(
                "SELECT COALESCE(SUM(amount), 0) FROM ("
                + "  SELECT amount FROM payment_adult_students "
                + "  WHERE tenant_id = :tid AND deleted_at IS NULL "
                + "    AND YEAR(payment_date) = YEAR(CURRENT_DATE) AND MONTH(payment_date) = MONTH(CURRENT_DATE) "
                + "  UNION ALL "
                + "  SELECT amount FROM payment_tutors "
                + "  WHERE tenant_id = :tid AND deleted_at IS NULL "
                + "    AND YEAR(payment_date) = YEAR(CURRENT_DATE) AND MONTH(payment_date) = MONTH(CURRENT_DATE) "
                + ") t")
                .setParameter("tid", tenantId)
                .getSingleResult();
        return result instanceof BigDecimal bd ? bd : new BigDecimal(result.toString());
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getRevenueTrend(Long tenantId) {
        return entityManager.createNativeQuery(
                "SELECT m.month_label, COALESCE(amt, 0) FROM ("
                + "  SELECT DATE_FORMAT(DATE_SUB(CURRENT_DATE, INTERVAL n MONTH), '%Y-%m') AS month_label "
                + "  FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) nums"
                + ") m LEFT JOIN ("
                + "  SELECT DATE_FORMAT(payment_date, '%Y-%m') AS ym, SUM(amount) AS amt FROM ("
                + "    SELECT payment_date, amount FROM payment_adult_students WHERE tenant_id = :tid AND deleted_at IS NULL "
                + "    UNION ALL "
                + "    SELECT payment_date, amount FROM payment_tutors WHERE tenant_id = :tid AND deleted_at IS NULL "
                + "  ) all_payments GROUP BY ym"
                + ") p ON m.month_label = p.ym ORDER BY m.month_label")
                .setParameter("tid", tenantId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getRevenueByType(Long tenantId) {
        return entityManager.createNativeQuery(
                "SELECT payment_method, SUM(amount) FROM ("
                + "  SELECT payment_method, amount FROM payment_adult_students WHERE tenant_id = :tid AND deleted_at IS NULL "
                + "  UNION ALL "
                + "  SELECT payment_method, amount FROM payment_tutors WHERE tenant_id = :tid AND deleted_at IS NULL "
                + ") t GROUP BY payment_method")
                .setParameter("tid", tenantId)
                .getResultList();
    }

    public BigDecimal getOutstandingPayments(Long tenantId) {
        Object result = entityManager.createNativeQuery(
                "SELECT COALESCE(SUM(m.fee), 0) FROM memberships m "
                + "INNER JOIN membership_adult_students mas "
                + "  ON m.tenant_id = mas.tenant_id AND m.membership_id = mas.membership_id "
                + "WHERE m.tenant_id = :tid AND m.deleted_at IS NULL AND mas.deleted_at IS NULL "
                + "  AND mas.due_date >= CURRENT_DATE "
                + "  AND NOT EXISTS ("
                + "    SELECT 1 FROM payment_adult_students pas "
                + "    WHERE pas.tenant_id = mas.tenant_id "
                + "      AND pas.membership_adult_student_id = mas.membership_adult_student_id "
                + "      AND pas.deleted_at IS NULL"
                + "  )")
                .setParameter("tid", tenantId)
                .getSingleResult();
        return result instanceof BigDecimal bd ? bd : new BigDecimal(result.toString());
    }

    public double getMembershipRenewalRate(Long tenantId) {
        Object result = entityManager.createNativeQuery(
                "SELECT CASE WHEN total_expired = 0 THEN 0.0 "
                + "  ELSE (renewed * 100.0 / total_expired) END FROM ("
                + "  SELECT "
                + "    (SELECT COUNT(*) FROM membership_adult_students "
                + "     WHERE tenant_id = :tid AND deleted_at IS NULL AND due_date < CURRENT_DATE) AS total_expired, "
                + "    (SELECT COUNT(DISTINCT mas2.adult_student_id) FROM membership_adult_students mas2 "
                + "     WHERE mas2.tenant_id = :tid AND mas2.deleted_at IS NULL "
                + "       AND mas2.start_date > (SELECT MIN(mas3.due_date) FROM membership_adult_students mas3 "
                + "         WHERE mas3.tenant_id = :tid AND mas3.adult_student_id = mas2.adult_student_id "
                + "           AND mas3.deleted_at IS NULL AND mas3.due_date < CURRENT_DATE)) AS renewed"
                + ") t")
                .setParameter("tid", tenantId)
                .getSingleResult();
        return ((Number) result).doubleValue();
    }

    public double getCourseUtilization(Long tenantId) {
        Object result = entityManager.createNativeQuery(
                "SELECT CASE WHEN total_capacity = 0 THEN 0.0 "
                + "  ELSE (total_enrolled * 100.0 / total_capacity) END FROM ("
                + "  SELECT "
                + "    COALESCE(SUM(c.max_capacity), 0) AS total_capacity, "
                + "    COALESCE(SUM("
                + "      (SELECT COUNT(*) FROM adult_student_courses asc2 "
                + "       WHERE asc2.tenant_id = c.tenant_id AND asc2.course_id = c.course_id AND asc2.deleted_at IS NULL) + "
                + "      (SELECT COUNT(*) FROM minor_student_courses msc "
                + "       WHERE msc.tenant_id = c.tenant_id AND msc.course_id = c.course_id AND msc.deleted_at IS NULL)"
                + "    ), 0) AS total_enrolled "
                + "  FROM courses c WHERE c.tenant_id = :tid AND c.deleted_at IS NULL"
                + ") t")
                .setParameter("tid", tenantId)
                .getSingleResult();
        return ((Number) result).doubleValue();
    }
}
