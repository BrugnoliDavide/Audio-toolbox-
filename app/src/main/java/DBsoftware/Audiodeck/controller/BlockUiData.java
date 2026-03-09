package DBsoftware.Audiodeck.controller;

/**
 * Dati UI immutabili per un singolo blocco della griglia.
 * Prodotto dal Controller e consumato dal Presenter per costruire l'HTML.
 */
public final class BlockUiData {

    private final int     row;
    private final int     col;
    private final String  title;
    private final boolean hasAudio;

    public BlockUiData(int row, int col, String title, boolean hasAudio) {
        this.row      = row;
        this.col      = col;
        this.title    = title;
        this.hasAudio = hasAudio;
    }

    public int     getRow()      { return row; }
    public int     getCol()      { return col; }
    public String  getTitle()    { return title; }
    public boolean hasAudio()    { return hasAudio; }
}