package it.antonio.paymentmanager.scheduler;
import it.antonio.paymentmanager.entity.Friend;
import it.antonio.paymentmanager.repository.FriendRepository;
import it.antonio.paymentmanager.telegram.SpotifyBot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ReminderScheduler.class);
    private final FriendRepository friendRepository;
    private final SpotifyBot spotifyBot;
    @Value("${telegram.bot.admin-id}")
    private Long adminId;

    @Scheduled(cron = "0 0 10 * * *")
    public void processReminders() {
        logger.info("Processing daily scanning for reminders...");
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        List<Friend> friendsToRemind = friendRepository.findByBalanceLessThan(BigDecimal.ZERO);
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        for (Friend friend : friendsToRemind) {
            BigDecimal balance = friend.getBalance();
            boolean toBeReminded = false;
            String urgencyLabel = "";
            if (balance.compareTo(new BigDecimal("-14.00"))<=0) {
                toBeReminded = true;
                urgencyLabel = "🚨 AVVISO CRITICO";
            } else if (balance.compareTo(new BigDecimal("-10.50"))<=0) {
                if (today == DayOfWeek.MONDAY || today == DayOfWeek.THURSDAY) {
                    toBeReminded = true;
                    urgencyLabel = "⚠️ SOLLECITO DI PAGAMENTO";
                }
            }
            else {
                if (today == DayOfWeek.MONDAY) {
                    toBeReminded = true;
                    urgencyLabel = "🔔 Promemoria Settimanale";
                }
            }
            if (toBeReminded && !friend.getTelegramUserId().equals(adminId)) {
                String msg = urgencyLabel + "\n\n" +
                        "Ciao " + friend.getName() + ",\n" +
                        "Il tuo saldo è in rosso di " + balance.abs() + "€.\n" +
                        "Digita il comand /saldo per vedere le istruzioni e rimetterti in pari con la quota!\nGrazie\nCordiali Saluti!\n\nSpotiPay (schiavo di Antonio)";
                spotifyBot.sendMessage(friend.getTelegramUserId(), msg);
                logger.info("inviato un reminder ({}) a: {}", msg, friend.getName());
            }
        }
        logger.info("Daily scanning for reminders done. Numbers of Debtors: {}", friendsToRemind.size());
    }
}
