package net.kdt.pojavlaunch.epic;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.kdt.mcgui.ProgressLayout;
import com.kdt.mcgui.mcAccountSpinner;
import net.kdt.pojavlaunch.*;
import net.kdt.pojavlaunch.extra.*;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.profiles.VersionSelectorDialog;
import net.kdt.pojavlaunch.progresskeeper.*;
import net.kdt.pojavlaunch.tasks.AsyncVersionList;
import net.kdt.pojavlaunch.value.MinecraftAccount;
import net.kdt.pojavlaunch.value.launcherprofiles.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class Home extends Fragment {
    private static String sessionUser;
    private final EpicMods mods = new EpicMods(this, this::log);
    private LinearLayout root;
    private TextView console, detail;
    private Button play;
    private ProgressBar bar;
    private Dialog progress;
    private String username, lastLine = "";
    private boolean fetching, observing;
    private long lastProgressTime;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final String[] records = {ProgressLayout.DOWNLOAD_MINECRAFT,
            ProgressLayout.UNPACK_RUNTIME, ProgressLayout.DOWNLOAD_VERSION_LIST};
    private final ProgressListener listener = new ProgressListener() {
        public void onProgressStarted() { handler.post(() -> { if (root != null) showProgress(); }); }
        public void onProgressEnded() { handler.post(() -> {
            if (!ProgressKeeper.hasOngoingTasks()) closeProgress();
        }); }
        public void onProgressUpdated(int p, int res, Object... args) {
            long now = android.os.SystemClock.elapsedRealtime();
            if (now - lastProgressTime < 250) return;
            lastProgressTime = now;
            handler.post(() -> {
                if (root == null || !isAdded()) return;
                try {
                    String line = getString(res, args);
                    if (!line.equals(lastLine)) { lastLine = line; log(line); }
                    if (detail != null && !DownloadStatus.current.downloading) {
                        detail.setText(line); bar.setIndeterminate(true);
                    }
                } catch (RuntimeException ignored) { /* Invalid native resource: keep last status. */ }
            });
        }
    };
    private final Runnable tick = new Runnable() {
        public void run() {
            if (root == null) return;
            DownloadStatus.Snapshot s = DownloadStatus.current;
            if (detail != null && s.downloading) {
                int percent = s.percent();
                bar.setIndeterminate(percent < 0);
                if (percent >= 0) bar.setProgress(percent);
                String count = percent < 0 ? String.format(Locale.ROOT, "%.1f MB procesados", s.done / 1048576d)
                    : String.format(Locale.ROOT, "%.1f / %.1f MB — %d %%", s.done / 1048576d, s.total / 1048576d, percent);
                detail.setText("Descargando: " + s.path + "\n" + count
                    + "\nIncluye archivos existentes verificados.");
            }
            if (play != null) play.setEnabled(!fetching && !mods.isBusy() && !ProgressKeeper.hasOngoingTasks());
            handler.postDelayed(this, 150);
        }
    };

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle state) {
        root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(12));
        EpicStyle.background(root);
        username = sessionUser;
        mods.restore(state);
        if (username == null) login();
        else home();
        return root;
    }
    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state); mods.save(state);
    }
    @Override public void onStart() {
        super.onStart();
        observing = true;
        for (String record : records) ProgressKeeper.addListener(record, listener);
        handler.post(tick);
    }
    @Override public void onStop() {
        fetching = false;
        if (observing) for (String record : records) ProgressKeeper.removeListener(record, listener);
        observing = false;
        handler.removeCallbacksAndMessages(null);
        closeProgress();
        super.onStop();
    }
    @Override public void onDestroyView() {
        root = null; console = null; play = null;
        super.onDestroyView();
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size) {
        TextView v = new TextView(requireContext());
        v.setText(value); v.setTextSize(size); v.setTextColor(0xffeeeeee);
        v.setPadding(dp(8), dp(8), dp(8), dp(8));
        return v;
    }
    private Button button(String value, Runnable action) {
        Button b = new Button(requireContext()); b.setAllCaps(false); b.setText(value);
        EpicStyle.button(b, false); b.setOnClickListener(v -> action.run()); return b;
    }
    private void home() {
        root.removeAllViews(); root.setGravity(Gravity.TOP); EpicStyle.background(root);
        LinearLayout header = new LinearLayout(requireContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text("EpicCraft Launcher", 25), new LinearLayout.LayoutParams(0, -2, 1));
        Button gear = button("⚙", this::settings);
        gear.setContentDescription("Ajustes");
        header.addView(gear, new LinearLayout.LayoutParams(dp(56), dp(56)));
        root.addView(header);
        TextView subtitle = text("JAVA EDITION  /  TU ESPACIO DE JUEGO", 11);
        subtitle.setTextColor(EpicStyle.MUTED); subtitle.setLetterSpacing(.12f); root.addView(subtitle);
        LinearLayout columns = new LinearLayout(requireContext());
        boolean wide = getResources().getConfiguration().screenWidthDp >= 600;
        columns.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        LinearLayout.LayoutParams body = new LinearLayout.LayoutParams(-1, 0, 1);
        body.topMargin = dp(14); body.bottomMargin = dp(16); root.addView(columns, body);
        LinearLayout left = new LinearLayout(requireContext()); left.setOrientation(1);
        columns.addView(left, wide ? new LinearLayout.LayoutParams(0, -1, 1) : new LinearLayout.LayoutParams(-1, -2));
        LinearLayout card = new LinearLayout(requireContext());
        card.setGravity(Gravity.CENTER_VERTICAL); EpicStyle.panel(card, 0xff303947);
        ImageView head = new ImageView(requireContext());
        head.setContentDescription("Cabeza de la skin de " + username);
        head.setImageResource(android.R.drawable.ic_menu_myplaces);
        card.addView(head, new LinearLayout.LayoutParams(dp(72), dp(72)));
        card.addView(text("Conectado como\n" + username, 19)); left.addView(card);
        LinearLayout.LayoutParams add = new LinearLayout.LayoutParams(-1, dp(52));
        add.topMargin = dp(12); add.bottomMargin = dp(8);
        left.addView(button("＋  Añadir cuenta", () -> ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true)), add);
        mcAccountSpinner accounts = requireActivity().findViewById(R.id.account_spinner);
        MinecraftAccount selected = accounts.getSelectedAccount();
        if (selected != null) left.addView(button("Cuenta: " + selected.username, this::chooseAccount));
        ScrollView scroll = new ScrollView(requireContext());
        console = text("CONSOLA\nListo. Tocá JUGAR para elegir la versión.\n", 12);
        console.setTypeface(Typeface.MONOSPACE); console.setTextIsSelectable(true);
        console.setTextColor(0xffb2c8cf); console.setLineSpacing(dp(4), 1);
        EpicStyle.panel(scroll, 0xff0c1521); scroll.addView(console);
        LinearLayout.LayoutParams logs = wide ? new LinearLayout.LayoutParams(0, -1, 1.3f) : new LinearLayout.LayoutParams(-1, 0, 1);
        if (wide) logs.leftMargin = dp(20); else logs.topMargin = dp(12);
        columns.addView(scroll, logs);
        play = button("▶   JUGAR", this::versions); EpicStyle.button(play, true);
        root.addView(play, new LinearLayout.LayoutParams(-1, dp(56)));
        loadHead(head, username);
    }
    private void login() {
        username = null; sessionUser = null; closeProgress();
        console = null; play = null;
        EpicLogin.show(root, value -> { username = value; sessionUser = value; home(); });
    }
    private void settings() {
        new AlertDialog.Builder(requireContext()).setTitle("Ajustes")
            .setItems(new String[]{"Importar mods Java (.jar)", "Instalar Fabric", "Instalar Forge",
                "Cambiar usuario", "Elegir cuenta de Minecraft", "Último registro del juego"}, (dialog, which) -> {
                if (ProgressKeeper.hasOngoingTasks() || mods.isBusy()) {
                    Toast.makeText(requireContext(), "Esperá a que termine la operación actual", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (which == 0) mods.chooseVersion();
                else if (which == 1) Tools.swapFragment(requireActivity(),
                    net.kdt.pojavlaunch.fragments.FabricInstallFragment.class,
                    net.kdt.pojavlaunch.fragments.FabricInstallFragment.TAG, null);
                else if (which == 2) Tools.swapFragment(requireActivity(),
                    net.kdt.pojavlaunch.fragments.ForgeInstallFragment.class,
                    net.kdt.pojavlaunch.fragments.ForgeInstallFragment.TAG, null);
                else if (which == 3) login();
                else if (which == 4) chooseAccount();
                else readGameLog();
            }).setNegativeButton("Cerrar", null).show();
    }
    private void chooseAccount() {
        mcAccountSpinner accounts = requireActivity().findViewById(R.id.account_spinner);
        int count = accounts.getCount() - 1;
        if (count <= 0) { ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true); return; }
        String[] names = new String[count];
        for (int i = 0; i < count; i++) names[i] = String.valueOf(accounts.getItemAtPosition(i + 1));
        new AlertDialog.Builder(requireContext()).setTitle("Cuenta de Minecraft")
            .setItems(names, (dialog, index) -> {
                accounts.setSelection(index + 1);
                accounts.post(() -> { if (root != null && username != null) home(); });
            }).setNegativeButton("Cancelar", null).show();
    }
    private void log(String line) {
        if (console == null) return;
        console.append(line + "\n");
        if (console.length() > 24000) console.setText(console.getText().subSequence(console.length() - 16000, console.length()));
    }
    private void versions() {
        if (fetching || ProgressKeeper.hasOngoingTasks()) return;
        fetching = true; play.setEnabled(false); log("Consultando versiones al motor…");
        new AsyncVersionList().getVersionList(list -> handler.post(() -> {
            if (root == null || !isAdded()) return;
            fetching = false; play.setEnabled(true);
            if (list != null) ExtraCore.setValue(ExtraConstants.RELEASE_TABLE, list);
            else log("Sin catálogo en línea: se mostrarán las versiones locales disponibles.");
            VersionSelectorDialog.open(requireContext(), false, (version, snapshot) -> launch(version));
        }), true);
    }
    private void launch(String version) {
        mcAccountSpinner accounts = requireActivity().findViewById(R.id.account_spinner);
        MinecraftAccount account = accounts.getSelectedAccount();
        if (account == null) {
            log("Para descargar y jugar, vinculá tu cuenta de Minecraft desde Añadir cuenta. Tu usuario local identifica tu perfil del launcher.");
            Toast.makeText(requireContext(), "Vinculá tu cuenta desde Añadir cuenta", Toast.LENGTH_LONG).show(); return;
        }
        if (account.isLocal() && !new File(Tools.DIR_HOME_VERSION, version + "/" + version + ".json").isFile()) {
            log("Esta versión aún no está instalada. Elegí una cuenta de Microsoft con Minecraft Java desde Ajustes para descargarla."); return;
        }
        try {
            MinecraftProfile profile = EpicMods.selectVersion(version);
            LauncherProfiles.write(); DownloadStatus.reset();
            log("Preparando Minecraft " + version + "…");
            ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
        } catch (RuntimeException e) { log("Error: " + e.getMessage()); }
    }
    private void showProgress() {
        if (!isAdded() || progress != null || username == null) return;
        LinearLayout box = new LinearLayout(requireContext()); box.setOrientation(1);
        box.setPadding(dp(20), dp(20), dp(20), dp(20));
        detail = text("Preparando descarga…", 15); box.addView(detail);
        bar = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(EpicStyle.ACCENT));
        bar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(EpicStyle.ACCENT));
        bar.setMax(100); bar.setIndeterminate(true); box.addView(bar, new LinearLayout.LayoutParams(-1, dp(30)));
        progress = new AlertDialog.Builder(requireContext()).setTitle("EpicCraft Launcher").setView(box)
            .setNegativeButton("Ver consola", (d, w) -> { progress = null; detail = null; bar = null; }).create();
        progress.setCancelable(false); progress.show();
    }
    private void closeProgress() {
        if (progress != null) progress.dismiss();
        progress = null; detail = null; bar = null;
    }
    private void loadHead(ImageView target, String name) {
        PojavApplication.sExecutorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL("https://mc-heads.net/avatar/" + name + "/64").openConnection();
                connection.setConnectTimeout(7000); connection.setReadTimeout(7000);
                if (connection.getResponseCode() != 200) return;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (InputStream in = connection.getInputStream()) {
                    byte[] buffer = new byte[4096]; int n;
                    while ((n = in.read(buffer)) != -1) {
                        if (bytes.size() + n > 262144) throw new IOException("Imagen demasiado grande");
                        bytes.write(buffer, 0, n);
                    }
                }
                byte[] data = bytes.toByteArray();
                BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
                if (bounds.outWidth > 512 || bounds.outHeight > 512) return;
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                handler.post(() -> { if (root != null && name.equals(username) && bitmap != null) target.setImageBitmap(bitmap); });
            } catch (IOException ignored) { /* Keep the placeholder when offline. */ }
            finally { if (connection != null) connection.disconnect(); }
        });
    }
    private void readGameLog() {
        PojavApplication.sExecutorService.execute(() -> {
            String result;
            try (RandomAccessFile file = new RandomAccessFile(new File(Tools.DIR_GAME_HOME, "latestlog.txt"), "r")) {
                file.seek(Math.max(0, file.length() - 16000));
                byte[] bytes = new byte[(int) (file.length() - file.getFilePointer())]; file.readFully(bytes);
                result = new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) { result = "Todavía no hay un registro del juego."; }
            String output = result; handler.post(() -> log(output));
        });
    }
}

