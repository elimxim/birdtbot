package com.github.elimxim.birdtbot

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class TelegramBotApp

fun main(args: Array<String>) {
    SpringApplication.run(TelegramBotApp::class.java, *args)
}