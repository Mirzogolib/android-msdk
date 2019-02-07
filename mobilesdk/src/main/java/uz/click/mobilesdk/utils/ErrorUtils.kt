package uz.click.mobilesdk.utils

import android.content.Context
import uz.click.mobilesdk.R
import uz.click.mobilesdk.core.errors.*
import java.io.IOException
import java.net.HttpRetryException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*

/**
 * @author rahmatkhujaevs on 29/01/19
 * */
object ErrorUtils {
    fun isApiError(e: Exception): Boolean {
        return when (e) {
            is HttpRetryException -> {
                false
            }
            is SocketTimeoutException -> false
            is IOException -> false
            is UnknownHostException -> false
            is ServerNotAvailableException -> true
            is ServerErrorException -> true
            is WrongServiceIdException -> true
            is MinimalAmountRangeException -> true
            is MaximalAmountRangeException -> true
            is IncorrectCardNumberException -> true
            is InvalidRequestIdException -> true
            is UnknownErrorException -> true
            else -> false
        }
    }

    fun getErrorMessage(e: Exception, locale: Locale, context: Context): String {
        return when (e) {
            is WrongServiceIdException -> {
                LanguageUtils.getLocaleStringResource(locale, R.string.service_id_error, context)
            }
            is MinimalAmountRangeException -> {
                LanguageUtils.getLocaleStringResource(locale, R.string.amount_error, context)
            }
            is MaximalAmountRangeException -> {
                LanguageUtils.getLocaleStringResource(locale, R.string.max_amount_error, context)
            }
            is IncorrectCardNumberException -> {
                LanguageUtils.getLocaleStringResource(locale, R.string.card_number_error, context)
            }
            is InvalidRequestIdException -> {
                LanguageUtils.getLocaleStringResource(locale, R.string.request_id_error, context)
            }
            is ServerErrorException -> {
                LanguageUtils.getLocaleStringResource(locale, R.string.server_error, context)
            }
            else -> {
                LanguageUtils.getLocaleStringResource(locale, R.string.unknown_error, context)
            }
        }
    }

    fun getException(code: Int?, message: String?): Exception {
        return when (code) {
            -1 -> {
                WrongServiceIdException(code, message)
            }
            -6 -> {
                MinimalAmountRangeException(code, message)
            }
            7 -> {
                MaximalAmountRangeException(code, message)
            }
            -400 -> {
                IncorrectCardNumberException(code, message)
            }
            -403 -> {
                InvalidRequestIdException(code, message)
            }
            -500 -> {
                ServerErrorException(code, message)
            }
            else -> {
                UnknownErrorException(code, message)
            }
        }
    }
}