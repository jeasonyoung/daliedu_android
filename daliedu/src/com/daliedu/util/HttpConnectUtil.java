package com.daliedu.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.util.Log;

import com.daliedu.app.AppContext;

public class HttpConnectUtil {
	private static String appCookie;
	public static void cleanCookie() {
		appCookie = "";
	}

	private static String getCookie(AppContext appContext) {
		if (appCookie == null || appCookie == "") {
			appCookie = appContext.getProperty("cookie");
		}
		return appCookie;
	}
	public static String httpGetRequest(AppContext appContext, String urladdr)
			throws Exception {
		return baseHttpGet(appContext,urladdr,5000);
		
	}
	public static String baseHttpGet(AppContext appContext, String urladdr,int millis)throws Exception
	{
		System.out.println("get url = " + urladdr);
		HttpURLConnection conn = null;
		BufferedReader br = null;
		String result = "";
		try {
			URL url = new URL(urladdr);
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(millis);
			conn.setReadTimeout(millis);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Cookie", getCookie(appContext));
			// ¼ì²éÍøÂç
			conn.connect();
			// Á¬½Ó´íÎó
			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
				Log.d(appContext.toString(), "ÇëÇó´íÎó");
				throw new Exception();
			}else if(conn.getResponseCode() == HttpURLConnection.HTTP_OK)
			{
				Map<String,List<String>> map=conn.getHeaderFields();
				Set<String> set=map.keySet();
				for (Iterator iterator = set.iterator(); iterator.hasNext();) {
					String key = (String) iterator.next();
					if (key!=null&&key.equals("Set-Cookie")) {
						List<String> list = map.get(key);
						StringBuilder builder = new StringBuilder();
						for (String str : list) {
							builder.append(str);
						}
						String tmpcookies = builder.toString();
						System.out.println("cookies == "+ tmpcookies);
						// ±£´æcookie
						if (appContext != null && tmpcookies != "") {
							appContext.setProperty("cookie", tmpcookies);
							appCookie = tmpcookies;
						}
						break;
					}
				}
			}
			InputStream in = conn.getInputStream();
			br = new BufferedReader(new InputStreamReader(in));
			StringBuffer buf = new StringBuffer();
			String line = null;
			while ((line = br.readLine()) != null) {
				buf.append(line);
			}
			result = buf.toString();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return result;
	}
	
	
	
	public static String httpPost(AppContext appContext,String postUrl,
			Map<String, String> postHeaders, String postEntity)
			throws IOException {

		URL postURL = new URL(postUrl);
		HttpURLConnection httpURLConnection = (HttpURLConnection) postURL
				.openConnection();
		httpURLConnection.setDoOutput(true);
		httpURLConnection.setDoInput(true);
		httpURLConnection.setRequestMethod("POST");
		httpURLConnection.setUseCaches(false);
		httpURLConnection.setInstanceFollowRedirects(true);
		httpURLConnection.setRequestProperty(" Content-Type ",
				" application/x-www-form-urlencoded ");
		httpURLConnection.setRequestProperty("Cookie", getCookie(appContext));
		if (postHeaders != null) {
			for (String pKey : postHeaders.keySet()) {
				httpURLConnection.setRequestProperty(pKey,
						postHeaders.get(pKey));
			}
		}
		if (postEntity != null) {
			DataOutputStream out = new DataOutputStream(
					httpURLConnection.getOutputStream());
			out.writeBytes(postEntity);
			out.flush();
			out.close(); // flush and close
		}
		// connection.connect();
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(httpURLConnection.getInputStream()));
		StringBuilder sbStr = new StringBuilder();
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			sbStr.append(line);
		}
		bufferedReader.close();
		httpURLConnection.disconnect();
		return new String(sbStr.toString().getBytes(), "utf-8");
	}
}
