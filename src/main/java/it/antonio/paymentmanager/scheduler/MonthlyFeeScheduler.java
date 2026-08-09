package it.antonio.paymentmanager.scheduler;
import it.antonio.paymentmanager.entity.Friend;
import it.antonio.paymentmanager.entity.Transaction;
import it.antonio.paymentmanager.entity.TransactionType;
import it.antonio.paymentmanager.repository.FriendRepository;
import it.antonio.paymentmanager.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyFeeScheduler {
    private final TransactionRepository transactionRepository;
    private final FriendRepository friendRepository;
    @Value("${telegram.bot.admin-id}")
    private Long adminId;
    @Value("${app.spotify.monthly-fee}")
    private BigDecimal monthlyFee;
    @Scheduled (cron = "${app.spotify.cron-schedule}")
    @Transactional
    public void processMonthlyFee() {
        log.info("Processing Monthly fee scheduled");
        List<Friend> activeFriends = friendRepository.findByActiveTrue();
        for (Friend friend : activeFriends) {
            if (!friend.getTelegramUserId().equals(adminId)) {
                friend.setBalance(friend.getBalance().subtract(monthlyFee));
                friendRepository.save(friend);
                Transaction transaction = Transaction.builder()
                        .friend(friend)
                        .amount(monthlyFee.negate())
                        .date(LocalDateTime.now())
                        .type(TransactionType.MONTHLY_FEE)
                        .build();
                transactionRepository.save(transaction);
                log.info("Added transaction per l'utente {}", friend.getName());
            }
        }
        log.info("Monthly fee scheduled done");
    }
}
