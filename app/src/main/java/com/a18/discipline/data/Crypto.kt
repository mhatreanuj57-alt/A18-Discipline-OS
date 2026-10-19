package com.a18.discipline.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.*

class PrivateVault {
    @Synchronized private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("a18-private-v1", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("a18-private-v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    fun encrypt(value: String, context: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()); updateAAD(context.toByteArray(Charsets.UTF_8)) }
        return Base64.getEncoder().encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)))
    }
    fun decrypt(value: String, context: String): String {
        val bytes = Base64.getDecoder().decode(value)
        require(bytes.size >= 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128,bytes.copyOfRange(0,12))); updateAAD(context.toByteArray(Charsets.UTF_8)) }
        return String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)), Charsets.UTF_8)
    }
}

object BackupCrypto {
    private const val label = "A18 Discipline OS encrypted backup"
    private val aad = "$label v1".toByteArray(Charsets.UTF_8)
    private fun derive(password: String, salt: ByteArray): SecretKey {
        require(password.length in 1..1024)
        val spec = PBEKeySpec(password.toCharArray(),salt,600000,256)
        return try { SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,"AES") } finally { spec.clearPassword() }
    }
    fun encrypt(plain: String, password: String): String {
        require(password.length in 12..1024) { "Use a backup passphrase of at least 12 characters" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,derive(password,salt)); updateAAD(aad) }
        return buildJsonObject {
            put("app",label); put("version",1); put("algorithm","AES-GCM-256"); put("kdf","PBKDF2-SHA256"); put("iterations",600000)
            put("salt",Base64.getEncoder().encodeToString(salt)); put("iv",Base64.getEncoder().encodeToString(cipher.iv))
            put("ciphertext",Base64.getEncoder().encodeToString(cipher.doFinal(plain.toByteArray(Charsets.UTF_8))))
        }.toString()
    }
    fun decode(value: String, password: String): String {
        val obj = json.parseToJsonElement(value).jsonObject
        if (obj["app"]?.jsonPrimitive?.content != label) return value
        require(obj["version"]?.jsonPrimitive?.int == 1 && obj["iterations"]?.jsonPrimitive?.int == 600000 && obj["algorithm"]?.jsonPrimitive?.content == "AES-GCM-256" && obj["kdf"]?.jsonPrimitive?.content == "PBKDF2-SHA256") { "Unsupported encryption" }
        fun bytes(k: String) = Base64.getDecoder().decode(obj.getValue(k).jsonPrimitive.content)
        val salt = bytes("salt"); val iv = bytes("iv"); require(salt.size == 16 && iv.size == 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE,derive(password,salt),GCMParameterSpec(128,iv)); updateAAD(aad) }
        return String(cipher.doFinal(bytes("ciphertext")),Charsets.UTF_8)
    }
}
