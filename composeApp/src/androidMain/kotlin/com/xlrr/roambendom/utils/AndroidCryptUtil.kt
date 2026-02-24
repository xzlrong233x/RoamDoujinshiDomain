package com.xlrr.roambendom.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


object AndroidCryptUtil {

    const val CRYPT_KEY = "TokenCryptKey"
    const val PROVIDER = "AndroidKeyStore"
    val ks: KeyStore? by lazy {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore
    }

    fun createKey() : SecretKey {
        if (!exist()) {
            val keyG = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
            val ksg = KeyGenParameterSpec.Builder(
                CRYPT_KEY,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyG.init(ksg)
            val keyProtection: KeyProtection =
                KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            val x = keyG.generateKey()
            ks?.setEntry(CRYPT_KEY, KeyStore.SecretKeyEntry(x), keyProtection)
            return x
        } else {
            return getSecretKey()
        }
    }

    fun getSecretKey() : SecretKey {
        return (ks?.getEntry(CRYPT_KEY, null) as KeyStore.SecretKeyEntry).secretKey
    }

    fun exist() : Boolean {
        return ks?.containsAlias(CRYPT_KEY) == true
    }
}