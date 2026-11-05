# Media Projector

App Android (tablet, Android 16 / API 36) per riprodurre una lista di video **MP4** e audio **MP3**
su uno **schermo esterno** (proiettore HDMI in modalità schermo esteso), controllata dal tablet.

## Funzioni
- Pannello con **PLAY, PAUSE, STOP, PREVIOUS, NEXT, EXIT** (EXIT chiede conferma).
- Il video va a schermo intero sul proiettore; sul tablet restano controlli, lista e barra di avanzamento (con seek).
- Se non c'è nessuno schermo esterno, l'anteprima del video appare sul tablet.
- Lista modificabile: **aggiungi** (selezione multipla), **rimuovi**, **sposta su/giù**, **svuota**, ▶ per partire da una riga.
- La lista viene salvata e ricaricata all'avvio (permessi permanenti sui file, nessun permesso storage richiesto).
- Opzione "Avanza automaticamente al file successivo" (spenta di default).
- Schermo del tablet sempre acceso; tasto Indietro disabilitato (si esce solo con EXIT).

## Build
1. Carica la cartella su GitHub.
2. Su Codemagic collega il repository: il file `codemagic.yaml` genera il Gradle wrapper e compila.
   - workflow `debug-apk`: APK debug, pronto da installare.
   - workflow `release-apk`: APK firmato (carica prima un keystore e correggi il nome in `android_signing`).
3. Scarica l'APK dagli artifacts e installalo sul tablet.

In locale (Android Studio): apri la cartella e premi Run. Richiede JDK 17.

## Note HDMI
Lo schermo esterno deve essere in modalità **estesa** (non mirroring). Il tablet deve supportare
l'uscita video esterna (HDMI nativo, adattatore USB-C → HDMI con DisplayPort Alt Mode, ecc.).
L'audio segue l'instradamento di sistema (HDMI, jack, USB, Bluetooth).
