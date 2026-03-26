package DBsoftware.Audiodeck.controller;

import android.app.Application;
import android.media.MediaPlayer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import DBsoftware.Audiodeck.model.SoundBlock;
import DBsoftware.Audiodeck.model.Table;
import DBsoftware.Audiodeck.model.TableRepository;

/**
 * Controller MVCP.
 *
 * Responsabilità:
 * - Caricare la {@link Table} dal disco tramite {@link TableRepository}
 *   al primo avvio; crearne una nuova se non esiste alcun salvataggio.
 * - Esporre lo stato UI tramite {@link LiveData} al Presenter.
 * - Salvare la Table su disco dopo ogni modifica (addSoundBlock).
 * - Gestire la riproduzione audio al click di un blocco.
 */
public class TableController extends AndroidViewModel {

    private static final String TAG = "TableController";

    // ─── Stato UI ──────────────────────────────────────────────────────────

    private final MutableLiveData<TableUiState> _uiState =
            new MutableLiveData<>(TableUiState.Loading.INSTANCE);

    public LiveData<TableUiState> getUiState() {
        return _uiState;
    }

    // ─── Player audio ──────────────────────────────────────────────────────

    private MediaPlayer mediaPlayer;

    // ─── Costruttore ───────────────────────────────────────────────────────

    public TableController(@NonNull Application application) {
        super(application);
    }

    // ─── Inizializzazione ──────────────────────────────────────────────────

    /**
     * Inizializza la griglia con la seguente priorità:
     *
     * 1. Carica la Table serializzata dal disco ({@link TableRepository#load}).
     * 2. Se non esiste un salvataggio, crea una nuova Table vuota con
     *    le dimensioni fornite.
     *
     * @param rows Numero di righe da usare se si crea una nuova Table.
     * @param cols Numero di colonne da usare se si crea una nuova Table.
     */
    public void initTable(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            _uiState.setValue(new TableUiState.Error(
                    "Dimensioni non valide: rows=" + rows + ", cols=" + cols));
            return;
        }
        try {
            boolean loaded = TableRepository.load(getApplication());
            if (!loaded) {
                // Prima esecuzione o file corrotto: crea una Table vuota.
                Table.getInstance(rows, cols);
                Log.d(TAG, "Nuova Table creata: " + rows + "×" + cols);
            }
            _uiState.setValue(buildReadyState(Table.getInstance()));
        } catch (Exception e) {
            Log.e(TAG, "Errore inizializzazione Table: " + e.getMessage());
            _uiState.setValue(new TableUiState.Error(
                    "Impossibile inizializzare la tabella.", e));
        }
    }

    // ─── Aggiunta di un nuovo SoundBlock ──────────────────────────────────

    /**
     * Trova il primo blocco vuoto, lo configura e persiste la Table su disco.
     *
     * @param title    Titolo da visualizzare nel riquadro.
     * @param audioUri URI stringa del file audio.
     * @param imageUri URI stringa dell'immagine di copertina (può essere null).
     * @return true se il blocco è stato assegnato, false se la griglia è piena.
     */
    public boolean addSoundBlock(String title, String audioUri, String imageUri) {
        Table table = Table.getInstance();
        if (table == null) {
            Log.w(TAG, "addSoundBlock chiamato prima di initTable.");
            return false;
        }

        int[] pos = table.findFirstEmpty();
        if (pos == null) {
            Log.w(TAG, "Nessun blocco vuoto disponibile.");
            return false;
        }

        boolean ok = table.assignBlock(pos[0], pos[1], title, audioUri, imageUri);
        if (ok) {
            Log.d(TAG, "Blocco assegnato in (" + pos[0] + "," + pos[1] + "): " + title);
            persistAndNotify(table);
        }
        return ok;
    }

    // ─── Gestione click ────────────────────────────────────────────────────

    public void onBlockClicked(int row, int col) {
        Table table = Table.getInstance();
        if (table == null) {
            Log.w(TAG, "onBlockClicked chiamato prima di initTable.");
            return;
        }
        SoundBlock block = table.getBlock(row, col);
        if (block == null) {
            Log.w(TAG, "Blocco non trovato in (" + row + "," + col + ").");
            return;
        }
        String audioUri = block.getAudioPath();
        if (audioUri == null || audioUri.isBlank()) {
            Log.d(TAG, "Blocco (" + row + "," + col + ") senza audio.");
            return;
        }
        playSound(audioUri);
    }

    // ─── Persistenza + notifica UI ─────────────────────────────────────────

    /**
     * Salva la Table su disco e aggiorna la LiveData.
     * Operazione atomica: la UI viene aggiornata solo se il salvataggio
     * ha successo o comunque per non bloccare l'utente (il log segnala l'errore).
     */
    private void persistAndNotify(Table table) {
        boolean saved = TableRepository.save(getApplication());
        if (!saved) {
            Log.e(TAG, "Salvataggio fallito. Le modifiche potrebbero non sopravvivere al riavvio.");
        }
        _uiState.setValue(buildReadyState(table));
    }

    // ─── Costruzione stato Ready ───────────────────────────────────────────

    private TableUiState.Ready buildReadyState(Table table) {
        List<BlockUiData> blocks = new ArrayList<>(table.getRows() * table.getCols());
        for (int r = 0; r < table.getRows(); r++) {
            for (int c = 0; c < table.getCols(); c++) {
                SoundBlock block = table.getBlock(r, c);
                String title     = (block != null) ? block.getTitle()     : r + "," + c;
                String audio     = (block != null) ? block.getAudioPath() : null;
                String image     = (block != null) ? block.getImagePath() : null;
                boolean hasAudio = (audio != null && !audio.isBlank());
                blocks.add(new BlockUiData(r, c, title, hasAudio, image));
            }
        }
        return new TableUiState.Ready(table.getRows(), table.getCols(), blocks);
    }

    // ─── Riproduzione audio ────────────────────────────────────────────────

    private void playSound(String uriString) {
        try {
            if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
            mediaPlayer = new MediaPlayer();
            android.net.Uri uri = android.net.Uri.parse(uriString);
            mediaPlayer.setDataSource(getApplication(), uri);
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
            });
        } catch (Exception e) {
            Log.e(TAG, "Errore riproduzione '" + uriString + "': " + e.getMessage());
            if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
        }
    }

    // ─── Ciclo di vita ─────────────────────────────────────────────────────

    @Override
    protected void onCleared() {
        super.onCleared();
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
        Log.d(TAG, "TableController distrutto, MediaPlayer rilasciato.");
    }
}