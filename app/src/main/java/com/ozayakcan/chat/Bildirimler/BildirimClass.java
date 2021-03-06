package com.ozayakcan.chat.Bildirimler;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ozayakcan.chat.ChatApp;
import com.ozayakcan.chat.MainActivity;
import com.ozayakcan.chat.MesajActivity;
import com.ozayakcan.chat.Model.BildirimMesaj;
import com.ozayakcan.chat.Model.Kullanici;
import com.ozayakcan.chat.Model.Mesaj;
import com.ozayakcan.chat.Ozellik.MesajFonksiyonlari;
import com.ozayakcan.chat.Ozellik.SharedPreference;
import com.ozayakcan.chat.Ozellik.Veritabani;
import com.ozayakcan.chat.R;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BildirimClass {

    private final Context mContext;
    private BildirimClass(Context context){
        this.mContext = context;
    }

    public static synchronized BildirimClass getInstance(Context context){
        return new BildirimClass(context);
    }

    public static String FCM_URL = "https://fcm.googleapis.com/";
    public static String BildirimTuruKey = "bildirimTuru";
    public static String MesajKey = "mesaj";
    public static String GorulduKey = "goruldu";
    public static String KisiKey = "kisi";
    public static int MesajBildirimiID = 1923;
    public static int MaxMesajSayisi = 7;

    public static long[] TitresimVarsayilan = new long[] {1000, 0, 1000, 0};
    public static long[] TitresimUzun = new long[] {2000, 0, 2000, 0};
    public static long[] TitresimKisa = new long[] {400, 0, 400, 0};

    public void MesajBildirimi() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null && firebaseUser.getPhoneNumber() != null){
            DatabaseReference mesajKisileriRef= FirebaseDatabase.getInstance().getReference(Veritabani.MesajTablosu).child(firebaseUser.getPhoneNumber());
            mesajKisileriRef.keepSynced(true);
            mesajKisileriRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot mesajKisileriSnapshot) {
                    long mesajKisiSayisi = 0;
                    if (mesajKisileriSnapshot.getChildrenCount() > 0){
                        for (DataSnapshot mesajKisileriDataSnapshot : mesajKisileriSnapshot.getChildren()){
                            mesajKisiSayisi++;
                            long finalMesajKisiSayisi = mesajKisiSayisi;
                            if (mesajKisileriDataSnapshot.getKey() != null){
                                DatabaseReference kullaniyicibul = FirebaseDatabase.getInstance().getReference(Veritabani.KullaniciTablosu).child(mesajKisileriDataSnapshot.getKey());
                                kullaniyicibul.keepSynced(true);
                                kullaniyicibul.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot kullaniciSnapshot) {
                                        Kullanici kullanici = kullaniciSnapshot.getValue(Kullanici.class);
                                        if (kullanici == null){
                                            kullanici = new Kullanici();
                                        }
                                        DatabaseReference kisiyiBul = FirebaseDatabase.getInstance().getReference(Veritabani.KullaniciTablosu).child(firebaseUser.getPhoneNumber()).child(Veritabani.KisiTablosu).child(mesajKisileriDataSnapshot.getKey());
                                        kisiyiBul.keepSynced(true);
                                        Kullanici finalKullanici = kullanici;
                                        kisiyiBul.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot kisiSnapshot) {
                                                Kullanici kisi = kisiSnapshot.getValue(Kullanici.class);
                                                String isim = kisi != null ? kisi.getIsim() : mesajKisileriDataSnapshot.getKey();
                                                DatabaseReference mesajlarRef = mesajKisileriDataSnapshot.getRef();
                                                mesajlarRef.keepSynced(true);
                                                mesajlarRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot mesajlarSnapshot) {
                                                        for (DataSnapshot mesajlarDataSnapshot : mesajlarSnapshot.getChildren()){
                                                            Mesaj mesaj = mesajlarDataSnapshot.getValue(Mesaj.class);
                                                            if (mesaj != null && !mesaj.getMesaj().equals("")){
                                                                MesajFonksiyonlari.getInstance(mContext).MesajiKaydet(mesajlarDataSnapshot.getKey(), finalKullanici.getTelefon(), mesaj.getMesaj(), mesaj.getMesajTuru(), Veritabani.MesajDurumuGonderildi, false);
                                                            }
                                                        }
                                                        mesajlarSnapshot.getRef().setValue(null);
                                                        if (mesajKisileriSnapshot.getChildrenCount() == finalMesajKisiSayisi){
                                                            YerelMesajlar(firebaseUser);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }
                        }
                    }else{
                        YerelMesajlar(firebaseUser);
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    private void YerelMesajlar(FirebaseUser firebaseUser) {
        List<BildirimMesaj> bildirimMesajList = new ArrayList<>();
        List<String> mesajKisilerStr = MesajFonksiyonlari.getInstance(mContext).CokKisiliMesajlariGetir(MesajFonksiyonlari.KaydedilecekTur);
        for (int i = 0; i < mesajKisilerStr.size(); i++){
            DatabaseReference kullaniciyiBul = FirebaseDatabase.getInstance().getReference(Veritabani.KullaniciTablosu).child(mesajKisilerStr.get(i));
            kullaniciyiBul.keepSynced(true);
            int finalI = i;
            kullaniciyiBul.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot6231) {
                    Kullanici kullanici = snapshot6231.getValue(Kullanici.class);
                    if (kullanici == null){
                        return;
                    }
                    DatabaseReference kisiyiBul = FirebaseDatabase.getInstance().getReference(Veritabani.KullaniciTablosu).child(firebaseUser.getPhoneNumber()).child(Veritabani.KisiTablosu).child(mesajKisilerStr.get(finalI));
                    kisiyiBul.keepSynced(true);
                    kisiyiBul.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot87542) {
                            Kullanici kisi = snapshot87542.getValue(Kullanici.class);
                            String isim = "";
                            if (kisi != null){
                                isim = kisi.getIsim();
                            }
                            List<Mesaj> kisiMesajListesi = MesajFonksiyonlari.getInstance(mContext).MesajlariGetir(mesajKisilerStr.get(finalI), MesajFonksiyonlari.KaydedilecekTur);
                            long mesajSayisi = 0;
                            for (int i = kisiMesajListesi.size() - 1; i >= 0; i--){
                                if (!kisiMesajListesi.get(i).isGonderen() && !kisiMesajListesi.get(i).isGoruldu()){
                                    mesajSayisi++;
                                    Mesaj mesajlar123 = kisiMesajListesi.get(i);
                                    BildirimMesaj bildirimMesaj = new BildirimMesaj(kullanici.getID(), isim, kullanici.getProfilResmi(), kullanici.getTelefon(), mesajlar123.getMesaj(), mesajlar123.getMesajTuru(), mesajlar123.getTarih(), mesajSayisi);
                                    bildirimMesajList.add(bildirimMesaj);
                                }
                            }
                            if (finalI == mesajKisilerStr.size() - 1 && bildirimMesajList.size() > 0){
                                if (!bildirimMesajList.get(bildirimMesajList.size()-1).getTelefon().equals(ChatApp.SuankiKisiyiBul())){
                                    if (SharedPreference.getInstance(mContext).GetirBoolean(Veritabani.BildirimDurumuKey, true)){
                                        MesajBildirimiGoster(bildirimMesajList);
                                    }
                                }
                                Intent bildirimGonder = new Intent(MesajKey);
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(bildirimGonder);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    public void MesajBildirimiGoster(List<BildirimMesaj> bildirimMesajList) {
        Collections.sort(bildirimMesajList);
        List<BildirimMesaj> bildirimMesajSayisi;
        bildirimMesajSayisi = bildirimMesajList;
        Collections.sort(bildirimMesajSayisi, (o1, o2) -> Long.compare(o2.getMesajSayisi(), o1.getMesajSayisi()));
        Intent acilacakActivity;
        if (bildirimMesajSayisi.get(0).getMesajSayisi() == bildirimMesajList.size()){
            acilacakActivity = new Intent(mContext, MesajActivity.class);
            acilacakActivity.putExtra(Veritabani.IDKey, bildirimMesajList.get(0).getID());
            acilacakActivity.putExtra(Veritabani.IsimKey, bildirimMesajList.get(0).getIsim());
            acilacakActivity.putExtra(Veritabani.TelefonKey, bildirimMesajList.get(0).getTelefon());
            acilacakActivity.putExtra(Veritabani.ProfilResmiKey, bildirimMesajList.get(0).getProfilResmi());
            acilacakActivity.putExtra(Veritabani.MesajTablosu, Veritabani.MesajTablosu);
        }else{
            acilacakActivity = new Intent(mContext, MainActivity.class);
        }
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(
                    mContext,
                    MesajBildirimiID,
                    acilacakActivity,
                    PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_ONE_SHOT
            );
        }else{
            pendingIntent = PendingIntent.getActivity(
                    mContext,
                    MesajBildirimiID,
                    acilacakActivity,
                    PendingIntent.FLAG_ONE_SHOT
            );
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext,MesajKey);
        if (bildirimMesajSayisi.get(0).getMesajSayisi() == bildirimMesajList.size()){
            mBuilder.setSmallIcon(R.drawable.varsayilan_bildirim_simgesi)
                    .setTicker(bildirimMesajList.get(0).getTelefon())
                    .setContentTitle(bildirimMesajList.get(0).getIsim());
        }else{
            mBuilder.setSmallIcon(R.drawable.varsayilan_bildirim_simgesi).setContentTitle(mContext.getString(R.string.app_name));
        }
        mBuilder.setColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
        mBuilder.setWhen(0)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        if (bildirimMesajSayisi.get(0).getMesajSayisi() == bildirimMesajList.size()){
            if (bildirimMesajList.size() > 1){
                mBuilder.setStyle(cokluBildirim(bildirimMesajList, true));
            }
        }else{
            mBuilder.setStyle(cokluBildirim(bildirimMesajList, false));
        }
        if (bildirimMesajSayisi.get(0).getMesajSayisi() == bildirimMesajList.size()){
            int bildirimID = R.drawable.ic_profil_resmi;
            if (!bildirimMesajList.get(0).getProfilResmi().equals(Veritabani.VarsayilanDeger)){
                Executor executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                executor.execute(() -> {
                    Bitmap profilResmi;
                    try {
                        URL kisiUrl = new URL(bildirimMesajList.get(0).getProfilResmi());
                        profilResmi = BitmapFactory.decodeStream(kisiUrl.openConnection().getInputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                        profilResmi = BitmapFactory.decodeResource(mContext.getResources(), bildirimID);
                    }
                    Bitmap finalProfilResmi = profilResmi;
                    handler.post(() -> {
                        mBuilder.setLargeIcon(finalProfilResmi);
                        DevamEt(mBuilder, bildirimMesajList);
                    });
                });
            }else{
                Bitmap profilResmi = BitmapFactory.decodeResource(mContext.getResources(), bildirimID);
                mBuilder.setLargeIcon(profilResmi);
                DevamEt(mBuilder, bildirimMesajList);
            }
        }else{
            DevamEt(mBuilder, bildirimMesajList);
        }

    }
    private void DevamEt(NotificationCompat.Builder mBuilder, List<BildirimMesaj> bildirimMesajList){
        Notification notification;
        if (bildirimMesajList.size() > 1){
            mBuilder.setContentText(mContext.getResources().getString(R.string.s_new_messages).replace("%s", bildirimMesajList.size()+""));
        }else {
            mBuilder.setContentText(bildirimMesajList.get(0).getMesaj());
        }
        mBuilder.setOnlyAlertOnce(false);
        mBuilder.setPriority(SharedPreference.getInstance(mContext).GetirBoolean(Veritabani.BildirimOncelikKey, true) ?
                NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT);
        long titresim = SharedPreference.getInstance(mContext).GetirLong(Veritabani.BildirimTitresimKey, 0);
        switch ((int) titresim){
            case 1:
                mBuilder.setVibrate(TitresimUzun);
                break;
            case 2:
                mBuilder.setVibrate(TitresimKisa);
                break;
            default:
                mBuilder.setVibrate(TitresimVarsayilan);
                break;
        }
        long isik = SharedPreference.getInstance(mContext).GetirLong(Veritabani.BildirimIsigiKey, 1);
        switch ((int) isik){
            case 1:
                mBuilder.setLights(Color.WHITE, 3000, 3000);
                break;
            case 2:
                mBuilder.setLights(Color.CYAN, 3000, 3000);
                break;
            case 3:
                mBuilder.setLights(Color.BLUE, 3000, 3000);
                break;
            case 4:
                mBuilder.setLights(Color.RED, 3000, 3000);
                break;
            case 5:
                mBuilder.setLights(Color.GREEN, 3000, 3000);
                break;
            case 6:
                mBuilder.setLights(Color.YELLOW, 3000, 3000);
                break;
        }
        Uri bildirimSesi = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (!SharedPreference.getInstance(mContext).GetirString(Veritabani.BildirimTonuKey, "").equals("")){
            bildirimSesi = Uri.parse(SharedPreference.getInstance(mContext).GetirString(Veritabani.BildirimTonuKey, ""));
        }
        if (SharedPreference.getInstance(mContext).GetirBoolean(Veritabani.BildirimSesiKey, true)){
            mBuilder.setSound(bildirimSesi);
        }
        notification = mBuilder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(MesajKey,
                    MesajKey,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(MesajKey);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(MesajBildirimiID, notification);
    }
    private NotificationCompat.InboxStyle cokluBildirim(List<BildirimMesaj> bildirimMesajList, boolean birKisi) {
        NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();
        int baslangic = 0;
        if (bildirimMesajList.size() >= MaxMesajSayisi){
            baslangic = bildirimMesajList.size() - MaxMesajSayisi;
        }
        for (int i = baslangic; i < bildirimMesajList.size(); i++) {
            SpannableString asilMesaj = MesajFonksiyonlari.getInstance(mContext).MesajiGoster(bildirimMesajList.get(i).getMesaj(), bildirimMesajList.get(i).getMesajTuru());
            if (birKisi){
                inbox.addLine(asilMesaj);
            }else{
                inbox.addLine(bildirimMesajList.get(i).getIsim() + ": " + asilMesaj);
            }
        }
        if (birKisi){
            inbox.setBigContentTitle(bildirimMesajList.get(0).getIsim());
        }else{
            inbox.setBigContentTitle(mContext.getString(R.string.app_name));
        }
        inbox.setSummaryText(mContext.getString(R.string.s_new_messages).replace("%s", bildirimMesajList.size()+""));
        return inbox;
    }

    public static void MesajBildiriminiKaldir(Context mContext){
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(MesajKey,
                    MesajKey,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(MesajKey);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.cancel(MesajBildirimiID);
    }

    public interface BildirimListener{
        void Gonderildi();
        void Gonderilmedi();
    }
    public static void MesajBildirimiYolla(String token, BildirimListener bildirimListener){
        RetrofitAyarlari retrofitAyarlari = RetrofitClient.getClient(FCM_URL).create(RetrofitAyarlari.class);
        DataMesaj data = new DataMesaj(MesajKey);
        Gonder gonder = new Gonder(data, token);
        retrofitAyarlari.bildirimGonder(gonder).enqueue(new Callback<Sonuc>() {
            @Override
            public void onResponse(@NonNull Call<Sonuc> call, @NonNull Response<Sonuc> response) {
                bildirimListener.Gonderildi();
            }

            @Override
            public void onFailure(@NonNull Call<Sonuc> call, @NonNull Throwable t) {
                bildirimListener.Gonderilmedi();
            }
        });
    }
    public void GorulduGuncelle(String kisi){
        List<Mesaj> mesajList = MesajFonksiyonlari.getInstance(mContext).MesajlariGetir(kisi, MesajFonksiyonlari.KaydedilecekTur);
        if (mesajList.size() > 0){
            for (int i = mesajList.size() - 1; i >= 0; i--){
                Mesaj mesaj = mesajList.get(i);
                if (mesajList.get(i).isGonderen() && !mesajList.get(i).isGoruldu()){
                    mesaj.setMesajDurumu(Veritabani.MesajDurumuGonderildi);
                    mesaj.setGoruldu(true);
                    mesajList.set(i, mesaj);
                }
            }
            MesajFonksiyonlari.getInstance(mContext).MesajDuzenle(kisi, mesajList);
            Intent bildirimGonder = new Intent(GorulduKey);
            bildirimGonder.putExtra(KisiKey, kisi);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(bildirimGonder);
        }
    }
    public void GorulduBildirimiYolla(String token, String kisi, String ben){
        RetrofitAyarlari retrofitAyarlari = RetrofitClient.getClient(FCM_URL).create(RetrofitAyarlari.class);
        DataGoruldu data = new DataGoruldu(GorulduKey, ben);
        Gonder gonder = new Gonder(data, token);
        retrofitAyarlari.bildirimGonder(gonder).enqueue(new Callback<Sonuc>() {
            @Override
            public void onResponse(@NonNull Call<Sonuc> call, @NonNull Response<Sonuc> response) {

            }

            @Override
            public void onFailure(@NonNull Call<Sonuc> call, @NonNull Throwable t) {

            }
        });
        List<Mesaj> mesajList = MesajFonksiyonlari.getInstance(mContext).MesajlariGetir(kisi, MesajFonksiyonlari.KaydedilecekTur);
        if (mesajList.size() > 0) {
            for (int i = 0; i < mesajList.size(); i++) {
                Mesaj mesaj = mesajList.get(i);
                if (!mesaj.isGonderen() && !mesaj.isGoruldu()) {
                    mesaj.setMesajDurumu(Veritabani.MesajDurumuGonderildi);
                    mesaj.setGoruldu(true);
                    mesajList.set(i, mesaj);
                }
            }
            MesajFonksiyonlari.getInstance(mContext).MesajDuzenle(kisi, mesajList);
        }
    }
}
