package com.nyle.nylepay.repositories;

import com.nyle.nylepay.models.UserBankDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBankDetailRepository extends JpaRepository<UserBankDetail, Long> {
    List<UserBankDetail> findByUserId(Long userId);
    Optional<UserBankDetail> findByUserIdAndAccountNumber(Long userId, String accountNumber);
}
