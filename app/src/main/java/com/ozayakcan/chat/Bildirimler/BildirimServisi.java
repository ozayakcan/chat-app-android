package com.ozayakcan.chat.Bildirimler;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ozayakcan.chat.Ozellik.Veritabani;

public class BildirimServisi extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null){
            Veritabani veritabani = new Veritabani(getApplicationContext());
            veritabani.TokenKaydet(firebaseUser, s);
        }
    }
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null){
            BildirimiAyarla(remoteMessage);
        }
    }
    private void BildirimiAyarla(RemoteMessage remoteMessage){
        if (remoteMessage.getData().size() > 0){
            String bildirimTuru = remoteMessage.getData().get(BildirimClass.BildirimTuruKey);
            if (bildirimTuru != null){
                if (bildirimTuru.equals(BildirimClass.MesajKey)){
                    BildirimClass.getInstance(getApplicationContext()).MesajBildirimi();
                }else if (bildirimTuru.equals(BildirimClass.GorulduKey)){
                    BildirimClass.getInstance(getApplicationContext()).GorulduGuncelle(remoteMessage.getData().get(BildirimClass.KisiKey));
                }
            }
        }
    }
}
