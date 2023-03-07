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
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.InputStream
import java.nio.file.Files
import java.util.Locale
import kotlin.io.path.*

class BirdCallsBot(
    private val botProperties: TelegramBotProperties,
    private val messages: Messages,
    botOptions: DefaultBotOptions
) : TelegramLongPollingBot(botOptions, botProperties.token) {

    private var commandsInstalled: Boolean = false

    override fun getBotUsername(): String {
        return botProperties.username!!
    }

    override fun onUpdateReceived(update: Update) {
        if (!commandsInstalled) {
            installCommands()
        }

        val locale = update.getLocale()

        if (update.hasMessage()) {
            if (update.message.text == Command.START) {
                sendTopKeyboard(update, locale)
            }
        } else if (update.hasCallbackQuery()) {
            val birds = loadAvailableBirds(locale)

            if (update.callbackQuery.data == CallbackCommand.SEND_VOICES) {
                sendVoicesKeyboard(update, birds, locale)
            } else if (birds.contains(update.callbackQuery.data)) {
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
                commands = listOf(
                    BotCommand().apply {
                        command = Command.START
                        description = "Launch @" + botProperties.username
                    }
                )
            })
        } else {
            commandsInstalled = true
        }
    }

    private fun sendTopKeyboard(update: Update, locale: Locale) {
        val keyboardMarkup = InlineKeyboardMarkup().apply {
            keyboard = listOf(
                listOf(
                    InlineKeyboardButton().apply {
                        text = Emoji.LOUD_SOUND + messages.get("bot.keyboard.voices.button.name", locale)
                        callbackData = CallbackCommand.SEND_VOICES
                    }
                )
            )
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

    private fun sendVoicesKeyboard(update: Update, birds: Set<String>, locale: Locale) {
        val birdGrid = birds.sorted()
            .foldIndexed(ArrayList<ArrayList<String>>()) { idx, acc, it ->
                if (idx % 3 == 0) {
                    acc.add(ArrayList(3))
                }

                acc.last().add(it)
                acc
            }

        if (birds.isNotEmpty()) {
            execute(EditMessageText().apply {
                chatId = update.getChatId()
                messageId = update.getMessageId()
                text = Emoji.BIRD + " " + messages.get("bot.keyboard.voices.message", locale)
                replyMarkup = InlineKeyboardMarkup().apply {
                    keyboard = birdGrid.map {
                        it.map {
                            InlineKeyboardButton().apply {
                                text = Emoji.MUSICAL_NOTE + it
                                callbackData = it
                            }
                        }
                    }.plus(listOf(
                        listOf(
                            InlineKeyboardButton().apply {
                                text = Emoji.BACK + messages.get("bot.keyboard.button.back.name", locale)
                                callbackData = CallbackCommand.BACK_VOICES
                            }
                        ))
                    )
                }
            })
        } else {
            execute(EditMessageText().apply {
                chatId = update.getChatId()
                messageId = update.getMessageId()
                text = Emoji.FACE + messages.get("bot.keyboard.voices.empty", locale)
                replyMarkup = InlineKeyboardMarkup().apply {
                    keyboard = listOf(
                        listOf(
                            InlineKeyboardButton().apply {
                                text = Emoji.BACK + messages.get("bot.keyboard.button.back.name", locale)
                                callbackData = CallbackCommand.BACK_VOICES
                            }
                        )
                    )
                }
            })
        }
    }

    private fun sendBirdVoice(update: Update, locale: Locale) {
        val bird = update.callbackQuery.data
        val filenames = loadBirdVoiceFilenames(locale, bird)

        val keyboardReply = InlineKeyboardMarkup().apply {
            keyboard = listOf(
                listOf(
                    InlineKeyboardButton().apply {
                        text = Emoji.BACK + messages.get("bot.keyboard.button.back.name", locale)
                        callbackData = CallbackCommand.BACK_BIRD_VOICE
                    }
                )
            )
        }

        if (filenames.isNotEmpty()) {
            val filename = filenames.random()

            removeCallbackMessage(update)
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

    private fun loadAvailableBirds(locale: Locale): Set<String> {
        return botProperties.voicesDir!!.resolve(locale.language).listDirectoryEntries()
            .filter { it.isDirectory() }
            .map { it.name }
            .toSet()
    }

    private fun loadBirdVoiceFilenames(locale: Locale, bird: String): List<String> {
        return botProperties.voicesDir!!.resolve(locale.language).resolve(bird).listDirectoryEntries()
            .filter { it.isRegularFile() }
            .map { it.fileName.name }
    }

    private fun birdVoiceInputStream(locale: Locale, bird: String, filename: String): InputStream {
        return Files.newInputStream(botProperties.voicesDir!!.resolve(locale.language).resolve(bird).resolve(filename))
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

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BirdCallsBot::class.java)
    }
}