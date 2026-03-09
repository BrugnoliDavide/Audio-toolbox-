package DBsoftware.Audiodeck.Presenter;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.IOException;
import java.util.List;

import DBsoftware.Audiodeck.controller.BlockUiData;
import DBsoftware.Audiodeck.controller.TableController;
import DBsoftware.Audiodeck.controller.TableUiState;

/**
 * Presenter MVCP — Fragment che presenta la griglia come pagina HTML/CSS
 * all'interno di una {@link WebView}.
 *
 * Responsabilità:
 * - Osservare la {@link LiveData} del {@link TableController}.
 * - Costruire l'HTML della griglia iniettando righe, colonne e dati dei blocchi.
 * - Caricare il foglio di stile {@code assets/table_grid.css}.
 * - Esporre un {@link JavascriptInterface} (bridge JS→Java) per ricevere
 *   i click delle celle e delegarli al Controller.
 */
public class TablePresenter extends Fragment {

    private WebView         webView;
    private TableController controller;

    // ─── Ciclo di vita Fragment ────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controller = new ViewModelProvider(requireActivity())
                .get(TableController.class);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Creiamo la WebView programmaticamente: nessun layout XML necessario.
        webView = new WebView(requireContext());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        // Necessario per caricare file da assets/ tramite file:///android_asset/
        settings.setAllowFileAccess(true);

        // Bridge: metodi annotati @JavascriptInterface sono chiamabili da JS
        // con window.Android.<metodo>()
        webView.addJavascriptInterface(new JsBridge(), "Android");

        // La WebView occupa l'intera area del Fragment
        FrameLayout root = new FrameLayout(requireContext());
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Osserva lo stato del Controller e aggiorna la WebView ad ogni emissione.
        controller.getUiState().observe(getViewLifecycleOwner(), this::renderState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }

    // ─── Rendering ─────────────────────────────────────────────────────────

    private void renderState(TableUiState state) {
        if (webView == null) return;

        if (state instanceof TableUiState.Loading) {
            // Durante il loading mostriamo una pagina minima;
            // in genere questo stato è brevissimo.
            webView.loadData(
                    buildLoadingHtml(),
                    "text/html",
                    "UTF-8");

        } else if (state instanceof TableUiState.Ready) {
            TableUiState.Ready ready = (TableUiState.Ready) state;
            webView.loadDataWithBaseURL(
                    "file:///android_asset/",   // base URL: permette al CSS di risolvere @import e url()
                    buildGridHtml(ready),
                    "text/html",
                    "UTF-8",
                    null);

        } else if (state instanceof TableUiState.Error) {
            TableUiState.Error error = (TableUiState.Error) state;
            webView.loadData(
                    buildErrorHtml(error.getMessage()),
                    "text/html",
                    "UTF-8");
        }
    }

    // ─── Costruzione HTML ──────────────────────────────────────────────────

    /**
     * Costruisce la pagina HTML completa della griglia.
     * Il CSS viene letto da assets/ tramite {@link CssProvider}:
     * per cambiare stile è sufficiente modificare il file CSS
     * senza toccare questo file.
     */
    private String buildGridHtml(TableUiState.Ready state) {
        int rows = state.getRows();
        int cols = state.getCols();

        String css;
        try {
            css = CssProvider.load(requireContext());
        } catch (IOException e) {
            // Fallback: griglia visibile anche senza CSS
            css = "#grid{display:grid;width:100vw;height:100vh;}" +
                    ".cell{display:flex;align-items:center;justify-content:center;" +
                    "border:1px solid #546e7a;background:#263238;}" +
                    ".cell-label{color:#fff;font-size:12px;font-family:sans-serif;}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head>")
                .append("<meta charset='UTF-8'>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'>")
                .append("<style>").append(css).append("</style>")
                .append("</head><body>")
                .append("<div id='grid' style='")
                .append("grid-template-columns: repeat(").append(cols).append(", 1fr);")
                .append("grid-template-rows: repeat(").append(rows).append(", 1fr);")
                .append("'>")
                .append(buildCells(state))
                .append("</div>")
                .append("</body></html>");

        return sb.toString();
    }

    private String buildCells(TableUiState.Ready state) {
        List<BlockUiData> blocks = state.getBlocks();
        StringBuilder sb = new StringBuilder();

        for (BlockUiData block : blocks) {
            String cssClass = block.hasAudio() ? "cell has-audio" : "cell";
            // Il click chiama il bridge JS→Java passando riga e colonna.
            sb.append("<div class='").append(cssClass).append("'")
                    .append(" onclick=\"window.Android.onBlockClicked(")
                    .append(block.getRow()).append(",").append(block.getCol())
                    .append(")\">")
                    .append("<span class='cell-label'>")
                    .append(escapeHtml(block.getTitle()))
                    .append("</span>")
                    .append("</div>");
        }

        return sb.toString();
    }

    private String buildLoadingHtml() {
        return "<!DOCTYPE html><html><body style='"
                + "background:#1a1a2e;display:flex;align-items:center;"
                + "justify-content:center;height:100vh;margin:0;'>"
                + "<p style='color:#b0bec5;font-family:sans-serif;font-size:16px;'>"
                + "Inizializzazione...</p></body></html>";
    }

    private String buildErrorHtml(String message) {
        return "<!DOCTYPE html><html><body>"
                + "<div id='error-screen'>"
                + "<p>" + escapeHtml(message) + "</p>"
                + "</div></body></html>";
    }

    // ─── Utilità ───────────────────────────────────────────────────────────

    /** Escaping minimale per prevenire XSS nell'HTML generato. */
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ─── JavascriptInterface (bridge JS → Java) ────────────────────────────

    /**
     * Bridge esposto alla WebView come {@code window.Android}.
     * I metodi annotati con {@link JavascriptInterface} sono invocabili
     * direttamente dal JavaScript della pagina.
     *
     * ATTENZIONE: i callback JS arrivano su un thread di background;
     * {@link TableController#onBlockClicked} è thread-safe perché
     * l'accesso a {@link DBsoftware.Audiodeck.model.Table} è sincronizzato.
     */
    private class JsBridge {

        @JavascriptInterface
        public void onBlockClicked(int row, int col) {
            controller.onBlockClicked(row, col);
        }
    }
}