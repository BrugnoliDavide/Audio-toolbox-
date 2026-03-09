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

/**
 * Controller MVCP.
 *
 * Responsabilità:
 * - Istanziare il Singleton {@link Table} se non esiste ancora.
 * - Esporre lo stato UI tramite {@link LiveData} al Presenter.
 * - Gestire la riproduzione audio al click di un blocco.
 *
 * Estende {@link AndroidViewModel} per sopravvivere ai cambi di
 * configurazione (es. rotazione schermo) e per disporre del contesto
 * applicativo necessario a {@link MediaPlayer}.
 */
public class TableController extends AndroidViewModel {

    private static final String TAG = "TableController";

    // ─── Stato UI ──────────────────────────────────────────────────────────

    private final MutableLiveData<TableUiState> _uiState =
            new MutableLiveData<>(TableUiState.Loading.INSTANCE);

    /**
     * LiveData osservabile dal Presenter.
     * Valore iniziale: {@link TableUiState.Loading}.
     * Transita in {@link TableUiState.Ready} o {@link TableUiState.Error}
     * dopo la chiamata a {@link #initTable(int, int)}.
     */
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
     * Inizializza la {@link Table} se non esiste ancora.
     * Se il Singleton è già presente, utilizza le dimensioni esistenti.
     *
     * @param rows Numero di righe desiderate.
     * @param cols Numero di colonne desiderate.
     */
    public void initTable(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            _uiState.setValue(new TableUiState.Error(
                    "Dimensioni non valide: rows=" + rows + ", cols=" + cols));
            return;
        }

        try {
            Table table = Table.getInstance(rows, cols);
            Log.d(TAG, "Table inizializzata: " + table.getRows() + "×" + table.getCols());
            _uiState.setValue(buildReadyState(table));
        } catch (Exception e) {
            Log.e(TAG, "Errore inizializzazione Table: " + e.getMessage());
            _uiState.setValue(new TableUiState.Error(
                    "Impossibile inizializzare la tabella.", e));
        }
    }

    // ─── Gestione click ────────────────────────────────────────────────────

    /**
     * Chiamato dal Presenter (tramite JavascriptInterface) quando l'utente
     * tocca il riquadro alla posizione (row, col).
     * Se al blocco è associato un percorso audio valido, avvia la riproduzione.
     *
     * @param row Indice di riga del blocco cliccato.
     * @param col Indice di colonna del blocco cliccato.
     */
    public void onBlockClicked(int row, int col) {
        Table table = Table.getInstance();
        if (table == null) {
            Log.w(TAG, "onBlockClicked chiamato prima di initTable.");
            return;
        }

        SoundBlock block = table.getBlock(row, col);
        if (block == null) {
            Log.w(TAG, "Blocco non trovato in posizione (" + row + "," + col + ").");
            return;
        }

        String audioPath = block.getAudioPath();
        if (audioPath == null || audioPath.isBlank()) {
            Log.d(TAG, "Blocco (" + row + "," + col + ") senza audio associato.");
            return;
        }

        Log.d(TAG, "Riproduzione audio per blocco (" + row + "," + col + "): " + audioPath);
        playSound(audioPath);
    }

    // ─── Costruzione stato Ready ───────────────────────────────────────────

    private TableUiState.Ready buildReadyState(Table table) {
        List<BlockUiData> blocks = new ArrayList<>(table.getRows() * table.getCols());

        for (int r = 0; r < table.getRows(); r++) {
            for (int c = 0; c < table.getCols(); c++) {
                SoundBlock block = table.getBlock(r, c);
                String title    = (block != null) ? block.getTitle()    : r + "," + c;
                String audio    = (block != null) ? block.getAudioPath(): null;
                boolean hasAudio = (audio != null && !audio.isBlank());
                blocks.add(new BlockUiData(r, c, title, hasAudio));
            }
        }

        return new TableUiState.Ready(table.getRows(), table.getCols(), blocks);
    }

    // ─── Riproduzione audio ────────────────────────────────────────────────

    private void playSound(String path) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
            });
        } catch (Exception e) {
            Log.e(TAG, "Errore riproduzione '" + path + "': " + e.getMessage());
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    }

    // ─── Ciclo di vita ─────────────────────────────────────────────────────

    @Override
    protected void onCleared() {
        super.onCleared();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        Log.d(TAG, "TableController distrutto, MediaPlayer rilasciato.");
    }
}