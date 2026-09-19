package net.kdt.pojavlaunch.epic;
import android.content.Context;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
public final class EpicLogin {
    public interface Done { void accept(String name); }
    public static void show(LinearLayout root, Done done) {
        Context c=root.getContext(); root.removeAllViews(); root.setGravity(Gravity.CENTER);
        EpicStyle.background(root);
        ScrollView scroll=new ScrollView(c); scroll.setFillViewport(false);
        int width=Math.min(EpicStyle.dp(root,440),c.getResources().getDisplayMetrics().widthPixels-EpicStyle.dp(root,40));
        root.addView(scroll,new LinearLayout.LayoutParams(width,-2));
        LinearLayout card=new LinearLayout(c); card.setOrientation(1); EpicStyle.panel(card,0xff182538);
        scroll.addView(card);
        TextView badge=EpicStyle.text(c,"E / C",24,EpicStyle.ACCENT);
        badge.setTypeface(null,Typeface.BOLD); badge.setPadding(0,0,0,EpicStyle.dp(root,20)); card.addView(badge);
        TextView title=EpicStyle.text(c,"EpicCraft Launcher",30,EpicStyle.INK);
        title.setTypeface(null,Typeface.BOLD); card.addView(title);
        TextView subtitle=EpicStyle.text(c,"Tu próxima aventura empieza acá.",15,EpicStyle.MUTED);
        subtitle.setPadding(0,EpicStyle.dp(root,8),0,EpicStyle.dp(root,24)); card.addView(subtitle);
        TextView label=EpicStyle.text(c,"USUARIO LOCAL",11,EpicStyle.MUTED); label.setLetterSpacing(.12f); card.addView(label);
        EditText name=new EditText(c); name.setSingleLine(true); name.setHint("Tu nombre de jugador");
        name.setTextSize(16); name.setTextColor(EpicStyle.INK); name.setHintTextColor(EpicStyle.MUTED);
        name.setBackground(EpicStyle.shape(name,0xff0e1929,14));
        name.setPadding(EpicStyle.dp(root,16),0,EpicStyle.dp(root,16),0);
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
        name.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        name.setImeOptions(EditorInfo.IME_ACTION_DONE);
        name.setText(c.getSharedPreferences("epic",0).getString("username",""));
        LinearLayout.LayoutParams field=new LinearLayout.LayoutParams(-1,EpicStyle.dp(root,56));
        field.topMargin=EpicStyle.dp(root,10); field.bottomMargin=EpicStyle.dp(root,20); card.addView(name,field);
        Button login=new Button(c); login.setText("Iniciar sesión"); EpicStyle.button(login,true);
        card.addView(login,new LinearLayout.LayoutParams(-1,EpicStyle.dp(root,54)));
        Runnable submit=()->{String value=name.getText().toString().trim();
            if(!value.matches("[A-Za-z0-9_]{3,16}")){name.setError("Usá entre 3 y 16 letras, números o guion bajo");return;}
            c.getSharedPreferences("epic",0).edit().putString("username",value).apply();
            android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)c.getSystemService(Context.INPUT_METHOD_SERVICE);
            if(keyboard!=null)keyboard.hideSoftInputFromWindow(name.getWindowToken(),0);
            done.accept(value);};
        login.setOnClickListener(v->submit.run());
        name.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){submit.run();return true;}return false;});
    }
}
