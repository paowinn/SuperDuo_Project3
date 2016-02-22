package it.jaschke.alexandria;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by alvarpao on 2/22/2016.
 */
public class Utility {

    public static boolean deviceIsConnected(Context context) {
        //Determine if there is an active internet connection
        if(context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

            return ((activeNetwork != null) && activeNetwork.isConnectedOrConnecting());
        }

        return false;
    }
}
