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
    private final String  imageUri;   // URI stringa dell'immagine di copertina, può essere null

    public BlockUiData(int row, int col, String title, boolean hasAudio, String imageUri) {
        this.row      = row;
        this.col      = col;
        this.title    = title;
        this.hasAudio = hasAudio;
        this.imageUri = imageUri;
    }

    public int     getRow()      { return row; }
    public int     getCol()      { return col; }
    public String  getTitle()    { return title; }
    public boolean hasAudio()    { return hasAudio; }
    public String  getImageUri() { return imageUri; }
}