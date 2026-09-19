package net.kdt.pojavlaunch.epic;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.*;

public final class EpicStyle {
    public static final int INK=0xffedf2ff, MUTED=0xff9baac3, ACCENT=0xff9be8bc;
    public static int dp(View v,int n){return Math.round(n*v.getResources().getDisplayMetrics().density);}
    public static GradientDrawable shape(View v,int color,int radius){
        GradientDrawable d=new GradientDrawable(); d.setColor(color);
        d.setCornerRadius(dp(v,radius)); return d;
    }
    public static void background(View v){
        v.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xff19283b,0xff0d1523,0xff101b29}));
    }
    public static void panel(View v,int color){
        GradientDrawable d=shape(v,color,22); d.setStroke(dp(v,1),0xff344257); v.setBackground(d);
        v.setPadding(dp(v,20),dp(v,20),dp(v,20),dp(v,20));
    }
    public static void button(Button v,boolean primary){
        v.setAllCaps(false); v.setTextSize(15); v.setTypeface(Typeface.create("sans-serif-medium",0));
        v.setTextColor(primary?0xff10271d:INK); v.setMinHeight(dp(v,52));
        v.setBackgroundTintList(null);
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x3381dcb0),
            shape(v,primary?ACCENT:0xff233247,16),null));
        v.setPadding(dp(v,18),dp(v,10),dp(v,18),dp(v,10));
        v.setStateListAnimator(null);
    }
    public static TextView text(android.content.Context c,String s,int size,int color){
        TextView v=new TextView(c); v.setText(s); v.setTextSize(size); v.setTextColor(color);
        v.setTypeface(Typeface.create("sans-serif",0)); return v;
    }
}
