package com.nyle.nylepay.repositories;

import com.nyle.nylepay.models.CryptoWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CryptoWalletRepository extends JpaRepository<CryptoWallet, Long> {

    Optional<CryptoWallet> findByUserIdAndChain(Long userId, String chain);

    List<CryptoWallet> findByUserId(Long userId);

    /** Used by the on-chain deposit webhook to resolve an incoming address to a user. */
    Optional<CryptoWallet> findByAddressIgnoreCase(String address);
}
