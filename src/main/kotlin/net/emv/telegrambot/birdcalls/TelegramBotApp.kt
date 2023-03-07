package net.emv.telegrambot.birdcalls

import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class TelegramBotApp

fun main(args: Array<String>) {
    runApplication<TelegramBotApp>(*args) {
        setBannerMode(Banner.Mode.OFF)
    }
}