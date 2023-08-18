package net.emv.telegrambot.birdcalls

import com.vdurmont.emoji.EmojiParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.commands.GetMyCommands
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendVoice
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.io.path.*

class BirdCallsBot(private val botProperties: TelegramBotProperties,
                   private val messages: Messages,
                   botOptions: DefaultBotOptions)
    : TelegramLongPollingBot(botOptions, botProperties.token) {
    private var commandsInstalled: Boolean = false

    override fun getBotUsername(): String {
        return botProperties.username!!
    }

    override fun onUpdateReceived(update: Update) {
        if (!commandsInstalled) {
            installCommands()
        }

        val locale = determineLocale(update.getLocale())

        if (update.hasMessage()) {
            if (update.message.text == Command.START) {
                sendTopKeyboard(update, locale)
            }
        } else if (update.hasCallbackQuery()) {
            val birds = getAvailableBirds(locale)

            if (update.callbackQuery.data == CallbackCommand.SEND_VOICES) {
                sendVoicesKeyboard(update, birds, locale)
            } else if (birds.keys.contains(update.callbackQuery.data)) {
                sendBirdVoice(update, locale)
            } else if (update.callbackQuery.data == CallbackCommand.BACK_VOICES) {
                sendTopKeyboard(update, locale)
            } else if (update.callbackQuery.data == CallbackCommand.BACK_BIRD_VOICE) {
                sendVoicesKeyboard(update, birds, locale)
            }
        }
    }

    private fun commandsNotInstalled(): Boolean {
        val commands = execute(GetMyCommands())
        return commands.isEmpty()
    }

    private fun installCommands() {
        if (commandsNotInstalled()) {
            commandsInstalled = execute(SetMyCommands().apply {
                commands = listOf(BotCommand().apply {
                    command = Command.START
                    description = "Launch @" + botProperties.username
                })
            })
        } else {
            commandsInstalled = true
        }
    }

    private fun sendTopKeyboard(update: Update, locale: Locale) {
        val keyboardMarkup = InlineKeyboardMarkup().apply {
            keyboard = listOf(listOf(InlineKeyboardButton().apply {
                text = Emoji.LOUD_SOUND + messages.get("bot.keyboard.voices.button.name", locale)
                callbackData = CallbackCommand.SEND_VOICES
            }))
        }

        if (update.hasMessage()) {
            execute(SendMessage().apply {
                chatId = update.getChatId()
                text = Emoji.DOOR + messages.get("bot.keyboard.top.message", locale)
                replyMarkup = keyboardMarkup
            })
        } else if (update.hasCallbackQuery()) {
            execute(EditMessageText().apply {
                chatId = update.getChatId()
                messageId = update.getMessageId()
                text = Emoji.DOOR + messages.get("bot.keyboard.top.message", locale)
                replyMarkup = keyboardMarkup
            })
        }
    }

    private fun sendVoicesKeyboard(update: Update, birds: Map<String, String>, locale: Locale) {
        val birdGrid = buildBirdAdaptiveGrid(birds)

        if (birds.isNotEmpty()) {
            execute(EditMessageText().apply {
                chatId = update.getChatId()
                messageId = update.getMessageId()
                text = Emoji.BIRD + " " + messages.get("bot.keyboard.voices.message", locale)
                replyMarkup = InlineKeyboardMarkup().apply {
                    keyboard = birdGrid.map {
                        it.map {
                            InlineKeyboardButton().apply {
                                text = Emoji.MUSICAL_NOTE + it.second
                                callbackData = it.first
                            }
                        }
                    }.plus(listOf(listOf(InlineKeyboardButton().apply {
                        text = Emoji.BACK + messages.get("bot.keyboard.button.back.name", locale)
                        callbackData = CallbackCommand.BACK_VOICES
                    })))
                }
            })
        } else {
            execute(EditMessageText().apply {
                chatId = update.getChatId()
                messageId = update.getMessageId()
                text = Emoji.FACE + messages.get("bot.keyboard.voices.empty", locale)
                replyMarkup = InlineKeyboardMarkup().apply {
                    keyboard = listOf(listOf(InlineKeyboardButton().apply {
                        text = Emoji.BACK + messages.get("bot.keyboard.button.back.name", locale)
                        callbackData = CallbackCommand.BACK_VOICES
                    }))
                }
            })
        }
    }

    private fun sendBirdVoice(update: Update, locale: Locale) {
        val bird = update.callbackQuery.data
        val filenames = loadBirdVoiceFilenames(locale, bird)

        val keyboardReply = InlineKeyboardMarkup().apply {
            keyboard = listOf(listOf(InlineKeyboardButton().apply {
                text = Emoji.BACK + messages.get("bot.keyboard.button.back.name", locale)
                callbackData = CallbackCommand.BACK_BIRD_VOICE
            }))
        }

        if (filenames.isNotEmpty()) {
            val filename = filenames.random()

            removeCallbackMessage(update)

            execute(SendMessage().apply {
                chatId = update.getChatId()
                text = Emoji.BIRD + translateBirdName(locale, bird)
            })

            birdVoiceInputStream(locale, bird, filename).use {
                execute(SendVoice().apply {
                    chatId = update.getChatId()
                    voice = InputFile(it, filename)
                })
            }

            execute(SendMessage().apply {
                chatId = update.getChatId()
                text = Emoji.PENCIL + messages.get("bot.keyboard.voices.bird.message", locale)
                replyMarkup = keyboardReply
            })
        } else {
            execute(EditMessageText().apply {
                chatId = update.getChatId()
                messageId = update.getMessageId()
                text = Emoji.FACE + messages.get("bot.keyboard.voices.bird.empty", locale)
                replyMarkup = keyboardReply
            })
        }
    }

    private fun removeCallbackMessage(update: Update) {
        execute(DeleteMessage().apply {
            chatId = update.getChatId()
            messageId = update.getMessageId()
        })
    }

    private fun getAvailableBirds(locale: Locale): Map<String, String> {
        if (locale == Locale.ENGLISH) {
            return botProperties.birds.keys.stream().collect(Collectors.toMap({ it }, { it }))
        }

        return botProperties.birds.entries.stream()
                .filter { it.value[locale.language] != null }
                .collect(Collectors.toMap({ it.key }, { it.value[locale.language]!! }))
    }

    private fun translateBirdName(locale: Locale, name: String): String {
        return if (locale != Locale.ENGLISH) {
            botProperties.birds[name]!![locale.language]!!
        } else {
            name
        }
    }

    private fun loadBirdVoiceFilenames(locale: Locale, bird: String): List<String> {
        return getBirdDir(locale, bird).listDirectoryEntries()
                .filter { it.isRegularFile() }
                .map { it.fileName.name }
    }

    private fun birdVoiceInputStream(locale: Locale, bird: String, filename: String): InputStream {
        return Files.newInputStream(getBirdDir(locale, bird).resolve(filename))
    }

    private fun getBirdDir(locale: Locale, bird: String): Path {
        val dir = bird.lowercase(locale).replace(" ", "_")
        return this::class.java.classLoader.getResource("birds/$dir")!!.toURI().toPath()
    }

    private fun buildBirdAdaptiveGrid(birds: Map<String, String>): Array<Array<Pair<String, String>>> {
        val sortedBirds = birds.entries.stream()
                .sorted(Comparator.comparing { it.value })
                .map { Pair(it.key, it.value) }
                .collect(Collectors.toList())

        val grid = mutableListOf<Array<Pair<String, String>>>()
        val row = mutableListOf<Pair<String, String>>()

        val iterator = sortedBirds.iterator()
        while (iterator.hasNext()) {
            val pair = iterator.next()

            val len = pair.second.length
            val sum = row.stream().mapToInt { it.second.length }.sum()

            if (len + sum > 23) {
                grid.add(row.toTypedArray())
                row.clear()
            }

            row.add(pair)
        }

        return grid.toTypedArray()
    }

    private fun determineLocale(userLocale: Locale): Locale {
        return if (getAvailableLocales().contains(userLocale.language)) {
            userLocale
        } else {
            Locale.ENGLISH
        }
    }

    private fun getAvailableLocales(): Set<String> {
        return botProperties.birds.values.stream()
                .map { it.keys }
                .flatMap { it.stream() }
                .collect(Collectors.toSet())
    }

    private object Command {
        const val START = "/start"
    }

    private object CallbackCommand {
        const val SEND_VOICES = "/send_voices"
        const val BACK_VOICES = "/back_voices"
        const val BACK_BIRD_VOICE = "/back_bird_voice"
    }

    private object Emoji {
        val DOOR: String = EmojiParser.parseToUnicode(":door: ")
        val LOUD_SOUND: String = EmojiParser.parseToUnicode(":loud_sound: ")
        val BIRD: String = EmojiParser.parseToUnicode(":bird: ")
        val BACK: String = EmojiParser.parseToUnicode(":arrow_backward: ")
        val FACE: String = EmojiParser.parseToUnicode(":face_with_monocle: ")
        val MUSICAL_NOTE: String = EmojiParser.parseToUnicode(":musical_note: ")
        val PENCIL: String = EmojiParser.parseToUnicode(":pencil2: ")
    }
}