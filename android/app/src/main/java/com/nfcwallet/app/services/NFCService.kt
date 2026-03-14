package com.nfcwallet.app.services

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.google.gson.Gson
import com.nfcwallet.app.types.NFCCardPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.Charset

object NFCService {

    private val gson = Gson()

    // ─── Write ────────────────────────────────────────────────────────────────

    /**
     * Writes [payload] as a UTF-8 text NDEF record to [tag].
     * Handles both already-formatted (Ndef) and formatable (NdefFormatable) tags.
     * Must be called from a coroutine; switches to [Dispatchers.IO] internally.
     *
     * @throws Exception if write fails
     */
    suspend fun writePayload(tag: Tag, payload: NFCCardPayload): Unit =
        withContext(Dispatchers.IO) {
            val json    = gson.toJson(payload)
            val record  = NdefRecord.createTextRecord("en", json)
            val message = NdefMessage(arrayOf(record))

            // Try as already-formatted NDEF first
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                try {
                    require(ndef.isWritable) { "NFC tag is read-only" }
                    val maxSize = ndef.maxSize
                    require(message.toByteArray().size <= maxSize) {
                        "NDEF message (${ message.toByteArray().size } bytes) exceeds tag capacity ($maxSize bytes)"
                    }
                    ndef.writeNdefMessage(message)
                } finally {
                    try { ndef.close() } catch (_: Exception) {}
                }
                return@withContext
            }

            // Fall back to NdefFormatable
            val formatable = NdefFormatable.get(tag)
                ?: throw IllegalStateException("Tag does not support NDEF")
            formatable.connect()
            try {
                formatable.format(message)
            } finally {
                try { formatable.close() } catch (_: Exception) {}
            }
        }

    // ─── Read ─────────────────────────────────────────────────────────────────

    /**
     * Reads the first NDEF text record from [tag] and parses it as [NFCCardPayload].
     * Must be called from a coroutine; switches to [Dispatchers.IO] internally.
     *
     * @throws Exception if tag has no NDEF message or the record cannot be parsed
     */
    suspend fun readPayload(tag: Tag): NFCCardPayload =
        withContext(Dispatchers.IO) {
            val ndef = Ndef.get(tag)
                ?: throw IllegalStateException("Tag is not NDEF formatted")

            ndef.connect()
            val ndefMessage = try {
                ndef.ndefMessage
                    ?: ndef.cachedNdefMessage
                    ?: throw IllegalStateException("No NDEF message found on tag")
            } finally {
                try { ndef.close() } catch (_: Exception) {}
            }

            val json = extractTextFromNdefMessage(ndefMessage)
            gson.fromJson(json, NFCCardPayload::class.java)
                ?: throw IllegalStateException("Failed to parse NFCCardPayload from JSON: $json")
        }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Extracts the text payload from the first text-type NDEF record.
     * NDEF Text record format:
     *   Byte 0: status byte (bit 7: UTF-16 flag, bits 5-0: language code length)
     *   Bytes 1..langLen: language code (e.g. "en")
     *   Remaining bytes: text payload
     */
    private fun extractTextFromNdefMessage(message: NdefMessage): String {
        for (record in message.records) {
            val tnf    = record.tnf
            val type   = record.type

            val isText = (tnf == NdefRecord.TNF_WELL_KNOWN &&
                          type.contentEquals(NdefRecord.RTD_TEXT)) ||
                         (tnf == NdefRecord.TNF_MIME_MEDIA &&
                          String(type, Charsets.US_ASCII).startsWith("text/"))

            if (isText) {
                val payload = record.payload
                // Decode NDEF text record
                val statusByte = payload[0].toInt() and 0xFF
                val langCodeLen = statusByte and 0x3F
                val isUtf16    = (statusByte and 0x80) != 0
                val charset    = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8
                return String(payload, 1 + langCodeLen, payload.size - 1 - langCodeLen, charset)
            }
        }
        // If no well-known text record found, try the raw payload of the first record
        val first   = message.records.firstOrNull()
            ?: throw IllegalStateException("Empty NDEF message")
        val payload = first.payload
        return try {
            val statusByte = payload[0].toInt() and 0xFF
            val langCodeLen = statusByte and 0x3F
            String(payload, 1 + langCodeLen, payload.size - 1 - langCodeLen, Charsets.UTF_8)
        } catch (_: Exception) {
            String(payload, Charsets.UTF_8)
        }
    }
}
