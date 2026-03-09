package DBsoftware.Audiodeck.model;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Model — rappresenta un singolo blocco sonoro nella tabella.
 * Implementa Serializable per eventuale persistenza futura.
 */
public class SoundBlock implements Serializable {

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);

    private final int id;
    private String title;
    private String imagePath;
    private String audioPath;
    private boolean isEmpty;

    /** Costruttore di default: blocco vuoto con ID auto-generato. */
    public SoundBlock() {
        this.id        = ID_COUNTER.incrementAndGet();
        this.title     = "Block " + this.id;
        this.imagePath = null;
        this.audioPath = null;
        this.isEmpty   = true;
    }

    /** Costruttore posizionale: blocco vuoto con etichetta riga/colonna. */
    public SoundBlock(int ignoredId, int row, int col) {
        this.id        = ID_COUNTER.incrementAndGet();
        this.title     = row + "," + col;
        this.imagePath = null;
        this.audioPath = null;
        this.isEmpty   = true;
    }

    /** Costruttore completo: blocco pre-configurato con audio. */
    public SoundBlock(String title, String imagePath, String audioPath) {
        this.id        = ID_COUNTER.incrementAndGet();
        this.title     = title;
        this.imagePath = imagePath;
        this.audioPath = audioPath;
        this.isEmpty   = (audioPath == null || audioPath.isBlank());
    }

    // ─── Getters ────────────────────────────────────────────────────────────

    public int getId()          { return id; }
    public String getTitle()    { return title; }
    public String getImagePath(){ return imagePath; }
    public String getAudioPath(){ return audioPath; }
    public boolean isEmpty()    { return isEmpty; }

    // ─── Setters ────────────────────────────────────────────────────────────

    public void setTitle(String title)         { this.title = title; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
        this.isEmpty   = (audioPath == null || audioPath.isBlank());
    }

    public void clearAudio() {
        this.audioPath = null;
        this.isEmpty   = true;
    }

    // ─── Utilità ────────────────────────────────────────────────────────────

    /** Compatibilità con il codice precedente. */
    public int returnId() { return id; }
}