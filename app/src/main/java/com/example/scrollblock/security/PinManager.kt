package com.example.scrollblock.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ebeveyn / Güvenlik PIN kilidi.
 *
 * Saklama: Ayrı bir SharedPreferences dosyasında YALNIZCA salt + PBKDF2 hash tutulur.
 * (EncryptedSharedPreferences / security-crypto kütüphanesi Google tarafından kullanımdan
 * kaldırıldığı için, hash'leme + deneme sınırı daha sağlam ve bağımlılıksız bir çözümdür.)
 *
 * Kaba kuvvet koruması: 5 hatalı denemede 30 sn, sonraki her 5 hatada süre ikiye katlanır
 * (en fazla 15 dk). Kilit süresi uygulama yeniden başlatılsa da korunur.
 */
class PinManager private constructor(context: Context) {

    sealed interface VerifyResult {
        data object Success : VerifyResult
        data class Wrong(val attemptsLeft: Int) : VerifyResult
        data class LockedOut(val remainingMs: Long) : VerifyResult
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isPinEnabled = MutableStateFlow(isPinEnabledSync())
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    fun isPinEnabledSync(): Boolean =
        prefs.getBoolean(KEY_ENABLED, false) && prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT)

    /** Yeni PIN belirler ve kilidi aktif eder. */
    fun setPin(pin: String): Boolean {
        if (!PinHasher.isValidFormat(pin)) return false
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, PinHasher.toHex(salt))
            .putString(KEY_HASH, PinHasher.toHex(hash))
            .putBoolean(KEY_ENABLED, true)
            .putInt(KEY_FAILED, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
        _isPinEnabled.value = true
        return true
    }

    /** PIN kilidini kapatır ve hash'i siler. Çağırmadan önce [verify] başarılı olmalı. */
    fun disablePin() {
        prefs.edit()
            .remove(KEY_SALT)
            .remove(KEY_HASH)
            .putBoolean(KEY_ENABLED, false)
            .putInt(KEY_FAILED, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
        _isPinEnabled.value = false
    }

    fun lockoutRemainingMs(now: Long = System.currentTimeMillis()): Long =
        (prefs.getLong(KEY_LOCKOUT_UNTIL, 0L) - now).coerceAtLeast(0L)

    @Synchronized
    fun verify(pin: String, now: Long = System.currentTimeMillis()): VerifyResult {
        val remaining = lockoutRemainingMs(now)
        if (remaining > 0) return VerifyResult.LockedOut(remaining)

        val saltHex = prefs.getString(KEY_SALT, null)
        val hashHex = prefs.getString(KEY_HASH, null)
        if (saltHex == null || hashHex == null) return VerifyResult.Success // PIN yok

        val ok = try {
            PinHasher.verify(pin, PinHasher.fromHex(saltHex), PinHasher.fromHex(hashHex))
        } catch (_: Exception) {
            false
        }

        if (ok) {
            prefs.edit().putInt(KEY_FAILED, 0).putLong(KEY_LOCKOUT_UNTIL, 0L).apply()
            return VerifyResult.Success
        }

        val failed = prefs.getInt(KEY_FAILED, 0) + 1
        val editor = prefs.edit().putInt(KEY_FAILED, failed)
        return if (failed % MAX_ATTEMPTS == 0) {
            val round = failed / MAX_ATTEMPTS // 1, 2, 3...
            val lockMs = (BASE_LOCKOUT_MS shl (round - 1).coerceAtMost(5)).coerceAtMost(MAX_LOCKOUT_MS)
            editor.putLong(KEY_LOCKOUT_UNTIL, now + lockMs).apply()
            VerifyResult.LockedOut(lockMs)
        } else {
            editor.apply()
            VerifyResult.Wrong(MAX_ATTEMPTS - failed % MAX_ATTEMPTS)
        }
    }

    companion object {
        private const val PREFS_NAME = "scrollblock_security"
        private const val KEY_ENABLED = "pin_enabled"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_FAILED = "pin_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "pin_lockout_until"

        const val MAX_ATTEMPTS = 5
        private const val BASE_LOCKOUT_MS = 30_000L
        private const val MAX_LOCKOUT_MS = 15 * 60_000L

        @Volatile
        private var INSTANCE: PinManager? = null

        fun getInstance(context: Context): PinManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PinManager(context).also { INSTANCE = it }
            }
    }
}
