package com.appeteria.training.gupshup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.appeteria.training.gupshup.Common.Util;

public class MessageActivity extends AppCompatActivity {

    private TextView tvMessage;
    private ProgressBar pbMessage;
    private ConnectivityManager.NetworkCallback networkCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        tvMessage = findViewById(R.id.tvMessage);
        pbMessage = findViewById(R.id.pbMessage);

        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.LOLLIPOP){
            networkCallback = new ConnectivityManager.NetworkCallback(){
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    finish();
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    tvMessage.setText(R.string.no_internet);
                }
            };

            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

            connectivityManager.registerNetworkCallback(new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build(), networkCallback);
        }
    }

    public void btnRetryClick(View v){
       pbMessage.setVisibility(View.VISIBLE);
       if(Util.connectionAvailable(this))
       {
           finish();
       }
       else
       {
           new android.os.Handler().postDelayed(new Runnable() {
               @Override
               public void run() {
                   pbMessage.setVisibility(View.GONE);
               }
           },1000);
       }
    }

    public  void btnCloseClick(View view)
    {
        finishAffinity();
    }

}
