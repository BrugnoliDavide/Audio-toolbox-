package DBsoftware.Audiodeck.Presenter;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Unico punto d'accesso al foglio di stile della griglia.
 *
 * Per cambiare CSS è sufficiente modificare {@link #CSS_FILE}
 * (o il file stesso in assets/) senza toccare nessun altro file.
 */
public final class CssProvider {

    /** Nome del file CSS in {@code app/src/main/assets/}. */
    private static final String CSS_FILE = "table_grid.css";

    private CssProvider() {}

    /**
     * Legge e restituisce il contenuto del CSS come stringa.
     *
     * @param context Contesto Android necessario per accedere agli assets.
     * @return Contenuto del file CSS, pronto per essere iniettato in un tag {@code <style>}.
     * @throws IOException Se il file non esiste o non è leggibile.
     */
    public static String load(Context context) throws IOException {
        try (InputStream is = context.getAssets().open(CSS_FILE)) {
            byte[] buffer = new byte[is.available()];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }
}