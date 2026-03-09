package DBsoftware.Audiodeck.model;

/**
 * Model — rappresenta la tabella (griglia) di SoundBlock.
 * Segue il pattern Singleton: esiste al più una Table alla volta.
 * La prima chiamata a getInstance(rows, cols) la istanzia;
 * le chiamate successive restituiscono l'istanza esistente
 * ignorando i parametri dimensionali.
 */
public class Table {

    private static Table instance;

    private final int rows;
    private final int cols;
    private final SoundBlock[][] blocks;

    // ─── Costruttore privato (Singleton) ───────────────────────────────────

    private Table(int rows, int cols) {
        this.rows   = rows;
        this.cols   = cols;
        this.blocks = new SoundBlock[rows][cols];
        initializeBlocks();
    }

    // ─── Accesso al Singleton ──────────────────────────────────────────────

    /**
     * Restituisce l'istanza esistente oppure ne crea una nuova
     * con le dimensioni specificate.
     *
     * @param rows numero di righe (ignorato se l'istanza esiste già)
     * @param cols numero di colonne (ignorato se l'istanza esiste già)
     * @return istanza Singleton di Table
     */
    public static synchronized Table getInstance(int rows, int cols) {
        if (instance == null) {
            instance = new Table(rows, cols);
        }
        return instance;
    }

    /**
     * Restituisce l'istanza esistente senza crearla.
     * Può essere null se getInstance(rows, cols) non è ancora stato chiamato.
     */
    public static synchronized Table getInstance() {
        return instance;
    }

    /** Indica se il Singleton è già stato creato. */
    public static synchronized boolean hasInstance() {
        return instance != null;
    }

    /** Distrugge il Singleton (utile per test o reset completo). */
    public static synchronized void resetInstance() {
        instance = null;
    }

    // ─── Inizializzazione ──────────────────────────────────────────────────

    private void initializeBlocks() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                blocks[r][c] = new SoundBlock(0, r, c);
            }
        }
    }

    // ─── Accesso ai blocchi ───────────────────────────────────────────────

    /**
     * Restituisce il SoundBlock alla posizione (row, col).
     * Restituisce null se le coordinate sono fuori dai limiti.
     */
    public SoundBlock getBlock(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return null;
        return blocks[row][col];
    }

    // ─── Getter dimensioni ─────────────────────────────────────────────────

    public int getRows() { return rows; }
    public int getCols() { return cols; }

    // ─── Ricerca per ID ────────────────────────────────────────────────────

    /** Restituisce la riga del blocco con l'ID specificato, -1 se non trovato. */
    public int posX(int id) {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (blocks[r][c].getId() == id) return r;
        return -1;
    }

    /** Restituisce la colonna del blocco con l'ID specificato, -1 se non trovato. */
    public int posY(int id) {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (blocks[r][c].getId() == id) return c;
        return -1;
    }
}