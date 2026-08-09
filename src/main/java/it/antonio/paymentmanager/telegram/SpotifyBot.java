package it.antonio.paymentmanager.telegram;
import it.antonio.paymentmanager.entity.Friend;
import it.antonio.paymentmanager.entity.Transaction;
import it.antonio.paymentmanager.repository.FriendRepository;
import it.antonio.paymentmanager.repository.TransactionRepository;
import it.antonio.paymentmanager.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class SpotifyBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyBot.class);
    private final String botUsername;
    private final Long adminId;
    private final String paymentInfo;
    private final PaymentService paymentService;
    private final TransactionRepository transactionRepository;
    private final FriendRepository friendRepository;

    public SpotifyBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.admin-id}") Long adminId,
            @Value("${app.payment.info}") String paymentInfo,
            PaymentService paymentService,
            FriendRepository friendRepository,
            TransactionRepository transactionRepository) {
        super(botToken);
        this.botUsername = botUsername;
        this.adminId = adminId;
        this.paymentInfo = paymentInfo;
        this.paymentService = paymentService;
        this.friendRepository = friendRepository;
        this.transactionRepository = transactionRepository;
    }
    @Override
    public String getBotUsername() {
        return botUsername;
    }
    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        String userName = update.getMessage().getFrom().getFirstName();

        logger.info("Comando ricevuto da {}: {}", userName, messageText);

        boolean isAdmin = chatId.equals(adminId); // Controlla se chi scrive è l'amministratore

        try {
            if (messageText.startsWith("/start")) {
                handleStart(chatId, userName); // Permesso a tutti (serve per farsi dare l'ID)
            } else if (messageText.startsWith("/saldo")) {
                handleSaldo(chatId); // Permesso a tutti (ma si comporterà diversamente)
            } else if (messageText.startsWith("/versa") && isAdmin) {
                handleVersa(chatId, messageText); // Permesso SOLO all'Admin
            } else if (messageText.startsWith("/aggiungi") && isAdmin) {
                handleAggiungi(chatId, messageText); // Permesso SOLO all'Admin
            } else {
                // Messaggio di errore differenziato
                if (isAdmin) {
                    sendMessage(chatId, "❓ Comando non riconosciuto. Usa /saldo, /versa o /aggiungi.");
                } else {
                    sendMessage(chatId, "❓ Comando non riconosciuto. Puoi usare solo il comando /saldo.");
                }
            }
        } catch (Exception e) {
            logger.error("Errore durante l'elaborazione del comando", e);
            sendMessage(chatId, "❌ Errore: " + e.getMessage());
        }
    }

    private void handleSaldo(Long chatId) {

        // BIVIO 1: VISTA AMMINISTRATORE (Situazione Globale)
        if (chatId.equals(adminId)) {
            List<Friend> allFriends = friendRepository.findByActiveTrue();

            if (allFriends.isEmpty()) {
                sendMessage(chatId, "📭 Il database è vuoto. Non ci sono amici registrati.");
                return;
            }

            StringBuilder adminResponse = new StringBuilder();
            adminResponse.append("👑 **SITUAZIONE GLOBALE GRUPPO SPOTIFY** 👑\n\n");

            BigDecimal totaleDebiti = BigDecimal.ZERO;

            for (Friend f : allFriends) {
                String icona = f.getBalance().compareTo(BigDecimal.ZERO) < 0 ? "🔴" : "🟢";
                adminResponse.append(icona).append(" ").append(f.getName()).append("(" + f.getTelegramUserId() + ")").append(": ").append(f.getBalance()).append("€\n");

                // Se è in rosso, sommiamo al totale dei debiti da recuperare
                if (f.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                    totaleDebiti = totaleDebiti.add(f.getBalance());
                }
            }

            adminResponse.append("\n💸 **Crediti totali da recuperare:** ").append(totaleDebiti.abs()).append("€");
            sendMessage(chatId, adminResponse.toString());
            return;
        }

        // BIVIO 2: VISTA AMICO (Situazione Singola)
        Friend friend = friendRepository.findByTelegramUserId(chatId)
                .orElseThrow(() -> new RuntimeException("Non sei registrato. Chiedi ad Antonio di aggiungerti."));

        StringBuilder response = new StringBuilder();
        response.append("👤 **Il tuo Saldo Attuale:** ").append(friend.getBalance()).append("€\n\n");

        if (friend.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            response.append("⚠️ **ATTENZIONE:** Sei in debito!\n")
                    .append("Per rimetterti in pari, segui queste istruzioni:\n")
                    .append("👉 ").append(paymentInfo).append("\n\n");
        }

        response.append("📄 **Ultimi 5 movimenti:**\n");

        List<Transaction> ultimiMovimenti = transactionRepository.findTop5ByFriendIdOrderByDateDesc(friend.getId());

        for (Transaction t : ultimiMovimenti) {
            String icona = t.getAmount().compareTo(BigDecimal.ZERO) < 0 ? "🔴" : "🟢";
            response.append(icona).append(" ").append(t.getAmount()).append("\n");
        }

        sendMessage(chatId, response.toString());
    }

    private void handleStart(Long chatId, String userName) {
        Optional<Friend> friendOpt = friendRepository.findByTelegramUserId(chatId);

        if (friendOpt.isEmpty()) {
            // Blocca l'accesso e fornisce l'ID per farsi aggiungere dall'admin
            logger.warn("Tentativo di accesso non autorizzato da: {} (ID: {})", userName, chatId);

            String msg = "⛔ Accesso negato.\nNon sei registrato nel sistema.\n\n" +
                    "Se fai parte del gruppo Spotify, copia questo numero e invialo ad Antonio in chat privata per farti aggiungere:\n\n" +
                    "👉 `" + chatId + "`";

            sendMessage(chatId, msg);
        } else {
            if (chatId.equals(adminId)) {
                sendMessage(chatId, "👑 Bentornato mio capo supremo " + friendRepository.findByTelegramUserId(adminId).get().getName() + ". Puoi usare i comandi /saldo, /versa e /aggiungi.");
            }else {
                sendMessage(chatId, "👋 Bentornato " + userName + "! Sei già registrato nel sistema. Digita /saldo per vedere la tua situazione.");
            }
        }
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Errore nell'invio del messaggio", e);
        }
    }

    private void handleVersa(Long chatId, String messageText) {
        if (!chatId.equals(adminId)) {
            sendMessage(chatId, "⛔ Accesso Negato: solo l'amministratore può registrare pagamenti.");
            return;
        }

        // Estrazione dei parametri dal testo (es: "/versa 123456789 15.00")
        String[] parts = messageText.split(" ");
        if (parts.length != 3) {
            sendMessage(chatId, "⚠️ Formato errato. Usa: /versa <ID_Telegram_Amico> <Importo>");
            return;
        }

        Long targetUserId;
        BigDecimal amount;

        try {
            targetUserId = Long.parseLong(parts[1]);
            // Il replace permette di usare sia la virgola che il punto per i decimali
            amount = new BigDecimal(parts[2].replace(",", "."));
        } catch (NumberFormatException e) {
            sendMessage(chatId, "⚠️ ID o Importo non validi. Assicurati di non aver inserito lettere per sbaglio.");
            return;
        }

        try {
            // Richiama la logica di business nel Service!
            paymentService.registerPayment(targetUserId, amount);

            // Feedback per admin
            sendMessage(chatId, "✅ Pagamento di " + amount + "€ registrato con successo!");
            // Feedback per l'amico
            sendMessage(targetUserId, "💰 Antonio ha appena confermato un tuo pagamento di " + amount + "€. Controlla il tuo /saldo.");
        } catch (IllegalArgumentException e) {
            // Se scrivi un ID che non esiste nel database, il Service lancia un'eccezione che catturiamo qui
            sendMessage(chatId, "❌ Errore: " + e.getMessage());
        }
    }

    private void handleAggiungi(Long chatId, String messageText) {
        if (!chatId.equals(adminId)) {
            sendMessage(chatId, "⛔ Accesso Negato: solo l'amministratore può aggiungere utenti.");
            return;
        }

        // Estrazione dei parametri (es: "/aggiungi Marco 123456789")
        String[] parts = messageText.split(" ");
        if (parts.length < 3 || parts.length > 4) {
            sendMessage(chatId, "⚠️ Formato errato. Usa: /aggiungi <Nome> <ID_Telegram> oppure /aggiungi <Nome> <ID_Telegram> <Saldo_Iniziale>");
            return;
        }

        String nome = parts[1];
        Long targetUserId;

        try {
            targetUserId = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "⚠️ L'ID Telegram deve essere un numero valido.");
            return;
        }
        BigDecimal amount = BigDecimal.ZERO;
        if (parts.length == 4) {
            try {
                amount = new BigDecimal(parts[3]);
            } catch (NumberFormatException e) {
                sendMessage(chatId,"Errore nella scrittura del saldo");
            }
        }
        // Controllo per evitare duplicati nel database
        if (friendRepository.findByTelegramUserId(targetUserId).isPresent()) {
            sendMessage(chatId, "⚠️ Attenzione: L'utente con ID " + targetUserId + " è già presente nel database!");
            return;
        }

        // Creazione sicura del nuovo record nel DB
        Friend newFriend = Friend.builder()
                .name(nome)
                .telegramUserId(targetUserId)
                .balance(amount)
                .active(true)
                .build();

        friendRepository.save(newFriend);

        // Feedback all'Admin
        sendMessage(chatId, "✅ Utente " + nome + " aggiunto con successo al database!");
        // Notifica Push automatica all'amico appena inserito
        sendMessage(targetUserId, "🎉 Ciao " + nome + ", Antonio ti ha appena aggiunto al gestore quote Spotify! Digita /saldo per iniziare.");
    }
}
