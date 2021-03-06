package com.ozayakcan.chat.Fragment;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ozayakcan.chat.Adapter.KisiAdapter;
import com.ozayakcan.chat.KisilerActivity;
import com.ozayakcan.chat.MainActivity;
import com.ozayakcan.chat.Model.Kullanici;
import com.ozayakcan.chat.Ozellik.Izinler;
import com.ozayakcan.chat.Ozellik.SharedPreference;
import com.ozayakcan.chat.Ozellik.Veritabani;
import com.ozayakcan.chat.R;

import java.util.ArrayList;
import java.util.List;

public class KisilerFragment extends Fragment {

    private FirebaseUser firebaseUser;
    private LinearLayout progressBarLayout;
    private RecyclerView kisilerRW;
    private KisiAdapter kisiAdapter;
    private List<Kullanici> kullaniciList;
    private final MainActivity mainActivity;
    private final KisilerActivity kisilerActivity;
    public KisilerFragment(MainActivity mainActivity) {
        this.kisilerActivity = null;
        this.mainActivity = mainActivity;
    }

    public KisilerFragment(KisilerActivity kisilerActivity){
        this.mainActivity = null;
        this.kisilerActivity = kisilerActivity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kisiler, container, false);
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        progressBarLayout = view.findViewById(R.id.progressBarLayout);
        kisilerRW = view.findViewById(R.id.kisilerRW);
        kisilerRW.setHasFixedSize(true);
        kisilerRW.setLayoutManager(new LinearLayoutManager(getActivity()));
        kullaniciList = new ArrayList<>();
        if (Izinler.getInstance(getContext()).KontrolEt(Manifest.permission.READ_CONTACTS)){
            KisileriGoster();
        }else{
            Izinler.getInstance(getContext()).Sor(Manifest.permission.READ_CONTACTS, kisiIzniResultLauncher);
        }
        return view;
    }

    public void KisileriYenile() {
        if (Izinler.getInstance(getContext()).KontrolEt(Manifest.permission.READ_CONTACTS)){
            progressBarLayout.setVisibility(View.VISIBLE);
            Veritabani.getInstance(getContext()).KisileriEkle(firebaseUser, () -> {
                progressBarLayout.setVisibility(View.GONE);
                KisileriGoster();
            });
        }else{
            Izinler.getInstance(getContext()).Sor(Manifest.permission.READ_CONTACTS, kisiIzniYenileResultLauncher);
        }
    }
    ActivityResultLauncher<String> kisiIzniYenileResultLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            result -> {
                if (result){
                    KisileriYenile();
                }else{
                    Toast.makeText(getContext(), getString(R.string.you_must_grant_contact_permission), Toast.LENGTH_SHORT).show();;
                }
            });
    private final String KisilerYenilendi = "kisilerYenilendi";
    private void KisileriGoster() {
        if (!SharedPreference.getInstance(getContext()).GetirBoolean(KisilerYenilendi, false)){
            Veritabani.getInstance(getContext()).KisileriEkle(firebaseUser, () -> {
                SharedPreference.getInstance(getContext()).KaydetBoolean(KisilerYenilendi, true);
                KisileriGoster();
            });
            return;
        }
        DatabaseReference kisilerRef = FirebaseDatabase.getInstance().getReference(Veritabani.KullaniciTablosu + "/" + firebaseUser.getPhoneNumber() + "/" + Veritabani.KisiTablosu);
        kisilerRef.keepSynced(true);
        kisilerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                kullaniciList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                    Kullanici kullanici = dataSnapshot.getValue(Kullanici.class);
                    if (kullanici != null){
                        kullaniciList.add(kullanici);
                    }
                }
                if (kisilerActivity != null){
                    kisiAdapter = new KisiAdapter(kullaniciList, kisilerActivity);
                }
                if (mainActivity != null){
                    kisiAdapter = new KisiAdapter(kullaniciList, mainActivity);
                }
                kisilerRW.setAdapter(kisiAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    ActivityResultLauncher<String> kisiIzniResultLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            result -> {
                if (result){
                    KisileriGoster();
                }else{
                    KisiIzniUyariKutusu();
                }
            });

    private void KisiIzniUyariKutusu() {
        Izinler.getInstance(getContext()).ZorunluIzinUyariKutusu(Manifest.permission.READ_CONTACTS, kisiIzniResultLauncher);
    }
}