package DBsoftware.Audiodeck.controller;

import java.util.List;

/**
 * Equivalente Java di una sealed class Kotlin.
 *
 * Il costruttore privato impedisce sottoclassi esterne;
 * i soli stati validi sono le tre inner class statiche:
 * {@link Loading}, {@link Ready}, {@link Error}.
 *
 * Utilizzo nel Presenter:
 * <pre>
 *   if (state instanceof TableUiState.Ready) {
 *       TableUiState.Ready ready = (TableUiState.Ready) state;
 *       // usa ready.getBlocks(), ready.getRows(), ready.getCols()
 *   } else if (state instanceof TableUiState.Loading) { ... }
 *   else if (state instanceof TableUiState.Error)   { ... }
 * </pre>
 */
public abstract class TableUiState {

    // Costruttore package-private: solo le inner class qui sotto possono estenderla.
    TableUiState() {}

    // ─── Loading ───────────────────────────────────────────────────────────

    /** La Table non è ancora stata costruita. */
    public static final class Loading extends TableUiState {
        public static final Loading INSTANCE = new Loading();
        private Loading() {}
    }

    // ─── Ready ─────────────────────────────────────────────────────────────

    /** La Table è pronta: contiene righe, colonne e lista piatta dei blocchi. */
    public static final class Ready extends TableUiState {

        private final int               rows;
        private final int               cols;
        private final List<BlockUiData> blocks;

        public Ready(int rows, int cols, List<BlockUiData> blocks) {
            this.rows   = rows;
            this.cols   = cols;
            this.blocks = blocks;
        }

        public int               getRows()   { return rows; }
        public int               getCols()   { return cols; }
        public List<BlockUiData> getBlocks() { return blocks; }

        /**
         * Restituisce il blocco alla posizione (row, col),
         * oppure null se le coordinate sono fuori range.
         */
        public BlockUiData blockAt(int row, int col) {
            if (row < 0 || row >= rows || col < 0 || col >= cols) return null;
            int index = row * cols + col;
            if (index >= blocks.size()) return null;
            return blocks.get(index);
        }
    }

    // ─── Error ─────────────────────────────────────────────────────────────

    /** Si è verificato un errore durante l'inizializzazione. */
    public static final class Error extends TableUiState {

        private final String    message;
        private final Throwable cause;   // nullable

        public Error(String message, Throwable cause) {
            this.message = message;
            this.cause   = cause;
        }

        public Error(String message) {
            this(message, null);
        }

        public String    getMessage() { return message; }
        public Throwable getCause()   { return cause; }
    }
}