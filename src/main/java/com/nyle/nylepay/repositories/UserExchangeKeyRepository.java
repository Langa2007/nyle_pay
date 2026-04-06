package com.nyle.nylepay.repositories;

import com.nyle.nylepay.models.UserExchangeKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserExchangeKeyRepository extends JpaRepository<UserExchangeKey, Long> {
    List<UserExchangeKey> findByUserId(Long userId);
    Optional<UserExchangeKey> findByUserIdAndExchangeName(Long userId, String exchangeName);
}
