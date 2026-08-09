package it.antonio.paymentmanager.config;
import it.antonio.paymentmanager.telegram.SpotifyBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class BotConfig {
    private static final Logger log = LoggerFactory.getLogger(BotConfig.class);

    @Bean
    public TelegramBotsApi telegramBotsApi(SpotifyBot spotifyBot) {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(spotifyBot);
            log.info("SpotifyBot registered successfully.");
            return api;
        } catch (TelegramApiException e) {
            log.error("SpotifyBot registration failed.", e);
            throw new RuntimeException(e);
        }
    }
}
