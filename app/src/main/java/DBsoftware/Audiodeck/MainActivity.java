package DBsoftware.Audiodeck;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import DBsoftware.Audiodeck.Presenter.AddBlockDialog;
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

        // ── Controller ────────────────────────────────────────────────────
        int rows = getIntent().getIntExtra(EXTRA_ROWS, DEFAULT_ROWS);
        int cols = getIntent().getIntExtra(EXTRA_COLS, DEFAULT_COLS);

        TableController controller = new ViewModelProvider(this)
                .get(TableController.class);

        if (savedInstanceState == null) {
            controller.initTable(rows, cols);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new TablePresenter())
                    .commit();
        }

        // ── Barra superiore: bottone + ────────────────────────────────────
        ImageButton btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> {
            // Apre il dialog solo se non è già visibile (evita duplicati da click rapidi)
            if (getSupportFragmentManager().findFragmentByTag(AddBlockDialog.TAG) == null) {
                new AddBlockDialog()
                        .show(getSupportFragmentManager(), AddBlockDialog.TAG);
            }
        });
    }
}