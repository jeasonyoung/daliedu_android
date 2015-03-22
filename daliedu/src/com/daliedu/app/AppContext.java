
package com.daliedu.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Properties;
import java.util.UUID;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;

import com.daliedu.entity.User;
import com.daliedu.util.CyptoUtils;
import com.daliedu.util.FileUtils;
import com.daliedu.util.ImageUtils;
import com.daliedu.util.MethodsCompat;
import com.daliedu.util.StringUtils;

/**
 * ȫ��Ӧ�ó����ࣺ���ڱ���͵���ȫ��Ӧ�����ü�������������
 * 
 * @version 1.0
 */
public class AppContext extends Application {

	public static final int NETTYPE_WIFI = 0x01;
	public static final int NETTYPE_CMWAP = 0x02;
	public static final int NETTYPE_CMNET = 0x03;

	public static final int PAGE_SIZE = 20;// Ĭ�Ϸ�ҳ��С
	private static final int CACHE_TIME = 60 * 60000;// ����ʧЧʱ��
	public static final int LOGINING = 1;// ���ڵ�¼
	public static final int LOGIN_FAIL = -1;// ��¼ʧ��
	public static final int LOGINED = 2;// �Ѿ���¼
	public static final int UNLOGIN = 0;// û�е�¼
	public static final int LOCAL_LOGINED = 3; // ���ص�¼

	private int loginState = 0; // ��¼״̬
	private String loginUid = null; // ��¼�û���id
	private String username = null;// ��¼�õ��û���
	private String nickname = null;// �ǳ�
	private boolean isAutoCheckuped, isAutoLogined,hasNewVersion;

	public boolean isHasNewVersion() {
		return hasNewVersion;
	}

	public void setHasNewVersion(boolean hasNewVersion) {
		this.hasNewVersion = hasNewVersion;
	}

	public boolean isAutoCheckuped() {
		return isAutoCheckuped;
	}

	public void setAutoCheckuped(boolean isAutoCheckuped) {
		this.isAutoCheckuped = isAutoCheckuped;
	}

	public boolean isAutoLogined() {
		return isAutoLogined;
	}

	public void setAutoLogined(boolean isAutoLogined) {
		this.isAutoLogined = isAutoLogined;
	}

	private Hashtable<String, Object> memCacheRegion = new Hashtable<String, Object>();

	private String saveImagePath;// ����ͼƬ·��

	private Handler unLoginHandler = new Handler() {
		public void handleMessage(Message msg) {
//			if (msg.what == 1) {
//				UIHelper.ToastMessage(AppContext.this,
//						getString(R.string.msg_login_error));
//				UIHelper.showLoginDialog(AppContext.this);
//			}
		}
	};
	
	@Override
	public void onCreate() {
		super.onCreate();
		// ע��App�쳣����������
//		 Thread.setDefaultUncaughtExceptionHandler(AppException
//		 .getAppExceptionHandler());
		init();
	}

	/**
	 * ��ʼ��
	 */
	private void init() {
		// ���ñ���ͼƬ��·��
		saveImagePath = getProperty(AppConfig.SAVE_IMAGE_PATH);
		if (StringUtils.isEmpty(saveImagePath)) {
			setProperty(AppConfig.SAVE_IMAGE_PATH,
					AppConfig.DEFAULT_SAVE_IMAGE_PATH);
			saveImagePath = AppConfig.DEFAULT_SAVE_IMAGE_PATH;
		}
	}

	//���̱�ɱ����application��Ķ���ʧ
	public void recoverLoginStatus()
	{
		String name = getProperty("user.account");
		if(username==null&&name!=null)
		{
			username = name;
			loginState = LOGINED;
		}
	}
	/**
	 * ��⵱ǰϵͳ�����Ƿ�Ϊ����ģʽ
	 * 
	 * @return
	 */
	public boolean isAudioNormal() {
		AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		return mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
	}

	/**
	 * Ӧ�ó����Ƿ񷢳���ʾ��
	 * 
	 * @return
	 */
	public boolean isAppSound() {
		return isAudioNormal() && isVoice();
	}

	/**
	 * ��������Ƿ����
	 * 
	 * @return
	 */
	public boolean isNetworkConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return ni != null && ni.isConnectedOrConnecting();
	}

	/**
	 * ��ȡ��ǰ��������
	 * 
	 * @return 0��û������ 1��WIFI���� 2��WAP���� 3��NET����
	 */
	public int getNetworkType() {
		int netType = 0;
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo == null) {
			return netType;
		}
		int nType = networkInfo.getType();
		if (nType == ConnectivityManager.TYPE_MOBILE) {
			String extraInfo = networkInfo.getExtraInfo();
			if (!StringUtils.isEmpty(extraInfo)) {
				if (extraInfo.toLowerCase().equals("cmnet")) {
					netType = NETTYPE_CMNET;
				} else {
					netType = NETTYPE_CMWAP;
				}
			}
		} else if (nType == ConnectivityManager.TYPE_WIFI) {
			netType = NETTYPE_WIFI;
		}
		return netType;
	}

	/**
	 * �жϵ�ǰ�汾�Ƿ����Ŀ��汾�ķ���
	 * 
	 * @param VersionCode
	 * @return
	 */
	public static boolean isMethodsCompat(int VersionCode) {
		int currentVersion = android.os.Build.VERSION.SDK_INT;
		return currentVersion >= VersionCode;
	}

	/**
	 * ��ȡApp��װ����Ϣ
	 * 
	 * @return
	 */
	public PackageInfo getPackageInfo() {
		PackageInfo info = null;
		try {
			info = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace(System.err);
		}
		if (info == null)
			info = new PackageInfo();
		return info;
	}

	/**
	 * ��ȡ�汾��
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getVersionName() {
		return getPackageInfo().versionName;
	}
	public int getVersionCode()
	{
		return getPackageInfo().versionCode;
	}
	/**
	 * ��ȡAppΨһ��ʶ
	 * 
	 * @return
	 */
	public String getAppId() {
		String uniqueID = getProperty(AppConfig.CONF_APP_UNIQUEID);
		if (StringUtils.isEmpty(uniqueID)) {
			uniqueID = UUID.randomUUID().toString();
			setProperty(AppConfig.CONF_APP_UNIQUEID, uniqueID);
		}
		return uniqueID;
	}

	/**
	 * ��ȡ�豸Ψһ��ʶ
	 */
	public String getDeviceId() {
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getDeviceId();
	}

	/**
	 * �û��Ƿ��¼
	 * 
	 * @return
	 */
	public int getLoginState() {
		return loginState;
	}

	/**
	 * ��ȡ��¼�û�id
	 * 
	 * @return
	 */
	public String getLoginUid() {
		return this.loginUid;
	}

	public String getUsername() {
		return this.username;
	}

	public String getNickname() {
		return this.nickname;
	}

	/**
	 * �û�ע��
	 */
	public void Logout() {
		ApiClient.cleanCookie();
		this.cleanCookie();
		this.loginState = UNLOGIN;
		this.loginUid = null;
		this.username = this.nickname = null;
	}

	/**
	 * δ��¼���޸������Ĵ���
	 */
	public Handler getUnLoginHandler() {
		return this.unLoginHandler;
	}

	//
	// /**
	// * ��ʼ���û���¼��Ϣ
	// */
	// public void initLoginInfo() {
	// User loginUser = getLoginInfo();
	// if(loginUser!=null && loginUser.getUid()!=null &&
	// loginUser.isRememberMe()){
	// this.loginUid = loginUser.getUid();
	// this.login = true;
	// }else{
	// this.Logout();
	// }
	// }

	/**
	 * �û���¼��֤
	 * 
	 * @param account
	 * @param pwd
	 * @return
	 * @throws AppException
	 */
	// public User loginVerify(String account, String pwd) throws AppException {
	// return ApiClient.login(this, account, pwd);
	// }

	/**
	 * �����¼��Ϣ
	 * 
	 * @param username
	 * @param pwd
	 */
	public void saveLoginInfo(final User user) {
		this.loginUid = user.getUid();
		this.loginState = LOGINED;
		this.username = user.getUsername();
		setProperties(new Properties() {
			{
				setProperty("user.uid", String.valueOf(user.getUid()));
				// setProperty("user.name", user.getNickname());
				// setProperty("user.face",
				// FileUtils.getFileName(user.getFace()));// �û�ͷ��-�ļ���
				setProperty("user.account", user.getUsername());
				setProperty("user.pwd",
						CyptoUtils.encode("youeclass", user.getPassword()));
				// setProperty("user.location", user.getLocation());
				// setProperty("user.deviceid", user.getDeviceId());
				// setProperty("user.isRememberMe",
				// String.valueOf(user.isRememberMe()));//�Ƿ��ס�ҵ���Ϣ
			}
		});
	}

	public void saveLocalLoginInfo(String username) {
		this.loginState = LOCAL_LOGINED;
		this.username = username;
	}

	/**
	 * �����¼��Ϣ
	 */
	public void cleanLoginInfo() {
		this.loginUid = null;
		this.loginState = UNLOGIN;
		this.username = this.nickname = null;
		// removeProperty("user.uid", "user.name", "user.face", "user.account",
		// "user.pwd", "user.location", "user.followers", "user.fans",
		// "user.score", "user.isRememberMe");
	}

	/**
	 * ��ȡ��¼��Ϣ
	 * 
	 * @return
	 */
	public User getLoginInfo() {
		User lu = new User();
		lu.setUid(getProperty("user.uid"));
		lu.setUsername(getProperty("user.account"));
		lu.setPassword(CyptoUtils.decode("changheng", getProperty("user.pwd")));
		return lu;
	}

	/**
	 * �����û�ͷ��
	 * 
	 * @param fileName
	 * @param bitmap
	 */
	public void saveUserFace(String fileName, Bitmap bitmap) {
		try {
			ImageUtils.saveImage(this, fileName, bitmap);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ��ȡ�û�ͷ��
	 * 
	 * @param key
	 * @return
	 * @throws AppException
	 */
	public Bitmap getUserFace(String key) throws AppException {
		FileInputStream fis = null;
		try {
			fis = openFileInput(key);
			return BitmapFactory.decodeStream(fis);
		} catch (Exception e) {
			throw AppException.run(e);
		} finally {
			try {
				fis.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * �Ƿ������ʾ����ͼƬ
	 * 
	 * @return
	 */
	public boolean isLoadImage() {
		String perf_loadimage = getProperty(AppConfig.CONF_LOAD_IMAGE);
		// Ĭ���Ǽ��ص�
		if (StringUtils.isEmpty(perf_loadimage))
			return true;
		else
			return StringUtils.toBool(perf_loadimage);
	}

	/**
	 * �����Ƿ��������ͼƬ
	 * 
	 * @param b
	 */
	public void setConfigLoadimage(boolean b) {
		setProperty(AppConfig.CONF_LOAD_IMAGE, String.valueOf(b));
	}

	/**
	 * �Ƿ񷢳���ʾ��
	 * 
	 * @return
	 */
	public boolean isVoice() {
		String perf_voice = getProperty(AppConfig.CONF_VOICE);
		// Ĭ���ǿ�����ʾ����
		if (StringUtils.isEmpty(perf_voice))
			return true;
		else
			return StringUtils.toBool(perf_voice);
	}

	/**
	 * �����Ƿ񷢳���ʾ��
	 * 
	 * @param b
	 */
	public void setConfigVoice(boolean b) {
		setProperty(AppConfig.CONF_VOICE, String.valueOf(b));
	}

	/**
	 * �Ƿ�����������
	 * 
	 * @return
	 */
	public boolean isCheckUp() {
		String perf_checkup = getProperty(AppConfig.CONF_CHECKUP);
		// Ĭ���ǿ���
		if (StringUtils.isEmpty(perf_checkup))
			return true;
		else
			return StringUtils.toBool(perf_checkup);
	}

	/**
	 * �Ƿ��Զ���¼
	 */
	public boolean isAutoLogin() {
		String perf_autoLogin = getProperty(AppConfig.CONF_AUTOLOGIN);
		if (StringUtils.isEmpty(perf_autoLogin))
			return false;
		else
			return StringUtils.toBool(perf_autoLogin);
	}

	/**
	 * ��������������
	 * 
	 * @param b
	 */
	public void setConfigCheckUp(boolean b) {
		setProperty(AppConfig.CONF_CHECKUP, String.valueOf(b));
	}

	/**
	 * �Ƿ����һ���
	 * 
	 * @return
	 */
	public boolean isScroll() {
		String perf_scroll = getProperty(AppConfig.CONF_SCROLL);
		// Ĭ���ǹر����һ���
		if (StringUtils.isEmpty(perf_scroll))
			return false;
		else
			return StringUtils.toBool(perf_scroll);
	}

	/**
	 * �������Ļ���
	 */
	public void cleanCookie() {
		removeProperty(AppConfig.CONF_COOKIE);
	}

	/**
	 * �жϻ��������Ƿ�ɶ�
	 * 
	 * @param cachefile
	 * @return
	 */
	private boolean isReadDataCache(String cachefile) {
		return readObject(cachefile) != null;
	}

	/**
	 * �жϻ����Ƿ����
	 * 
	 * @param cachefile
	 * @return
	 */
	private boolean isExistDataCache(String cachefile) {
		boolean exist = false;
		File data = getFileStreamPath(cachefile);
		if (data.exists())
			exist = true;
		return exist;
	}

	/**
	 * �жϻ����Ƿ�ʧЧ
	 * 
	 * @param cachefile
	 * @return
	 */
	public boolean isCacheDataFailure(String cachefile) {
		boolean failure = false;
		File data = getFileStreamPath(cachefile);
		if (data.exists()
				&& (System.currentTimeMillis() - data.lastModified()) > CACHE_TIME)
			failure = true;
		else if (!data.exists())
			failure = true;
		return failure;
	}

	/**
	 * ���app����
	 */
	public void clearAppCache() {
		// ���webview����
		// File file = CacheManager.getCacheFileBaseDir();
		// if (file != null && file.exists() && file.isDirectory()) {
		// for (File item : file.listFiles()) {
		// item.delete();
		// }
		// file.delete();
		// }
		deleteDatabase("webview.db");
		deleteDatabase("webview.db-shm");
		deleteDatabase("webview.db-wal");
		deleteDatabase("webviewCache.db");
		deleteDatabase("webviewCache.db-shm");
		deleteDatabase("webviewCache.db-wal");
		// ������ݻ���
		clearCacheFolder(getFilesDir(), System.currentTimeMillis());
		clearCacheFolder(getCacheDir(), System.currentTimeMillis());
		// 2.2�汾���н�Ӧ�û���ת�Ƶ�sd���Ĺ���
		if (isMethodsCompat(android.os.Build.VERSION_CODES.FROYO)) {
			clearCacheFolder(MethodsCompat.getExternalCacheDir(this),
					System.currentTimeMillis());
		}
		// ����༭���������ʱ����
		Properties props = getProperties();
		for (Object key : props.keySet()) {
			String _key = key.toString();
			if (_key.startsWith("temp"))
				removeProperty(_key);
		}
	}

	/**
	 * �������Ŀ¼
	 * 
	 * @param dir
	 *            Ŀ¼
	 * @param numDays
	 *            ��ǰϵͳʱ��
	 * @return
	 */
	private int clearCacheFolder(File dir, long curTime) {
		int deletedFiles = 0;
		if (dir != null && dir.isDirectory()) {
			try {
				for (File child : dir.listFiles()) {
					if (child.isDirectory()) {
						deletedFiles += clearCacheFolder(child, curTime);
					}
					if (child.lastModified() < curTime) {
						if (child.delete()) {
							deletedFiles++;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return deletedFiles;
	}

	/**
	 * �����󱣴浽�ڴ滺����
	 * 
	 * @param key
	 * @param value
	 */
	public void setMemCache(String key, Object value) {
		memCacheRegion.put(key, value);
	}

	/**
	 * ���ڴ滺���л�ȡ����
	 * 
	 * @param key
	 * @return
	 */
	public Object getMemCache(String key) {
		return memCacheRegion.get(key);
	}

	/**
	 * ������̻���
	 * 
	 * @param key
	 * @param value
	 * @throws IOException
	 */
	public void setDiskCache(String key, String value) throws IOException {
		FileOutputStream fos = null;
		try {
			fos = openFileOutput("cache_" + key + ".data", Context.MODE_PRIVATE);
			fos.write(value.getBytes());
			fos.flush();
		} finally {
			try {
				fos.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * ��ȡ���̻�������
	 * 
	 * @param key
	 * @return
	 * @throws IOException
	 */
	public String getDiskCache(String key) throws IOException {
		FileInputStream fis = null;
		try {
			fis = openFileInput("cache_" + key + ".data");
			byte[] datas = new byte[fis.available()];
			fis.read(datas);
			return new String(datas);
		} finally {
			try {
				fis.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * �������
	 * 
	 * @param ser
	 * @param file
	 * @throws IOException
	 */
	public boolean saveObject(Serializable ser, String file) {
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = openFileOutput(file, MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(ser);
			oos.flush();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				oos.close();
			} catch (Exception e) {
			}
			try {
				fos.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * ���㻺���С
	 * 
	 * @return
	 */
	public String calculateCacheSize() {
		long fileSize = 0;
		String cacheSize = "0KB";
		File filesDir = getFilesDir();
		File cacheDir = getCacheDir();

		fileSize += FileUtils.getDirSize(filesDir);
		fileSize += FileUtils.getDirSize(cacheDir);

		// 2.2�汾���н�Ӧ�û���ת�Ƶ�sd���Ĺ���
		if (AppContext.isMethodsCompat(android.os.Build.VERSION_CODES.FROYO)) {
			File externalCacheDir = MethodsCompat.getExternalCacheDir(this);
			fileSize += FileUtils.getDirSize(externalCacheDir);
		}
		if (fileSize > 0)
			cacheSize = FileUtils.formatFileSize(fileSize);
		return cacheSize;
	}

	/**
	 * ��ȡ����
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public Serializable readObject(String file) {
		if (!isExistDataCache(file))
			return null;
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = openFileInput(file);
			ois = new ObjectInputStream(fis);
			return (Serializable) ois.readObject();
		} catch (FileNotFoundException e) {
		} catch (Exception e) {
			e.printStackTrace();
			// �����л�ʧ�� - ɾ�������ļ�
			if (e instanceof InvalidClassException) {
				File data = getFileStreamPath(file);
				data.delete();
			}
		} finally {
			try {
				ois.close();
			} catch (Exception e) {
			}
			try {
				fis.close();
			} catch (Exception e) {
			}
		}
		return null;
	}

	public boolean containsProperty(String key) {
		Properties props = getProperties();
		return props.containsKey(key);
	}

	public void setProperties(Properties ps) {
		AppConfig.getAppConfig(this).set(ps);
	}

	public Properties getProperties() {
		return AppConfig.getAppConfig(this).get();
	}

	public void setProperty(String key, String value) {
		AppConfig.getAppConfig(this).set(key, value);
	}

	public String getProperty(String key) {
		return AppConfig.getAppConfig(this).get(key);
	}

	public void removeProperty(String... key) {
		AppConfig.getAppConfig(this).remove(key);
	}

	/**
	 * ��ȡ�ڴ��б���ͼƬ��·��
	 * 
	 * @return
	 */
	public String getSaveImagePath() {
		return saveImagePath;
	}

	/**
	 * �����ڴ��б���ͼƬ��·��
	 * 
	 * @return
	 */
	public void setSaveImagePath(String saveImagePath) {
		this.saveImagePath = saveImagePath;
	}

	public void setLoginState(int loginState) {
		this.loginState = loginState;
	}


//	public AppUpdate getAppUpdate() throws AppException {
//		AppUpdate update = null;
//		String key = "appUpdateInfo";
//		if(!isNetworkConnected())
//		{
//			throw AppException.http(0);
//		}
//		if(isReadDataCache(key)) //�ɶ�
//		{
//			System.out.println("�ɶ�..........");
//			update = (AppUpdate) readObject(key);
//			if(!update.isNeedUpdate(getVersionCode()))
//			{
//				update = ApiClient.checkVersion(this);
//				if (update != null) {
//					update.setCacheKey(key);
//					saveObject(update, key);
//				}
//			}
//		}else
//		{
//			System.out.println("���ɶ�...........");
//			update = ApiClient.checkVersion(this);
//			if (update != null) {
//				update.setCacheKey(key);
//				saveObject(update, key);
//			}
//		}
//		System.out.println(update);
//		return update;
//	}
//	public AppUpdate isNeedUpdate()
//	{
//		AppUpdate update = (AppUpdate)readObject("appUpdateInfo");
//		if(update == null) return null;
//		if(update.getVersionCode()>getVersionCode())
//		{
//			return update;
//		}
//		return null;
//	}
}