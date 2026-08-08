package it.antonio.paymentmanager.service;

import it.antonio.paymentmanager.entity.Friend;
import it.antonio.paymentmanager.entity.Transaction;
import it.antonio.paymentmanager.entity.TransactionType;
import it.antonio.paymentmanager.repository.FriendRepository;
import it.antonio.paymentmanager.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final FriendRepository friendRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void registerPayment(Long telegramUserId, BigDecimal amount) {
        Friend friend = friendRepository.findByTelegramUserId(telegramUserId).orElseThrow(()-> new IllegalArgumentException("Utente non trovato nel database"));
        friend.setBalance(friend.getBalance().add(amount));
        friendRepository.save(friend);
        Transaction transaction = Transaction.builder()
                .friend(friend)
                .amount(amount)
                .date(LocalDateTime.now())
                .type(TransactionType.PAYMENT)
                .build();
        transactionRepository.save(transaction);
    }


}


