package com.ozayakcan.chat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ozayakcan.chat.Adapter.MesajAdapter;
import com.ozayakcan.chat.Model.Kullanici;
import com.ozayakcan.chat.Model.Mesaj;
import com.ozayakcan.chat.Ozellik.E3KitKullanici;
import com.ozayakcan.chat.Ozellik.Resimler;
import com.ozayakcan.chat.Ozellik.SharedPreference;
import com.ozayakcan.chat.Ozellik.Veritabani;
import com.virgilsecurity.android.common.model.EThreeParams;
import com.virgilsecurity.android.ethree.interaction.EThree;
import com.virgilsecurity.common.callback.OnResultListener;
import com.virgilsecurity.sdk.cards.Card;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import kotlin.jvm.functions.Function0;

public class MesajActivity extends AppCompatActivity {

    private boolean ilkAcilis = true;
    private RecyclerView recyclerView;
    private TextView gonderText;
    private TextView durum;

    private String telefonString;
    private String tabloString = Veritabani.MesajTablosu;

    private FirebaseUser firebaseUser;
    private Veritabani veritabani;
    private MesajAdapter mesajAdapter;
    private List<Mesaj> mesajList;
    private Query mesajlariGosterQuery;
    private String idString;

    private EThree eThree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mesaj);
        Resimler resimler = new Resimler(MesajActivity.this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null){
            getSupportActionBar().setTitle("");
        }
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> Geri());

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        veritabani = new Veritabani(MesajActivity.this);
        mesajList = new ArrayList<>();

        Intent intent = getIntent();
        idString = intent.getStringExtra(Veritabani.IDKey);
        String isimString = intent.getStringExtra(Veritabani.IsimKey);
        telefonString = intent.getStringExtra(Veritabani.TelefonKey);
        String profilResmiString = intent.getStringExtra(Veritabani.ProfilResmiKey);
        String intentTabloString = intent.getStringExtra(Veritabani.MesajTablosu);
        if (intentTabloString != null){
            if (!intentTabloString.equals("")){
                tabloString = intentTabloString;
            }
        }
        RelativeLayout yaziKismi = findViewById(R.id.yaziKismi);
        RelativeLayout arsivKismi = findViewById(R.id.arsivKismi);
        if (tabloString.equals(Veritabani.ArsivTablosu)){
            yaziKismi.setVisibility(View.GONE);
            arsivKismi.setVisibility(View.VISIBLE);
        }else{
            arsivKismi.setVisibility(View.GONE);
            yaziKismi.setVisibility(View.VISIBLE);
        }
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        mesajAdapter = new MesajAdapter(MesajActivity.this, mesajList);
        mesajAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                recyclerView.setAdapter(mesajAdapter);
            }
        });
        recyclerView.setAdapter(mesajAdapter);
        CircleImageView profilResmi = findViewById(R.id.profilResmi);
        TextView kisiBasHarfi = findViewById(R.id.kisiBasHarfi);
        TextView isim = findViewById(R.id.isim);
        durum = findViewById(R.id.durum);
        gonderText = findViewById(R.id.gonderText);
        CircleImageView gonderBtn = findViewById(R.id.gonderBtn);
        resimler.ResimGoster(profilResmiString, profilResmi, R.drawable.varsayilan_arkaplan);
        if (profilResmiString.equals(Veritabani.VarsayilanDeger)){
            if (isimString.equals("")){
                kisiBasHarfi.setText("#");
            }else{
                kisiBasHarfi.setText(String.valueOf(isimString.charAt(0)));
            }
        }
        if (isimString.equals("")){
            isim.setText(telefonString);
        }else{
            isim.setText(isimString);
        }
        KisiBilgileriniGoster();
        KisininOnlineDurumunuGuncelle();
        veritabani.MesajDurumuGuncelle(firebaseUser.getPhoneNumber(), false);
        veritabani.MesajDurumuGuncelle(telefonString, true);
        SharedPreference sharedPreference = new SharedPreference(MesajActivity.this);
        if (sharedPreference.GetirString(E3KitKullanici.VirgilTokenKey, "").equals("")){
            E3KitKullanici e3KitKullanici = new E3KitKullanici(MesajActivity.this, firebaseUser.getPhoneNumber());
            new Thread(() -> e3KitKullanici.KullaniciyiGetir(new E3KitKullanici.Tamamlandi() {
                @Override
                public void Basarili(EThree kullanici) {
                    eThree = kullanici;
                    MesajlariGoster();
                    gonderBtn.setOnClickListener(v -> MesajGonder());
                }

                @Override
                public void Basarisiz(Throwable hata) {
                    Log.e("Chatapp", "Başarısız: "+hata.getMessage());
                    runOnUiThread(() -> Toast.makeText(MesajActivity.this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show());
                }
            })).start();
        }else{
            EThreeParams eThreeParams = new EThreeParams(firebaseUser.getPhoneNumber(),
                    () -> sharedPreference.GetirString(E3KitKullanici.VirgilTokenKey, ""),
                    MesajActivity.this);
            eThree = new EThree(eThreeParams);
            MesajlariGoster();
            gonderBtn.setOnClickListener(v -> MesajGonder());
        }
    }
    boolean gosterildi = false;
    private void MesajlariGoster() {
        gosterildi = true;
        mesajlariGosterQuery = FirebaseDatabase.getInstance().getReference(tabloString).child(firebaseUser.getPhoneNumber()).child(telefonString).orderByKey();
        mesajlariGosterQuery.keepSynced(true);
        mesajlariGosterQuery.addValueEventListener(mesajlariGosterEventListener);
    }

    private void MesajGonder() {
        String mesaj = gonderText.getText().toString();
        if(!mesaj.equals("")){
            veritabani.MesajGonder(eThree, mesaj, telefonString, firebaseUser);
            gonderText.setText("");
        }
    }

    private final ValueEventListener mesajlariGosterEventListener = new ValueEventListener() {

        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            new Thread(() -> {
                Card card = eThree.findUser(telefonString).get();
                runOnUiThread(() -> SifreliMesajlariGoster(card ,snapshot));
            }).start();
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };

    @SuppressLint("NotifyDataSetChanged")
    private void SifreliMesajlariGoster(Card card, DataSnapshot snapshot) {
        mesajList.clear();
        for (DataSnapshot dataSnapshot : snapshot.getChildren()){
            Mesaj mesaj = dataSnapshot.getValue(Mesaj.class);
            if (mesajList.size() > 0){
                if (!ChatApp.MesajTarihiBul(mesaj.getTarih(), false).equals(ChatApp.MesajTarihiBul(mesajList.get(mesajList.size()-1).getTarih(), false))){
                    Mesaj tarihMsj = new Mesaj();
                    tarihMsj.setTarih(mesaj.getTarih());
                    tarihMsj.setTarihGoster(true);
                    tarihMsj.setMesaj(ChatApp.MesajTarihiBul(mesaj.getTarih(), false));
                    mesajList.add(tarihMsj);
                }
            }else{
                Mesaj tarihMsj = new Mesaj();
                tarihMsj.setTarih(mesaj.getTarih());
                tarihMsj.setTarihGoster(true);
                tarihMsj.setMesaj(ChatApp.MesajTarihiBul(mesaj.getTarih(), false));
                mesajList.add(tarihMsj);
            }
            if (!mesaj.isGonderen()){
                if (tabloString.equals(Veritabani.MesajTablosu)){
                    HashMap<String, Object> mapBir = new HashMap<>();
                    mapBir.put(Veritabani.GorulduKey, true);
                    DatabaseReference gorulduOlarakIsaretleBir = FirebaseDatabase.getInstance()
                            .getReference(Veritabani.MesajTablosu+"/"+firebaseUser.getPhoneNumber()+"/"+telefonString+"/"+dataSnapshot.getKey());
                    gorulduOlarakIsaretleBir.updateChildren(mapBir);
                    DatabaseReference gorulduOlarakIsaretleIki = FirebaseDatabase.getInstance()
                            .getReference(Veritabani.MesajTablosu+"/"+telefonString+"/"+firebaseUser.getPhoneNumber()+"/"+dataSnapshot.getKey());
                    gorulduOlarakIsaretleIki.updateChildren(mapBir);
                }
                if (card != null){
                    mesaj.setMesaj(eThree.authDecrypt(mesaj.getMesaj(), card));
                }else{
                    mesaj.setMesaj(getString(R.string.this_message_could_not_be_decrypted));
                }
                mesajList.add(mesaj);
            }else{
                mesaj.setMesaj(eThree.authDecrypt(mesaj.getMesaj()));
                mesajList.add(mesaj);
            }
        }
        mesajAdapter.notifyDataSetChanged();
    }

    private void KisiBilgileriniGoster() {
        DatabaseReference kisiBilgileri = FirebaseDatabase.getInstance().getReference(Veritabani.KullaniciTablosu).child(telefonString);
        kisiBilgileri.keepSynced(true);
        kisiBilgileri.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Kullanici kullanici = snapshot.getValue(Kullanici.class);
                if (kullanici != null){
                    if (kullanici.getOnlineDurumu()){
                        durum.setText(getString(R.string.online));
                    }else{
                        durum.setText(getString(R.string.offline));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void KisininOnlineDurumunuGuncelle(){
        DatabaseReference onlineDurumu = FirebaseDatabase.getInstance().getReference(".info/connected");
        onlineDurumu.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean baglandi = snapshot.getValue(Boolean.class);
                if (baglandi){
                    KisiBilgileriniGoster();
                }else{
                    durum.setText(getString(R.string.offline));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void Geri(){
        if (gosterildi){
            mesajlariGosterQuery.removeEventListener(mesajlariGosterEventListener);
        }
        Intent intent = new Intent(MesajActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
        overridePendingTransition(R.anim.soldan_saga_giris, R.anim.soldan_saga_cikis);
        finish();
    }
    @Override
    public void onBackPressed() {
        Geri();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            Geri();
            return false;
        }
        else{
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gosterildi && !ilkAcilis){
            mesajlariGosterQuery.addValueEventListener(mesajlariGosterEventListener);
        }
        ilkAcilis = false;
        Veritabani.DurumGuncelle(firebaseUser, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gosterildi){
            mesajlariGosterQuery.removeEventListener(mesajlariGosterEventListener);
        }
        Veritabani.DurumGuncelle(firebaseUser, false);
    }
}