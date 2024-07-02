package com.github.elimxim.birdtbot

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@Configuration
@EnableConfigurationProperties(TelegramBotProperties::class)
open class TelegramBotConfig(private val botProperties: TelegramBotProperties) {

    @Bean
    open fun birdCallsBot(messages: Messages): BirdCallsBot {
        return BirdCallsBot(botProperties, messages, DefaultBotOptions())
    }

    @Bean
    open fun telegramBotsApi(birdCallsBot: BirdCallsBot): TelegramBotsApi {
        return TelegramBotsApi(DefaultBotSession::class.java).apply {
            registerBot(birdCallsBot)
        }
    }
}