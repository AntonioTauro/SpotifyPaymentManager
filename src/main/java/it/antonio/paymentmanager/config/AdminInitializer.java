package it.antonio.paymentmanager.config;
import it.antonio.paymentmanager.entity.Friend;
import it.antonio.paymentmanager.repository.FriendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {
    private final FriendRepository friendRepository;

    @Value("${telegram.bot.admin-id}")
    private Long adminId;
    @Override
    public void run(String... args) throws Exception {
        if (friendRepository.findByTelegramUserId(adminId).isEmpty()) {
            log.info("Admin non trovato nel database. Creazione utente Admin automatico...");

            Friend admin = Friend.builder()
                    .name("Antonio (Admin)")
                    .telegramUserId(adminId)
                    .balance(BigDecimal.ZERO)
                    .active(true)
                    .build();

            friendRepository.save(admin);
            log.info("Admin (ID: {}) creato ed inserito con successo nel database!", adminId);
        } else {
            log.info("Admin (ID: {}) già presente nel database.", adminId);
        }
    }
}