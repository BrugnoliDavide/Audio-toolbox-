# AudioDeck

Applicazione Android per la riproduzione di effetti sonori tramite una griglia di riquadri tattili (SoundBlock). Pensata per uso live, ogni blocco è associato a un file audio che viene riprodotto al tocco.

---

## Indice

- [Architettura](#architettura)
- [Struttura del progetto](#struttura-del-progetto)
- [Flusso dati](#flusso-dati)
- [Componenti principali](#componenti-principali)
- [Aggiunta di un SoundBlock](#aggiunta-di-un-soundblock)
- [Personalizzazione del CSS](#personalizzazione-del-css)
- [Requisiti](#requisiti)
- [Configurazione del progetto](#configurazione-del-progetto)

---

## Architettura

Il progetto segue il pattern **MVCP** (Model – View – Controller – Presenter):

```
┌─────────────┐     LiveData      ┌───────────────────┐
│   Presenter │ ◄──────────────── │    Controller     │
│TablePresenter│                  │  TableController  │
│(Fragment +  │ ──── click ─────► │(AndroidViewModel) │
│  WebView)   │                  └────────┬──────────┘
└─────────────┘                           │
                                          │ read / write
                                 ┌────────▼──────────┐
                                 │       Model       │
                                 │  Table (Singleton)│
                                 │  SoundBlock       │
                                 └───────────────────┘
```

| Livello | Responsabilità |
|---|---|
| **Model** | Struttura dati pura: `Table` (griglia singleton) e `SoundBlock` (singolo blocco). Nessuna dipendenza da Android. |
| **Controller** | `TableController` estende `AndroidViewModel`. Inizializza la griglia, gestisce click, riproduzione audio e aggiunta di nuovi blocchi. Espone lo stato tramite `LiveData`. |
| **Presenter** | `TablePresenter` (Fragment) osserva la `LiveData` e renderizza la griglia come pagina HTML/CSS in una `WebView`. Riceve i click via `JavascriptInterface`. |
| **View** | `MainActivity` + layout XML. Ospita la barra superiore e il Fragment. |

---

## Struttura del progetto

```
app/src/main/
├── java/DBsoftware/Audiodeck/
│   ├── MainActivity.java
│   ├── controller/
│   │   ├── BlockUiData.java         # POJO immutabile per il rendering di un singolo blocco
│   │   ├── TableController.java     # AndroidViewModel: logica e stato
│   │   └── TableUiState.java        # Stati UI: Loading | Ready | Error
│   ├── model/
│   │   ├── SoundBlock.java          # Dati di un blocco (titolo, audio, isEmpty)
│   │   └── Table.java               # Griglia singleton di SoundBlock
│   └── Presenter/
│       ├── CssProvider.java         # Unico punto d'accesso al CSS
│       ├── TablePresenter.java      # Fragment: rendering HTML via WebView
│       └── AddBlockDialog.java      # DialogFragment: form aggiunta blocco
└── assets/
    └── table_grid.css               # Foglio di stile della griglia
```

---

## Flusso dati

### Avvio

```
MainActivity.onCreate()
  └─► controller.initTable(rows, cols)
        └─► Table.getInstance(rows, cols)   // crea il Singleton con blocchi vuoti
        └─► _uiState.setValue(Ready)
              └─► TablePresenter.renderState(Ready)
                    └─► CssProvider.load()  // legge assets/table_grid.css
                    └─► WebView.loadData(HTML + CSS + celle)
```

### Click su un blocco

```
WebView (JS) ── window.Android.onBlockClicked(row, col)
  └─► JsBridge.onBlockClicked()            // thread di background
        └─► controller.onBlockClicked()
              └─► Table.getBlock(row, col)
              └─► MediaPlayer.start(audioPath)
```

### Aggiunta di un nuovo blocco

```
btnAdd (click) ── AddBlockDialog.show()
  └─► utente inserisce titolo + percorso audio
        └─► controller.addSoundBlock(title, audioPath)
              └─► Table.findFirstEmpty()    // primo slot con isEmpty == true
              └─► Table.assignBlock()       // scrive titolo e audioPath
              └─► _uiState.setValue(Ready)  // aggiorna la UI automaticamente
```

---

## Componenti principali

### `Table`

Singleton thread-safe che rappresenta la griglia `rows × cols` di `SoundBlock`.

| Metodo | Descrizione |
|---|---|
| `getInstance(rows, cols)` | Crea il Singleton se non esiste, altrimenti lo restituisce |
| `getInstance()` | Restituisce il Singleton esistente o `null` |
| `findFirstEmpty()` | Ritorna `int[]{row, col}` del primo blocco vuoto, o `null` se piena |
| `assignBlock(row, col, title, audioPath)` | Configura un blocco esistente con titolo e audio |
| `resetInstance()` | Distrugge il Singleton (utile per test) |

### `SoundBlock`

Rappresenta un singolo blocco. Un blocco è considerato **vuoto** (`isEmpty == true`) finché non gli viene associato un `audioPath` non nullo tramite `setAudioPath()`.

### `TableController`

`AndroidViewModel` che sopravvive ai cambi di configurazione (es. rotazione schermo).

| Metodo | Descrizione |
|---|---|
| `initTable(rows, cols)` | Inizializza la griglia ed emette `Ready` sulla LiveData |
| `addSoundBlock(title, audioPath)` | Trova il primo slot vuoto, lo configura e aggiorna la UI |
| `onBlockClicked(row, col)` | Riproduce l'audio del blocco cliccato tramite `MediaPlayer` |
| `getUiState()` | Espone `LiveData<TableUiState>` al Presenter |

### `TablePresenter`

Fragment che costruisce l'HTML della griglia e lo carica in una `WebView`. Il CSS viene iniettato come tag `<style>` inline (letto tramite `CssProvider`). Le dimensioni della griglia (`grid-template-columns/rows`) vengono iniettate come stile inline sull'elemento `#grid`, lasciando il CSS generico e riutilizzabile.

### `CssProvider`

Classe `final` con costruttore privato. Unico punto d'accesso al CSS:

```java
// Per cambiare foglio di stile, modificare solo questa costante:
private static final String CSS_FILE = "table_grid.css";
```

### `AddBlockDialog`

`DialogFragment` con due campi: **titolo** e **percorso audio**. Alla conferma chiama `controller.addSoundBlock()`. Se non ci sono slot liberi mostra un `Toast` informativo.

---

## Aggiunta di un SoundBlock

1. Premere il pulsante **+** nella barra superiore.
2. Compilare il campo **Titolo blocco**.
3. Compilare il campo **Percorso audio** con il percorso assoluto del file (es. `/storage/emulated/0/Music/suono.mp3`).
4. Premere **Aggiungi**.

Il sistema trova automaticamente il primo riquadro libero nella griglia (scansione riga per riga, da sinistra a destra) e lo popola. La griglia si aggiorna immediatamente senza riavviare l'Activity.

Se tutti i riquadri sono occupati, viene mostrato un avviso.

---

## Personalizzazione del CSS

Il file `app/src/main/assets/table_grid.css` controlla interamente l'aspetto della griglia. Può essere modificato liberamente senza toccare alcun file Java.

Le classi disponibili sono:

| Selettore | Descrizione |
|---|---|
| `#grid` | Contenitore principale (display: grid). Le track vengono iniettate inline. |
| `.cell` | Riquadro standard (vuoto o con audio non ancora assegnato). |
| `.cell.has-audio` | Riquadro con audio associato (colore differenziato). |
| `.cell-label` | Testo del titolo all'interno del riquadro. |
| `#error-screen` | Schermata di errore mostrata in caso di fallimento inizializzazione. |

---

## Requisiti

| Requisito | Valore |
|---|---|
| `minSdk` | API 26 (Android 8.0) |
| `compileSdk` / `targetSdk` | API 36 |
| Linguaggio | Java (con file Kotlin per i temi) |
| `AppCompatActivity` | Obbligatorio — il tema deve estendere `Theme.AppCompat` |

### Dipendenze principali (`libs.versions.toml`)

```toml
androidx-appcompat          = "1.7.0"
androidx-fragment           = "1.8.8"
androidx-lifecycle-livedata = "2.10.0"
androidx-lifecycle-viewmodel= "2.10.0"
```

---

## Configurazione del progetto

### Dimensioni della griglia

Il numero di righe e colonne di default è definito in `MainActivity`:

```java
private static final int DEFAULT_ROWS = 4;
private static final int DEFAULT_COLS = 4;
```

Per avviare l'Activity con dimensioni diverse, passare gli extra nell'Intent:

```java
Intent intent = new Intent(context, MainActivity.class);
intent.putExtra(MainActivity.EXTRA_ROWS, 3);
intent.putExtra(MainActivity.EXTRA_COLS, 5);
startActivity(intent);
```

### Tema

Il tema dell'applicazione deve obbligatoriamente estendere `Theme.AppCompat` o un suo discendente. In `res/values/themes.xml`:

```xml
<style name="Theme.AudioDeck" parent="Theme.AppCompat.Light.NoActionBar" />
```

### Permessi

Per la lettura di file audio da storage esterno è necessario dichiarare in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

Su Android 13+ (API 33) sostituire con:

```xml
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
```

La gestione runtime del permesso è a carico dello sviluppatore e non è ancora implementata in questa versione.