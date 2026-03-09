package DBsoftware.Audiodeck;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import DBsoftware.Audiodeck.Presenter.TablePresenter;
import DBsoftware.Audiodeck.controller.TableController;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_ROWS   = "extra_rows";
    public static final String EXTRA_COLS   = "extra_cols";
    private static final int   DEFAULT_ROWS = 4;
    private static final int   DEFAULT_COLS = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ── Barra superiore ────────────────────────────────────────────────
        ImageButton btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> {
            // TODO: aprire dialog per aggiungere/configurare un blocco
            Toast.makeText(this, "Aggiungi blocco", Toast.LENGTH_SHORT).show();
        });

        // ── Controller ────────────────────────────────────────────────────
        int rows = getIntent().getIntExtra(EXTRA_ROWS, DEFAULT_ROWS);
        int cols = getIntent().getIntExtra(EXTRA_COLS, DEFAULT_COLS);

        TableController controller = new ViewModelProvider(this)
                .get(TableController.class);

        // initTable viene chiamato solo alla prima creazione.
        // Alla ricreazione (rotazione) il ViewModel è già inizializzato.
        if (savedInstanceState == null) {
            controller.initTable(rows, cols);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new TablePresenter())
                    .commit();
        }
    }
}