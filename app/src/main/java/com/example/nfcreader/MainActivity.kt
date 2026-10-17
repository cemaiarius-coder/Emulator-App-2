package com.example.nfcreader

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        resultText = findViewById(R.id.resultText)
        findViewById<android.widget.Button>(R.id.clearButton).setOnClickListener {
            resultText.text = ""
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            statusText.text = "Dieses Gerät unterstützt kein NFC."
        } else if (!nfcAdapter!!.isEnabled) {
            statusText.text = "NFC ist deaktiviert. Bitte in den Einstellungen aktivieren."
        }

        // Falls die App über einen TECH_DISCOVERED-Intent gestartet wurde
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.let { adapter ->
            if (adapter.isEnabled) {
                val pendingIntent = PendingIntent.getActivity(
                    this, 0,
                    Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_MUTABLE
                )
                val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
                val techLists = arrayOf(
                    arrayOf(NfcA::class.java.name),
                    arrayOf(NfcB::class.java.name),
                    arrayOf(NfcF::class.java.name),
                    arrayOf(NfcV::class.java.name),
                    arrayOf(IsoDep::class.java.name),
                    arrayOf(MifareClassic::class.java.name),
                    arrayOf(MifareUltralight::class.java.name),
                    arrayOf(Ndef::class.java.name)
                )
                adapter.enableForegroundDispatch(this, pendingIntent, filters, techLists)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        if (tag != null) {
            readTag(tag)
        }
    }

    private fun readTag(tag: Tag) {
        val sb = StringBuilder()
        val uidBytes = tag.id
        val uidHex = bytesToHex(uidBytes)

        sb.appendLine("=== Tag erkannt ===")
        sb.appendLine("UID: $uidHex")
        sb.appendLine("UID-Länge: ${uidBytes.size} Byte")
        sb.appendLine()
        sb.appendLine("Unterstützte Technologien:")
        tag.techList.forEach { sb.appendLine("  - ${it.substringAfterLast('.')}") }
        sb.appendLine()

        // NfcA Details (ATQA / SAK) - relevant für MIFARE-Erkennung
        NfcA.get(tag)?.let { nfcA ->
            try {
                sb.appendLine("--- NfcA (ISO 14443 Typ A) ---")
                sb.appendLine("ATQA: ${bytesToHex(nfcA.atqa)}")
                sb.appendLine("SAK: 0x${Integer.toHexString(nfcA.sak.toInt())}")
                sb.appendLine()
            } catch (e: Exception) {
                sb.appendLine("NfcA-Fehler: ${e.message}")
            }
        }

        NfcB.get(tag)?.let {
            sb.appendLine("--- NfcB (ISO 14443 Typ B) erkannt ---")
            sb.appendLine()
        }

        NfcF.get(tag)?.let { nfcF ->
            sb.appendLine("--- NfcF (FeliCa) ---")
            sb.appendLine("Manufacturer: ${bytesToHex(nfcF.manufacturer)}")
            sb.appendLine("System Code: ${bytesToHex(nfcF.systemCode)}")
            sb.appendLine()
        }

        NfcV.get(tag)?.let { nfcV ->
            sb.appendLine("--- NfcV (ISO 15693) ---")
            sb.appendLine("DSF-ID: ${nfcV.dsfId}")
            sb.appendLine("Response-Flags: ${nfcV.responseFlags}")
            sb.appendLine()
        }

        // ISO-DEP: zeigt an, ob APDU-Kommunikation möglich ist (wichtig für dein Zugangssystem!)
        IsoDep.get(tag)?.let { isoDep ->
            try {
                isoDep.connect()
                sb.appendLine("--- IsoDep (ISO 14443-4, APDU-fähig) ---")
                sb.appendLine("Historical Bytes: ${bytesToHex(isoDep.historicalBytes ?: ByteArray(0))}")
                sb.appendLine("Hi-Layer Response: ${bytesToHex(isoDep.hiLayerResponse ?: ByteArray(0))}")
                sb.appendLine("Max Transceive Length: ${isoDep.maxTransceiveLength}")
                sb.appendLine("→ Diese Karte spricht ISO-DEP/APDU. HCE-Emulation wäre technisch möglich.")
                isoDep.close()
            } catch (e: Exception) {
                sb.appendLine("IsoDep-Verbindung fehlgeschlagen: ${e.message}")
            }
            sb.appendLine()
        }

        // MIFARE Classic
        MifareClassic.get(tag)?.let { mc ->
            sb.appendLine("--- MIFARE Classic ---")
            sb.appendLine("Typ: ${mifareTypeToString(mc.type)}")
            sb.appendLine("Größe: ${mc.size} Byte")
            sb.appendLine("Sektoren: ${mc.sectorCount}")
            sb.appendLine("Blöcke: ${mc.blockCount}")
            sb.appendLine("→ Diese Karte nutzt feste UID + Crypto1. Software-HCE-Emulation auf")
            sb.appendLine("   handelsüblichen Android-Geräten ist NICHT möglich.")
            sb.appendLine()
        }

        // MIFARE Ultralight
        MifareUltralight.get(tag)?.let { mu ->
            sb.appendLine("--- MIFARE Ultralight ---")
            sb.appendLine("Typ: ${if (mu.type == MifareUltralight.TYPE_ULTRALIGHT_C) "Ultralight C" else "Ultralight"}")
            sb.appendLine()
        }

        // NDEF-Inhalt, falls vorhanden
        Ndef.get(tag)?.let { ndef ->
            try {
                sb.appendLine("--- NDEF ---")
                sb.appendLine("Typ: ${ndef.type}")
                sb.appendLine("Beschreibbar: ${ndef.isWritable}")
                sb.appendLine("Max. Größe: ${ndef.maxSize} Byte")
                val message: NdefMessage? = ndef.cachedNdefMessage
                if (message != null) {
                    sb.appendLine("Anzahl Records: ${message.records.size}")
                    message.records.forEachIndexed { i, record ->
                        sb.appendLine("  Record $i: TNF=${record.tnf}, Typ=${bytesToHex(record.type)}")
                    }
                } else {
                    sb.appendLine("Keine NDEF-Nachricht auf dem Tag.")
                }
                sb.appendLine()
            } catch (e: Exception) {
                sb.appendLine("NDEF-Lesefehler: ${e.message}")
            }
        }

        resultText.text = sb.toString()
        statusText.text = "Tag gelesen um ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())}"
        Toast.makeText(this, "Tag gelesen", Toast.LENGTH_SHORT).show()
    }

    private fun mifareTypeToString(type: Int): String = when (type) {
        MifareClassic.TYPE_CLASSIC -> "Classic"
        MifareClassic.TYPE_PLUS -> "Plus"
        MifareClassic.TYPE_PRO -> "Pro"
        else -> "Unbekannt"
    }

    private fun bytesToHex(bytes: ByteArray): String {
        if (bytes.isEmpty()) return "(leer)"
        return bytes.joinToString(" ") { String.format("%02X", it) }
    }
}
