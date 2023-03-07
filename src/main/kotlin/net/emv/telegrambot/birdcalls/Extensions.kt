package net.emv.telegrambot.birdcalls

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import java.util.*

fun Update.getChatId(): String {
    return if (hasMessage()) {
        message.chatId.toString()
    } else {
        callbackQuery.message.chatId.toString()
    }
}

fun Update.getMessageId(): Int {
    return if (hasMessage()) {
        message.messageId
    } else {
        callbackQuery.message.messageId
    }
}

fun Update.getLocale(): Locale {
    val user: User? = when {
        hasMessage() -> message.from
        hasCallbackQuery() -> callbackQuery.from
        else -> null
    }

    return if (user?.languageCode != null) {
        Locale.of(user.languageCode)
    } else {
        Locale.ENGLISH
    }
}