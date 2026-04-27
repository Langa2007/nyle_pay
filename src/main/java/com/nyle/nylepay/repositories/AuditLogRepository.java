package com.nyle.nylepay.repositories;

import com.nyle.nylepay.models.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

       Page<AuditLog> findByUserId(Long userId, Pageable pageable);

       Page<AuditLog> findByEventType(String eventType, Pageable pageable);

       List<AuditLog> findByUserIdAndEventType(Long userId, String eventType);

       /** Count failed login attempts for a user since a given timestamp */
       @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userId = :userId " +
                     "AND a.eventType = 'AUTH_LOGIN_FAILED' AND a.timestamp >= :since")
       long countFailedLoginsSince(@Param("userId") Long userId,
                     @Param("since") LocalDateTime since);

       /** Count events from an IP address since a given timestamp */
       @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.ipAddress = :ip " +
                     "AND a.eventType = :eventType AND a.timestamp >= :since")
       long countByIpAndEventTypeSince(@Param("ip") String ip,
                     @Param("eventType") String eventType,
                     @Param("since") LocalDateTime since);

       /** Get recent fraud alerts for admin dashboard */
       @Query("SELECT a FROM AuditLog a WHERE a.eventType IN ('FRAUD_ALERT', 'FRAUD_BLOCKED') " +
                     "ORDER BY a.timestamp DESC")
       List<AuditLog> findRecentFraudAlerts(Pageable pageable);

       /** Find all events for a user within a date range (for compliance export) */
       List<AuditLog> findByUserIdAndTimestampBetween(Long userId,
                     LocalDateTime start,
                     LocalDateTime end);
}
