# Mantiene i metodi annotati con @JavascriptInterface nella WebView.
# Senza questa regola, in release il minifier rimuove i metodi del bridge
# JS→Java e i click sulle celle smettono di funzionare.
-keepclassmembers class DBsoftware.Audiodeck.Presenter.TablePresenter$JsBridge {
    public *;
}

# Opzionale: conserva i numeri di riga negli stack trace per il debug.
# -keepattributes SourceFile,LineNumberTable
# -renamesourcefileattribute SourceFile