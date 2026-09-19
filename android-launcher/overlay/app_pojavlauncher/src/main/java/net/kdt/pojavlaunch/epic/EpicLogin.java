package net.kdt.pojavlaunch.epic;

import android.content.Context;
import android.text.InputFilter;
import android.view.Gravity;
import android.widget.*;

/** Local launcher identity. It is deliberately separate from game authentication. */
public final class EpicLogin {
    public interface Done { void accept(String name); }
    public static void show(LinearLayout root, Done done) {
        Context c = root.getContext();
        root.removeAllViews();
        root.setGravity(Gravity.CENTER);
        TextView title = new TextView(c);
        title.setText("EpicCraft Launcher");
        title.setTextColor(0xffeeeeee);
        title.setTextSize(30);
        root.addView(title);
        EditText name = new EditText(c);
        name.setSingleLine(true);
        name.setHint("Nombre de usuario local");
        name.setTextColor(0xffffffff);
        name.setHintTextColor(0xffaab0bd);
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
        name.setText(c.getSharedPreferences("epic", 0).getString("username", ""));
        root.addView(name, new LinearLayout.LayoutParams(-1, -2));
        Button login = new Button(c);
        login.setText("Iniciar sesión");
        root.addView(login);
        Runnable submit = () -> {
            String value = name.getText().toString().trim();
            if (!value.matches("[A-Za-z0-9_]{3,16}")) {
                name.setError("Usá entre 3 y 16 letras, números o guion bajo");
                return;
            }
            c.getSharedPreferences("epic", 0).edit().putString("username", value).apply();
            done.accept(value);
        };
        login.setOnClickListener(v -> submit.run());
        name.setOnEditorActionListener((v, action, event) -> {
            submit.run(); return true;
        });
    }
}
