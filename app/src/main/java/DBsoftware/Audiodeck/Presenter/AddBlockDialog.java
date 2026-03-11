package DBsoftware.Audiodeck.Presenter;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import DBsoftware.Audiodeck.controller.TableController;

/**
 * DialogFragment per la creazione di un nuovo SoundBlock.
 *
 * Permette di scegliere titolo, file audio e immagine di copertina
 * tramite il file picker di sistema (ACTION_OPEN_DOCUMENT).
 * Gli URI ottenuti sono persistenti grazie a takePersistableUriPermission.
 *
 * Al momento della conferma delega a
 * {@link TableController#addSoundBlock(String, String, String)}.
 */
public class AddBlockDialog extends DialogFragment {

    public static final String TAG = "AddBlockDialog";

    // ─── URI scelti dall'utente ────────────────────────────────────────────

    private Uri selectedAudioUri = null;
    private Uri selectedImageUri = null;

    // ─── Riferimenti alle view del form ────────────────────────────────────

    private TextView tvAudioName;
    private TextView tvImageName;

    // ─── Launcher picker audio ─────────────────────────────────────────────

    private final ActivityResultLauncher<String[]> audioPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri == null) return;
                        // Permesso persistente: l'URI rimane accessibile dopo il riavvio
                        requireContext().getContentResolver()
                                .takePersistableUriPermission(uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        selectedAudioUri = uri;
                        tvAudioName.setText(resolveDisplayName(uri));
                    });

    // ─── Launcher picker immagine ──────────────────────────────────────────

    private final ActivityResultLauncher<String[]> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri == null) return;
                        requireContext().getContentResolver()
                                .takePersistableUriPermission(uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        selectedImageUri = uri;
                        tvImageName.setText(resolveDisplayName(uri));
                    });

    // ─── Costruzione dialog ────────────────────────────────────────────────

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        int p = dpToPx(16);
        int gap = dpToPx(10);

        // ── Campo titolo ────────────────────────────────────────────────────
        EditText etTitle = new EditText(requireContext());
        etTitle.setHint("Titolo blocco");
        etTitle.setSingleLine(true);

        // ── Sezione audio ───────────────────────────────────────────────────
        tvAudioName = makeStatusLabel("Nessun file audio selezionato");
        Button btnAudio = new Button(requireContext());
        btnAudio.setText("Scegli file audio…");
        btnAudio.setOnClickListener(v ->
                audioPickerLauncher.launch(new String[]{"audio/*"}));

        // ── Sezione immagine ────────────────────────────────────────────────
        tvImageName = makeStatusLabel("Nessuna immagine selezionata (opzionale)");
        Button btnImage = new Button(requireContext());
        btnImage.setText("Scegli immagine copertina…");
        btnImage.setOnClickListener(v ->
                imagePickerLauncher.launch(new String[]{"image/*"}));

        // ── Layout ──────────────────────────────────────────────────────────
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(p, p, p, p);

        layout.addView(etTitle);
        layout.addView(spacer(gap));
        layout.addView(btnAudio);
        layout.addView(tvAudioName);
        layout.addView(spacer(gap));
        layout.addView(btnImage);
        layout.addView(tvImageName);

        // ── Controller condiviso con l'Activity ─────────────────────────────
        TableController controller = new ViewModelProvider(requireActivity())
                .get(TableController.class);

        return new AlertDialog.Builder(requireContext())
                .setTitle("Aggiungi blocco")
                .setView(layout)
                .setPositiveButton("Aggiungi", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();

                    if (TextUtils.isEmpty(title)) {
                        toast("Il titolo non può essere vuoto.");
                        return;
                    }
                    if (selectedAudioUri == null) {
                        toast("Selezionare un file audio.");
                        return;
                    }

                    String imageUriString = (selectedImageUri != null)
                            ? selectedImageUri.toString() : null;

                    boolean ok = controller.addSoundBlock(
                            title,
                            selectedAudioUri.toString(),
                            imageUriString);

                    if (!ok) toast("Nessuno slot libero disponibile.");
                })
                .setNegativeButton("Annulla", null)
                .create();
    }

    // ─── Utilità ───────────────────────────────────────────────────────────

    /**
     * Ricava il nome del file dall'URI tramite ContentResolver.
     * Fallback: ultimo segmento dell'URI.
     */
    private String resolveDisplayName(Uri uri) {
        String name = null;
        try (android.database.Cursor cursor = requireContext().getContentResolver()
                .query(uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME},
                        null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
        } catch (Exception ignored) {}
        return (name != null && !name.isEmpty()) ? name : uri.getLastPathSegment();
    }

    private TextView makeStatusLabel(String hint) {
        TextView tv = new TextView(requireContext());
        tv.setText(hint);
        tv.setTextSize(12f);
        tv.setPadding(0, dpToPx(4), 0, 0);
        tv.setAlpha(0.7f);
        return tv;
    }

    private View spacer(int heightPx) {
        View v = new View(requireContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, heightPx));
        return v;
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}