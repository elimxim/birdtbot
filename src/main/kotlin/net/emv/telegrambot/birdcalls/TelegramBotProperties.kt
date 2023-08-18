package net.emv.telegrambot.birdcalls

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.springframework.validation.annotation.Validated
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Path

@Validated
@ConfigurationProperties(prefix = "telegram.bot")
class TelegramBotProperties {
    @NotBlank
    var username: String? = null

    @NotBlank
    var token: String? = null

    @NotEmpty
    var birds: Map<String, Map<String, String>> = mapOf()
}