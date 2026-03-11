package DBsoftware.Audiodeck.Presenter;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import DBsoftware.Audiodeck.controller.BlockUiData;
import DBsoftware.Audiodeck.controller.TableController;
import DBsoftware.Audiodeck.controller.TableUiState;

/**
 * Presenter MVCP — Fragment che presenta la griglia come pagina HTML/CSS
 * all'interno di una {@link WebView}.
 *
 * Le immagini di copertina vengono convertite in data URI base64 prima
 * di essere iniettate nell'HTML, rendendo il rendering indipendente
 * dal tipo di URI sorgente (content://, file://, ecc.).
 */
public class TablePresenter extends Fragment {

    private static final String TAG = "TablePresenter";

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

        webView = new WebView(requireContext());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);

        webView.addJavascriptInterface(new JsBridge(), "Android");

        FrameLayout root = new FrameLayout(requireContext());
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
            webView.loadData(buildLoadingHtml(), "text/html", "UTF-8");

        } else if (state instanceof TableUiState.Ready) {
            TableUiState.Ready ready = (TableUiState.Ready) state;
            webView.loadDataWithBaseURL(
                    "file:///android_asset/",
                    buildGridHtml(ready),
                    "text/html",
                    "UTF-8",
                    null);

        } else if (state instanceof TableUiState.Error) {
            TableUiState.Error error = (TableUiState.Error) state;
            webView.loadData(buildErrorHtml(error.getMessage()), "text/html", "UTF-8");
        }
    }

    // ─── Costruzione HTML ──────────────────────────────────────────────────

    private String buildGridHtml(TableUiState.Ready state) {
        int rows = state.getRows();
        int cols = state.getCols();

        String css;
        try {
            css = CssProvider.load(requireContext());
        } catch (IOException e) {
            css = "#grid{display:grid;width:100vw;height:100vh;}" +
                    ".cell{display:flex;flex-direction:column;align-items:center;" +
                    "justify-content:center;border:1px solid #546e7a;background:#263238;}" +
                    ".cell-cover{width:100%;flex:1;object-fit:cover;}" +
                    ".cell-label{color:#fff;font-size:12px;font-family:sans-serif;}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head>")
                .append("<meta charset='UTF-8'>")
                .append("<meta name='viewport' content='width=device-width,initial-scale=1.0,user-scalable=no'>")
                .append("<style>").append(css).append("</style>")
                .append("</head><body>")
                .append("<div id='grid' style='")
                .append("grid-template-columns:repeat(").append(cols).append(",1fr);")
                .append("grid-template-rows:repeat(").append(rows).append(",1fr);")
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

            sb.append("<div class='").append(cssClass).append("'")
                    .append(" onclick=\"window.Android.onBlockClicked(")
                    .append(block.getRow()).append(",").append(block.getCol())
                    .append(")\">");

            // ── Immagine di copertina ────────────────────────────────────────
            String imageUri = block.getImageUri();
            if (imageUri != null && !imageUri.isEmpty()) {
                String dataUri = toDataUri(imageUri);
                if (dataUri != null) {
                    sb.append("<img class='cell-cover' src='")
                            .append(dataUri)
                            .append("' alt=''>");
                }
            }

            // ── Titolo ───────────────────────────────────────────────────────
            sb.append("<span class='cell-label'>")
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

    // ─── Conversione URI → data URI base64 ────────────────────────────────

    /**
     * Legge il contenuto dell'URI tramite ContentResolver e lo converte
     * in un data URI base64 del tipo {@code data:<mime>;base64,<dati>}.
     *
     * Supporta qualsiasi schema (content://, file://) purché il
     * ContentResolver riesca ad aprire lo stream.
     *
     * @param uriString URI stringa dell'immagine.
     * @return data URI stringa, oppure null in caso di errore.
     */
    @Nullable
    private String toDataUri(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            ContentResolver cr = requireContext().getContentResolver();

            // Determina il MIME type (fallback: image/jpeg)
            String mime = cr.getType(uri);
            if (mime == null) mime = "image/jpeg";

            // Legge tutti i byte
            try (InputStream is = cr.openInputStream(uri)) {
                if (is == null) return null;
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int read;
                while ((read = is.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                }
                String encoded = Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP);
                return "data:" + mime + ";base64," + encoded;
            }
        } catch (Exception e) {
            Log.w(TAG, "Impossibile convertire l'immagine in base64: " + uriString, e);
            return null;
        }
    }

    // ─── Utilità ───────────────────────────────────────────────────────────

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ─── JavascriptInterface ───────────────────────────────────────────────

    private class JsBridge {
        @JavascriptInterface
        public void onBlockClicked(int row, int col) {
            controller.onBlockClicked(row, col);
        }
    }
}