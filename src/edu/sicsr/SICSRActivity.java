package edu.sicsr;

import com.example.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;
import android.widget.Toast;

public class SICSRActivity extends Activity {
	private boolean running;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		running = false;
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (LoggingService.class.getName().equals(service.service.getClassName())) {
	        	running = true;
	        }
	    }
	    if(running == false) {
	    	Intent i = new Intent(getApplicationContext(), LoggingService.class);
    		startService(i);
    		Toast.makeText(getApplicationContext(), "The logging process has started", Toast.LENGTH_SHORT).show();
	    }
	  //  TelephonyManager telephonyManager  =  
	    //     ( TelephonyManager )getSystemService( Context.TELEPHONY_SERVICE );
	    TextView textView =  (TextView)(findViewById(R.id.textView));
	    WifiManager wifiMan = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiMan.getConnectionInfo();
		
		String macAddress = wifiInfo.getMacAddress();
		if(macAddress != null && macAddress.trim().length() != 0) {
			getSharedPreferences("macfile", 0).edit().putString("mac", macAddress).commit();
		} else if(!(getSharedPreferences("macfile", 0).getString("mac", "null").equals("null"))) {
			macAddress = getSharedPreferences("macfile", 0).getString("mac", "null");
		}
	   // textView.setText(textView.getText().toString() + Html.fromHtml("<br/>") + "Unique ID: " + telephonyManager.getDeviceId());
		textView.setText(textView.getText().toString() + Html.fromHtml("<br/>") + "Unique ID: " + macAddress);
	  //textView.setText(textView.getText().toString() + Html.fromHtml("<br/>") + "Unique ID: " +  Secure.getString(getContentResolver(), Secure.ANDROID_ID));
		
	 }

}
