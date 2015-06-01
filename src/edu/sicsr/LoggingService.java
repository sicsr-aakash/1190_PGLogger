package edu.sicsr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

public class LoggingService extends Service {

	private static final String TAG = "LoggingService";
	private FileOutputStream outputStream;
	private String macAddress;
	private String filename = "apps";
	private String appName = null;
	private String content = "1";
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
	
	@Override
	public void onDestroy() {
		Log.e(TAG, "onDestroy");
	}
	
		
	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		Log.i("Logging Service","Started");
		//if external file is not empty, copy to internal file
	//	while(true)
		{
			new Thread(new Runnable(){
				public void run()
				{
					runInForeground();
				}}).start();
	}
		return START_STICKY;
	}
	


	@SuppressLint("SimpleDateFormat")
	public void runInForeground()
	{
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
     // And From your main() method or any other method
        Timer timer = new Timer();
        timer.schedule(new getApp(), 0, 1000);
	}
	
	class getApp extends TimerTask {
		public void run() {
			ActivityManager am = (ActivityManager)getApplicationContext().getSystemService(ACTIVITY_SERVICE);
			 
			//String packageName = am.getRunningTasks(1).get(0).topActivity.getPackageName();
			try {
				appName = getApplicationContext().getPackageManager().getApplicationLabel(getApplicationContext().getPackageManager().getApplicationInfo(am.getRunningTasks(1).get(0).topActivity.getPackageName(), 0)).toString();
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
			if(Global.apps != null  && !Global.apps.trim().equals("") && !Global.apps.equals(appName))
			{
				//send current time
				sendCurrentStopTime();
				sendCurrentStartTime();
				Global.apps = appName;
			}
			else if(Global.apps == null || Global.apps.trim().equals(""))
			{
				//store app name and start time
				try 
				{
				  //send current time
				  sendCurrentStartTime();
				}
				catch (Exception e)
				{
				  e.printStackTrace();
				}	
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	class ScreenReceiver extends BroadcastReceiver {
	 
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
	            //send current time
	        	sendCurrentStopTime();
	        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
	        	sendCurrentStartTime();
	        }
	    }
	}
	
	@SuppressLint("SimpleDateFormat")
	public void sendCurrentStopTime() {
		try {
			SimpleDateFormat df = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
		  	Date date=new Date(System.currentTimeMillis());
			outputStream = openFileOutput(filename, Context.MODE_PRIVATE|Context.MODE_APPEND);
			outputStream.write(("," + df.format(date)).getBytes());
			outputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		//if device is online and current time minus uptime > 1 hour, update and clear file
		WifiManager wifiMan = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiMan.getConnectionInfo();
		
		macAddress = wifiInfo.getMacAddress();
		if(macAddress != null && macAddress.trim().length() != 0) {
			getSharedPreferences("macfile", 0).edit().putString("mac", macAddress).commit();
		} else if(!(getSharedPreferences("macfile", 0).getString("mac", "null").equals("null"))) {
			macAddress = getSharedPreferences("macfile", 0).getString("mac", "null");
		}
		
		 TelephonyManager    telephonyManager;                                             
         
		    telephonyManager  =  
		         ( TelephonyManager )getSystemService( Context.TELEPHONY_SERVICE );
		                      
		    /*
		     * getDeviceId() function Returns the unique device ID.
		     * for example,the IMEI for GSM and the MEID or ESN for CDMA phones.  
		     */                                                                
		  //  macAddress = telephonyManager.getDeviceId(); 
		ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni != null && ni.isConnected())
		
	//	if(wifiInfo.getNetworkId() == 0 ) //connected
			//&& (Global.upTime == null || System.currentTimeMillis() - 
			//Global.upTime >= (1*60*1000))
		{
			//send to server			
			
			content = getContent();
			 // the URL where the file will be posted
			 String postReceiverUrl = "http://ug.elearning.sicsr.ac.in/local/deviceallocation/log.php";

			 // new HttpClient
			 final HttpClient httpClient = new DefaultHttpClient();

			// post header
			final HttpPost httpPost = new HttpPost(postReceiverUrl);
			

		    List<NameValuePair> params = new ArrayList<NameValuePair>();
		    params.add(new BasicNameValuePair("content",content));
		   // params.add(new BasicNameValuePair("user",macAddr));

		    try {
				httpPost.setEntity(new UrlEncodedFormEntity(params));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}    
		    
			Runnable r = new Runnable() {
				
				@Override
				public void run() {
					try {
							HttpResponse res = httpClient.execute(httpPost);
							
							Global.upTime = System.currentTimeMillis();
							String filename = "apps";
							HttpEntity entity = res.getEntity();
							if(entity != null) {
								try {
									String responseString = EntityUtils.toString(entity, "UTF-8");
									//JSONObject json = new JSONObject(EntityUtils.toString(res.getEntity(), "UTF-8"));
									
									if(responseString.contains("pre")) {
										
										outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
										outputStream.write(("").getBytes());
										
										outputStream.close();
									}
								} catch (ParseException e) {
									e.printStackTrace();
								}
								
							}
						} catch (ClientProtocolException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			new Thread(r).start();
		}	
	}
	
	@SuppressLint("SimpleDateFormat")
	public void sendCurrentStartTime() {
		try {
			SimpleDateFormat df = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
		  	Date date=new Date(System.currentTimeMillis());
			outputStream = openFileOutput(filename, Context.MODE_PRIVATE|Context.MODE_APPEND);
			outputStream.write(("\r\n" + macAddress + ",").getBytes());
			outputStream.write(appName.getBytes());
			outputStream.write(("," + df.format(date)).getBytes());
			
			outputStream.close();
			Global.apps = appName;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public boolean appStopped() {
		String content = getContent();
		
 		if((content.trim()).charAt(content.length()-1) == ',') {
			return false;
		}
		return true;
	}

	public String getContent() {
		File textFile = getFileStreamPath("apps");
		String content = null;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(textFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
		StringBuilder sb = new StringBuilder();
		String line = null;
			try {
				line = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

		while (line != null) {
		    sb.append(line);
		    sb.append('\n');
		    try {
				line = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		content = sb.toString();
		} finally {
		try {
			br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return content;
	}
}
