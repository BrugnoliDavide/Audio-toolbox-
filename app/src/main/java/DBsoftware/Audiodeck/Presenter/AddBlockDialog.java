package DBsoftware.Audiodeck.Presenter;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import DBsoftware.Audiodeck.controller.TableController;

/**
 * DialogFragment per la creazione di un nuovo SoundBlock.
 *
 * Raccoglie titolo e percorso audio dall'utente, poi delega a
 * {@link TableController#addSoundBlock(String, String)}, che trova il
 * primo slot vuoto nella griglia e aggiorna la LiveData.
 * Il Presenter riceve automaticamente l'aggiornamento e ridisegna la griglia.
 */
public class AddBlockDialog extends DialogFragment {

    /** Tag da usare in FragmentManager per evitare istanze duplicate. */
    public static final String TAG = "AddBlockDialog";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        int padding = dpToPx(20);

        // ── Campo titolo ────────────────────────────────────────────────────
        EditText etTitle = new EditText(requireContext());
        etTitle.setHint("Titolo blocco");
        etTitle.setSingleLine(true);

        // ── Campo percorso audio ────────────────────────────────────────────
        EditText etAudioPath = new EditText(requireContext());
        etAudioPath.setHint("Percorso audio (es. /storage/suono.mp3)");
        etAudioPath.setSingleLine(true);

        // ── Layout ──────────────────────────────────────────────────────────
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(padding, padding, padding, padding);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(etTitle);
        layout.addView(etAudioPath);

        // ── Controller condiviso con l'Activity ─────────────────────────────
        TableController controller = new ViewModelProvider(requireActivity())
                .get(TableController.class);

        // ── Costruzione dialog ──────────────────────────────────────────────
        return new AlertDialog.Builder(requireContext())
                .setTitle("Aggiungi blocco")
                .setView(layout)
                .setPositiveButton("Aggiungi", (dialog, which) -> {
                    String title     = etTitle.getText().toString().trim();
                    String audioPath = etAudioPath.getText().toString().trim();

                    if (TextUtils.isEmpty(title)) {
                        Toast.makeText(requireContext(),
                                "Il titolo non può essere vuoto.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(audioPath)) {
                        Toast.makeText(requireContext(),
                                "Inserire un percorso audio.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean ok = controller.addSoundBlock(title, audioPath);
                    if (!ok) {
                        Toast.makeText(requireContext(),
                                "Nessuno slot libero disponibile.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annulla", null)
                .create();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}