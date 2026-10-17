# NFC Reader

Eine minimale Android-App (Kotlin), die NFC-Tags/Karten ausliest und technische
Details anzeigt: UID, unterstützte Technologien, ATQA/SAK, ISO-DEP-Infos,
MIFARE-Typ und NDEF-Inhalt (falls vorhanden).

## Einrichten

1. **Android Studio** öffnen (aktuelle Version, z. B. Android Studio Jellyfish
   oder neuer empfohlen).
2. `File → Open...` und diesen Ordner (`NfcReader`) auswählen.
3. Android Studio generiert beim ersten Öffnen automatisch den Gradle-Wrapper
   (`gradlew`/`gradlew.bat` + `gradle-wrapper.jar`). Das kann beim ersten Sync
   einen Moment dauern.
4. Ein echtes Android-Gerät mit NFC per USB anschließen (USB-Debugging muss
   aktiviert sein). **Der Emulator unterstützt kein NFC.**
5. Auf "Run" (▶) klicken.

## Nutzung

- App öffnen, Hinweistext "Bereit..." erscheint.
- Karte an die Rückseite des Telefons halten (dort sitzt meist die NFC-Antenne).
- Die App zeigt UID, Technologie-Liste und Detailinfos an.

## Was dir die Ausgabe sagt (bezogen auf deine Gym-Karte)

- **"IsoDep" taucht in der Technologie-Liste auf UND im IsoDep-Block steht
  ein Hinweis "APDU-fähig"** → Das Zutrittssystem spricht ISO-DEP. Eine
  HCE-Emulation (Host Card Emulation) wäre technisch grundsätzlich möglich,
  sofern das Zutrittssystem tatsächlich per APDU prüft und nicht nur die UID
  abgleicht.
- **"MIFARE Classic" wird erkannt** → Die Karte nutzt eine feste Hardware-UID
  und eigene Verschlüsselung (Crypto1). Das lässt sich auf normalen
  Android-Smartphones per Software **nicht** nachbilden – unabhängig davon,
  welche App man verwendet.
- **Nur "NfcA" ohne IsoDep, mit MIFARE Classic SAK (0x08/0x18 o. Ä.)** →
  Reine UID-Karte, sehr wahrscheinlich das, was bei John Reed/RSG-Group-Studios
  verbaut ist.

Kopier dir die Ausgabe (oben rechts im Text antippen und halten zum Markieren,
oder Screenshot) und schick sie mir – dann kann ich dir sagen, ob und wie eine
Emulation überhaupt infrage kommt.

## APK automatisch über GitHub bauen lassen (ohne Android Studio)

Das Projekt enthält einen fertigen Workflow (`.github/workflows/build.yml`),
der dir bei jedem Push automatisch eine installierbare `app-debug.apk` baut.

1. Auf [github.com](https://github.com) ein kostenloses Konto anlegen (falls
   noch nicht vorhanden) und ein **neues, leeres Repository** erstellen
   (z. B. `nfc-reader`) – ohne README/License beim Erstellen ankreuzen.
2. Diesen entpackten `NfcReader`-Ordner in das neue Repo hochladen. Am
   einfachsten per Browser: Auf der leeren Repo-Seite auf
   **"uploading an existing file"** klicken, den kompletten Inhalt des
   `NfcReader`-Ordners (inkl. `.github`-Ordner, der ist standardmäßig
   unsichtbar im Dateimanager – ggf. versteckte Dateien anzeigen lassen!)
   per Drag & Drop reinziehen und committen.
   *(Alternative mit Git-Kenntnissen: `git init`, `git add .`,
   `git commit -m "init"`, `git remote add origin <dein-repo-url>`,
   `git push -u origin main`)*
3. Im Repo oben auf den Reiter **"Actions"** klicken. Der Workflow
   "Build APK" startet automatisch nach dem Push (dauert 2-4 Minuten).
4. Wenn der Lauf grün ist (✅), draufklicken → ganz unten bei
   **"Artifacts"** liegt `NfcReader-debug-apk` zum Herunterladen als ZIP.
5. ZIP entpacken → `app-debug.apk` aufs Handy übertragen (z. B. per
   Google Drive, E-Mail an dich selbst, oder direkt vom Handy-Browser aus
   bei Artifacts herunterladen) → installieren. Dabei ggf. einmalig
   "Installation aus unbekannten Quellen" für den Browser/die App erlauben,
   die die Datei öffnet.

Der Workflow lässt sich auch jederzeit manuell erneut anstoßen: Reiter
"Actions" → "Build APK" → "Run workflow".

## Berechtigungen

Die App benötigt nur die NFC-Berechtigung. Es werden keine Daten versendet,
gespeichert oder sonst irgendwo hin übertragen – alles bleibt lokal auf dem
Bildschirm.
