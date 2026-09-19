package net.kdt.pojavlaunch.epic;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.launcherprofiles.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipFile;

/** Copies validated mod archives to the exact game directory launched for this version. */
public final class EpicMods {
    public interface Message { void show(String text); }
    private final Fragment host;
    private final Message output;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<String[]> picker;
    private String targetVersion;
    private volatile boolean busy;

    public EpicMods(Fragment host, Message output) {
        this.host = host; this.output = output;
        picker = host.registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
            if (uris.isEmpty() || targetVersion == null) return;
            String version = targetVersion;
            Context context = host.requireContext().getApplicationContext();
            busy = true;
            PojavApplication.sExecutorService.execute(() -> {
                ArrayList<String> results = new ArrayList<>();
                try {
                    String loader = loaderFor(version);
                    File directory = new File(Tools.DIR_GAME_HOME, "epiccraft/instances/" + version + "/mods");
                    if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("No se pudo crear la carpeta mods");
                    for (Uri uri : uris) {
                        try { results.add("Importado: " + copy(context, uri, directory, loader)); }
                        catch (Exception e) { results.add("No importado: " + e.getMessage()); }
                    }
                } catch (Exception e) { results.add("Error: " + e.getMessage()); }
                finally { busy = false; }
                main.post(() -> {
                    if (!host.isAdded()) return;
                    for (String result : results) output.show(result);
                    new AlertDialog.Builder(host.requireContext()).setTitle("Mods para " + version)
                        .setMessage(android.text.TextUtils.join("\n", results)
                            + "\n\nEn JUGAR elegí esta misma versión con su cargador. Las dependencias de cada mod también deben estar instaladas.")
                        .setPositiveButton("Listo", null).show();
                });
            });
        });
    }
    public boolean isBusy() { return busy; }
    public void save(Bundle state) { state.putString("epicModTarget", targetVersion); }
    public void restore(Bundle state) { if (state != null) targetVersion = state.getString("epicModTarget"); }

    public void chooseVersion() {
        if (busy) return;
        File[] folders = new File(Tools.DIR_HOME_VERSION).listFiles();
        ArrayList<String> choices = new ArrayList<>();
        if (folders != null) for (File folder : folders) {
            try { if (!loaderFor(folder.getName()).isEmpty()) choices.add(folder.getName()); }
            catch (Exception ignored) { }
        }
        Collections.sort(choices);
        if (choices.isEmpty()) {
            new AlertDialog.Builder(host.requireContext()).setTitle("Instalá un cargador de mods")
                .setMessage("Primero instalá Fabric o Forge desde el engranaje. Después podés importar los archivos .jar para esa versión.")
                .setPositiveButton("Entendido", null).show(); return;
        }
        new AlertDialog.Builder(host.requireContext()).setTitle("¿Para qué versión son los mods?")
            .setItems(choices.toArray(new String[0]), (dialog, index) -> {
                targetVersion = choices.get(index);
                selectVersion(targetVersion);
                new AlertDialog.Builder(host.requireContext()).setTitle("Importar mods .jar")
                    .setMessage("Elegí mods compatibles con " + targetVersion + ". Se copiarán a su carpeta mods; no se ejecutan durante la importación.")
                    .setPositiveButton("Elegir archivos", (d, w) -> picker.launch(new String[]{"*/*"}))
                    .setNegativeButton("Cancelar", null).show();
            }).setNegativeButton("Cancelar", null).show();
    }

    public static synchronized MinecraftProfile selectVersion(String version) {
        if (!version.matches("[A-Za-z0-9_.+\\-]+") || version.equals(".") || version.equals(".."))
            throw new IllegalArgumentException("Identificador de versión inválido");
        LauncherProfiles.load();
        String key = UUID.nameUUIDFromBytes(("EpicCraft:" + version).getBytes(StandardCharsets.UTF_8)).toString();
        MinecraftProfile profile = LauncherProfiles.mainProfileJson.profiles.get(key);
        if (profile == null) profile = MinecraftProfile.getDefaultProfile();
        profile.name = "EpicCraft " + version;
        profile.lastVersionId = version;
        try {
            profile.gameDir = loaderFor(version).isEmpty() ? null : "epiccraft/instances/" + version;
        } catch (Exception ignored) { profile.gameDir = null; }
        LauncherProfiles.mainProfileJson.profiles.put(key, profile);
        LauncherProfiles.write();
        LauncherPreferences.DEFAULT_PREF.edit().putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key).apply();
        return profile;
    }

    private static String loaderFor(String version) throws Exception {
        if (!version.matches("[A-Za-z0-9_.+\\-]+") || version.equals(".") || version.equals("..")) return "";
        File metadata = new File(Tools.DIR_HOME_VERSION, version + "/" + version + ".json");
        JSONObject json = new JSONObject(Tools.read(metadata));
        JSONArray libraries = json.optJSONArray("libraries");
        if (libraries == null) return "";
        for (int i = 0; i < libraries.length(); i++) {
            String name = libraries.getJSONObject(i).optString("name");
            if (name.startsWith("net.fabricmc:fabric-loader:")) return "fabric";
            if (name.startsWith("net.neoforged:neoforge:")) return "neoforge";
            if (name.startsWith("net.minecraftforge:forge:")) return "forge";
        }
        return "";
    }
    private static String copy(Context c, Uri uri, File directory, String loader) throws Exception {
        if (loader.isEmpty()) throw new IOException("Falta un cargador de mods");
        String name = null;
        try (Cursor cursor = c.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
        }
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".jar") || name.contains("/") || name.contains("\\"))
            throw new IOException("Seleccioná un archivo .jar válido");
        File target = new File(directory, name);
        if (target.exists()) throw new IOException(name + " ya existe; no se sobrescribió");
        File temporary = File.createTempFile("mod-", ".part", directory);
        try {
            try (InputStream in = c.getContentResolver().openInputStream(uri); OutputStream out = new FileOutputStream(temporary)) {
                if (in == null) throw new IOException("No se pudo abrir " + name);
                byte[] buffer = new byte[65536]; int n; long total = 0;
                while ((n = in.read(buffer)) != -1) {
                    total += n;
                    if (total > 512L * 1024 * 1024) throw new IOException("El mod supera 512 MB");
                    out.write(buffer, 0, n);
                }
            }
            try (ZipFile jar = new ZipFile(temporary)) {
                boolean valid = loader.equals("fabric") ? jar.getEntry("fabric.mod.json") != null
                    : loader.equals("neoforge") ? jar.getEntry("META-INF/neoforge.mods.toml") != null
                    : jar.getEntry("META-INF/mods.toml") != null || jar.getEntry("mcmod.info") != null;
                if (!valid) throw new IOException(name + " no contiene metadatos para " + loader);
            }
            if (!temporary.renameTo(target)) throw new IOException("No se pudo guardar " + name);
            return name;
        } finally { if (temporary.exists()) temporary.delete(); }
    }
}
