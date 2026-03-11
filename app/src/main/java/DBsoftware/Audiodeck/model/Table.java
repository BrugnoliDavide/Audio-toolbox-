package DBsoftware.Audiodeck.model;

/**
 * Model — rappresenta la tabella (griglia) di SoundBlock.
 * Singleton: esiste al più una Table alla volta.
 */
public class Table {

    private static Table instance;

    private final int rows;
    private final int cols;
    private final SoundBlock[][] blocks;

    private Table(int rows, int cols) {
        this.rows   = rows;
        this.cols   = cols;
        this.blocks = new SoundBlock[rows][cols];
        initializeBlocks();
    }

    // ─── Singleton ─────────────────────────────────────────────────────────

    public static synchronized Table getInstance(int rows, int cols) {
        if (instance == null) instance = new Table(rows, cols);
        return instance;
    }

    public static synchronized Table getInstance()     { return instance; }
    public static synchronized boolean hasInstance()   { return instance != null; }
    public static synchronized void resetInstance()    { instance = null; }

    // ─── Inizializzazione ──────────────────────────────────────────────────

    private void initializeBlocks() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                blocks[r][c] = new SoundBlock(0, r, c);
    }

    // ─── Accesso ai blocchi ────────────────────────────────────────────────

    public SoundBlock getBlock(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return null;
        return blocks[row][col];
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }

    // ─── Assegnazione blocco ───────────────────────────────────────────────

    /**
     * Scansiona la griglia in ordine riga-per-riga e restituisce le coordinate
     * del primo SoundBlock vuoto (isEmpty == true).
     *
     * @return int[]{row, col} oppure null se tutti i blocchi sono occupati.
     */
    public synchronized int[] findFirstEmpty() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (blocks[r][c].isEmpty())
                    return new int[]{r, c};
        return null;
    }

    /**
     * Configura il blocco in posizione (row, col) con titolo, percorso audio
     * e URI dell'immagine di copertina.
     *
     * @param imageUri URI stringa dell'immagine (può essere null).
     * @return true se riuscito, false se le coordinate sono fuori range.
     */
    public synchronized boolean assignBlock(int row, int col,
                                            String title,
                                            String audioPath,
                                            String imageUri) {
        SoundBlock block = getBlock(row, col);
        if (block == null) return false;
        block.setTitle(title);
        block.setAudioPath(audioPath);
        block.setImagePath(imageUri);
        return true;
    }

    // ─── Ricerca per ID ────────────────────────────────────────────────────

    public int posX(int id) {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (blocks[r][c].getId() == id) return r;
        return -1;
    }

    public int posY(int id) {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (blocks[r][c].getId() == id) return c;
        return -1;
    }
}