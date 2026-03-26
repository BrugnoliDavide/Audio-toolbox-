package DBsoftware.Audiodeck.model;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Repository per la persistenza della {@link Table} su disco.
 *
 * Utilizza la serializzazione Java standard e scrive il file nella
 * directory privata dell'app ({@code Context.getFilesDir()}), che
 * non richiede permessi di storage.
 *
 * Unico punto d'accesso a lettura e scrittura: per cambiare il formato
 * di persistenza (es. JSON, Room) è sufficiente modificare solo questa classe.
 */
public final class TableRepository {

    private static final String TAG      = "TableRepository";
    private static final String FILENAME = "audiodeck_table.ser";

    private TableRepository() {}

    // ─── Salvataggio ──────────────────────────────────────────────────────

    /**
     * Serializza l'istanza corrente di {@link Table} sul disco.
     * Operazione sincrona: chiamare da un thread di background se
     * la griglia è molto grande.
     *
     * @param context Contesto Android per ricavare la directory privata.
     * @return true se il salvataggio è riuscito, false in caso di errore.
     */
    public static boolean save(Context context) {
        Table table = Table.getInstance();
        if (table == null) {
            Log.w(TAG, "save() chiamato senza un'istanza di Table.");
            return false;
        }

        File file = new File(context.getFilesDir(), FILENAME);
        try (FileOutputStream   fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            oos.writeObject(table);
            Log.d(TAG, "Table salvata in: " + file.getAbsolutePath());
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Errore durante il salvataggio della Table.", e);
            return false;
        }
    }

    // ─── Caricamento ──────────────────────────────────────────────────────

    /**
     * Deserializza la {@link Table} dal disco e la ripristina come Singleton
     * tramite {@link Table#restoreInstance(Table)}.
     *
     * Se il file non esiste o la deserializzazione fallisce, il metodo
     * restituisce false senza modificare lo stato corrente.
     *
     * @param context Contesto Android per ricavare la directory privata.
     * @return true se il caricamento è riuscito, false altrimenti.
     */
    public static boolean load(Context context) {
        File file = new File(context.getFilesDir(), FILENAME);
        if (!file.exists()) {
            Log.d(TAG, "Nessun file di salvataggio trovato: prima esecuzione.");
            return false;
        }

        try (FileInputStream   fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            Table loaded = (Table) ois.readObject();
            Table.restoreInstance(loaded);
            Log.d(TAG, "Table caricata da: " + file.getAbsolutePath()
                    + " (" + loaded.getRows() + "×" + loaded.getCols() + ")");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Errore durante il caricamento della Table. Il file potrebbe "
                    + "essere corrotto o incompatibile con la versione attuale.", e);
            // File corrotto: lo elimina per evitare blocchi ai prossimi avvii
            deleteFile(context);
            return false;
        }
    }

    // ─── Eliminazione ─────────────────────────────────────────────────────

    /**
     * Elimina il file di salvataggio dal disco.
     * Utile per reset completo o in caso di file corrotto.
     *
     * @param context Contesto Android per ricavare la directory privata.
     */
    public static void deleteFile(Context context) {
        File file = new File(context.getFilesDir(), FILENAME);
        if (file.exists() && file.delete()) {
            Log.d(TAG, "File di salvataggio eliminato.");
        }
    }
}