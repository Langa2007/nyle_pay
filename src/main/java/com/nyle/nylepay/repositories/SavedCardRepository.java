package com.nyle.nylepay.repositories;

import com.nyle.nylepay.models.SavedCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SavedCardRepository extends JpaRepository<SavedCard, Long> {
    List<SavedCard> findByUserIdAndIsActiveTrue(Long userId);
    Optional<SavedCard> findByUserIdAndIdAndIsActiveTrue(Long userId, Long cardId);
    Optional<SavedCard> findByUserIdAndIsDefaultTrueAndIsActiveTrue(Long userId);
    List<SavedCard> findByFingerprint(String fingerprint);
}
