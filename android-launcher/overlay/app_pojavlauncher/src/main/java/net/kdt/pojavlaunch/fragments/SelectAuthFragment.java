package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.epic.EpicStyle;

/** Account management is opened explicitly, separately from the local launcher profile. */
public class SelectAuthFragment extends Fragment {
    public static final String TAG = "AUTH_SELECT_FRAGMENT";
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle state) {
        LinearLayout root = new LinearLayout(requireContext()); root.setOrientation(1);
        root.setGravity(Gravity.CENTER); EpicStyle.background(root);
        root.setPadding(EpicStyle.dp(root,24),EpicStyle.dp(root,24),EpicStyle.dp(root,24),EpicStyle.dp(root,24));
        ScrollView scroll = new ScrollView(requireContext());
        int width = Math.min(EpicStyle.dp(root,480),getResources().getDisplayMetrics().widthPixels-EpicStyle.dp(root,48));
        root.addView(scroll,new LinearLayout.LayoutParams(width,-2));
        LinearLayout card = new LinearLayout(requireContext()); card.setOrientation(1);
        EpicStyle.panel(card,0xff182538); scroll.addView(card);
        card.addView(EpicStyle.text(requireContext(),"Añadir cuenta",28,EpicStyle.INK));
        TextView explanation = EpicStyle.text(requireContext(),
            "Tu usuario local personaliza EpicCraft. Para descargar Minecraft Java y jugar, vinculá la cuenta que tiene el juego.",15,EpicStyle.MUTED);
        explanation.setPadding(0,EpicStyle.dp(root,16),0,EpicStyle.dp(root,24)); card.addView(explanation);
        add(card,"Vincular cuenta de Microsoft",true,()->Tools.swapFragment(requireActivity(),MicrosoftLoginFragment.class,MicrosoftLoginFragment.TAG,null));
        add(card,"Perfil sin conexión",false,()->Tools.hasNoOnlineProfileDialog(requireActivity(),()->Tools.swapFragment(requireActivity(),LocalLoginFragment.class,LocalLoginFragment.TAG,null)));
        TextView offline = EpicStyle.text(requireContext(),"El motor habilita el perfil sin conexión después de verificar una cuenta con Minecraft Java.",12,EpicStyle.MUTED);
        offline.setPadding(0,0,0,EpicStyle.dp(root,16)); card.addView(offline);
        add(card,"Volver a EpicCraft",false,()->Tools.backToMainMenu(requireActivity()));
        return root;
    }
    private void add(LinearLayout card,String label,boolean primary,Runnable action){
        Button button = new Button(requireContext()); button.setText(label); EpicStyle.button(button,primary);
        button.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,-2); params.bottomMargin=EpicStyle.dp(card,12);
        card.addView(button,params);
    }
}
