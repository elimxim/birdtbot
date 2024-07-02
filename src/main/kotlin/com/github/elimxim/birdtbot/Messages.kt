package com.github.elimxim.birdtbot

import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import java.util.*

@Component
class Messages(private val messageSource: MessageSource) {
    fun get(code: String, locale: Locale): String {
        return messageSource.getMessage(code, arrayOf(), locale)
    }
}