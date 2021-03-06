package com.ozayakcan.chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ozayakcan.chat.Giris.BilgilerActivity;
import com.ozayakcan.chat.Giris.GirisActivity;
import com.ozayakcan.chat.Model.Kullanici;
import com.ozayakcan.chat.Ozellik.SharedPreference;
import com.ozayakcan.chat.Ozellik.Veritabani;

public class SSActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ss);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        /*if (BuildConfig.DEBUG){
            firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance());
        }*/
        firebaseAppCheck.installAppCheckProviderFactory(
        SafetyNetAppCheckProviderFactory.getInstance());
    }

    @Override
    protected void onStart() {
        super.onStart();
        KullaniciyiKontrolEt();
    }

    private void KullaniciyiKontrolEt() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            //Giriş Yapıldı
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(Veritabani.KullaniciTablosu).child(user.getPhoneNumber());
			databaseReference.keepSynced(true);
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Kullanici kullanici = snapshot.getValue(Kullanici.class);
                    if (kullanici == null){
                        Intent intent = new Intent(SSActivity.this, BilgilerActivity.class);
                        intent.putExtra(Veritabani.ProfilResmiKey, Veritabani.VarsayilanDeger);
                        intent.putExtra(Veritabani.IsimKey, "");
                        startActivity(intent);
                    }else{
                        if (SharedPreference.getInstance(SSActivity.this).GetirBoolean(SharedPreference.kullaniciKaydedildi, false)){
                            //Kaydedildi
                            startActivity(new Intent(SSActivity.this, MainActivity.class));
                        }else{
                            //Kaydedilmedi
                            Intent intent = new Intent(SSActivity.this, BilgilerActivity.class);
                            intent.putExtra(Veritabani.ProfilResmiKey, kullanici.getProfilResmi());
                            intent.putExtra(Veritabani.IsimKey, kullanici.getIsim());
                            startActivity(intent);
                        }
                    }
                    overridePendingTransition(0,0);
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("Veritabanı", error.getMessage());
                    Toast.makeText(SSActivity.this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            //Giriş yapılmadı
            startActivity(new Intent(SSActivity.this, GirisActivity.class));
            overridePendingTransition(0,0);
            finish();
        }
    }
}