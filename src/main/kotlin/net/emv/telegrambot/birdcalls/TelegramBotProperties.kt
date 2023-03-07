package net.emv.telegrambot.birdcalls

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.springframework.validation.annotation.Validated
import java.nio.file.Files
import java.nio.file.Path

@Validated
@ConfigurationProperties(prefix = "telegram.bot")
class TelegramBotProperties : Validator {
    @NotBlank
    var username: String? = null

    @NotBlank
    var token: String? = null

    @NotNull
    var voicesDir: Path? = null

    override fun supports(clazz: Class<*>): Boolean {
        return TelegramBotProperties::class.java.isAssignableFrom(clazz)
    }

    override fun validate(target: Any, errors: Errors) {
        val properties = target as TelegramBotProperties

        properties.voicesDir?.let {
            if (Files.notExists(it)) {
                errors.rejectValue(
                    "voices-dir",
                    "net.emv.validation.constraints.notExists",
                    "Directory doesn't exist"
                )
            } else if (!Files.isDirectory(it)) {
                errors.rejectValue(
                    "voices-dir",
                    "net.emv.validation.constraints.isNotDirectory",
                    "Path isn't a directory"
                )
            }
        }
    }
}