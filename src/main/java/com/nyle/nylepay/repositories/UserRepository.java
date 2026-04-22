// UserRepository.java
package com.nyle.nylepay.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nyle.nylepay.models.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMpesaNumber(String mpesaNumber);
    Optional<User> findByCryptoAddress(String cryptoAddress);
    Optional<User> findByKycReference(String kycReference);
}