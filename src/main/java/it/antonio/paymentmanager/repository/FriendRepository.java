package it.antonio.paymentmanager.repository;

import it.antonio.paymentmanager.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {
    Optional<Friend> findByTelegramUserId(Long telegramUserId);
    List<Friend> findByActiveTrue();
    List<Friend> findByBalanceLessThan(BigDecimal amount);
    Optional<Friend> findByName(String name);
}
