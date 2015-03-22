package com.daliedu;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.daliedu.app.AppContext;
import com.daliedu.util.Constant;
import com.daliedu.util.HttpConnectUtil;
import com.umeng.analytics.MobclickAgent;

public class RegisterActivity extends Activity implements OnClickListener {
	private ImageButton returnbtn;
	private ProgressDialog dialog;
	private EditText username;
	private EditText pwd;
	private EditText email;
	private EditText phone;
	private EditText qq;
	private EditText code;
	private EditText name;
	private Spinner spinner_prov, spinner_city, spinner_exam;
	private CheckBox checkBox;
	private Button getCodeBtn;
	private Button submitBtn;
	private Button treatyBtn;// 协议
	private JSONArray jArray;
	private SharedPreferences abfile;
	private static final int GET_CODE_SUCCESS = 1;
	private static final int GET_CODE_FAILED = -1;
	private static final int GET_CODE_EXCEPTION = 0, CHECK_CODE_EXCEPTION = 4;
	private static final int CHECK_CODE_SUCCESS = 2;
	private static final int CHECK_CODE_FAILED = -2;
	private static final int COUNT = 3;
	private static final int GET_EXAM_JSON = 10;
	private static final int GET_AREA_JSON = 20;
	private static final int EXCEPTION = 30;
	private Handler handler;
	private int count = 60;
	private boolean flag = true;
	private Integer check_phone = 0;
	private AppContext appContext;
	private ArrayList<String> citys = new ArrayList<String>();
	private ArrayAdapter<String> cityAdapter;

	protected void onCreate(Bundle paramBundle) {
		super.onCreate(paramBundle);
		setContentView(R.layout.activity_register);
		appContext = (AppContext) this.getApplication();
		setHandler();
		findViewsById();
	}

	// 初始化控�?
	private void findViewsById() {
		returnbtn = (ImageButton) this.findViewById(R.id.returnbtn);
		username = (EditText) this.findViewById(R.id.reg_userNameText);
		pwd = (EditText) this.findViewById(R.id.reg_userPwd1Text);
		email = (EditText) this.findViewById(R.id.reg_userEmail);
		phone = (EditText) this.findViewById(R.id.reg_userPhone);
		qq = (EditText) this.findViewById(R.id.reg_userQQ);
		code = (EditText) this.findViewById(R.id.reg_code);
		name = (EditText) this.findViewById(R.id.reg_name);
		checkBox = (CheckBox) this.findViewById(R.id.reg_checkBox);
		submitBtn = (Button) this.findViewById(R.id.reg_submitBtn);
		treatyBtn = (Button) this.findViewById(R.id.reg_treatyBtn);
		getCodeBtn = (Button) this.findViewById(R.id.reg_getCode);
		spinner_prov = (Spinner) this.findViewById(R.id.reg_area_prov);
		spinner_city = (Spinner) this.findViewById(R.id.reg_area_city);
		spinner_exam = (Spinner) this.findViewById(R.id.reg_exam);
		returnbtn.setOnClickListener(new ReturnBtnClickListener(this));
		abfile = getSharedPreferences("abfile", 0);
		// 开两个线程加载spinner
		new Thread1().start();
		new Thread2().start();
		submitBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (checkInput()) {
					// 验证输入无误
					dialog = ProgressDialog.show(RegisterActivity.this, null,
							"注册中请稍侯", true, true);
					dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					// �?��个线程去注册
					// new RegisterThread().start();
					// 异步任务注册�?
					String area = spinner_prov.getSelectedItem().toString()+"(省)"+spinner_city.getSelectedItem()+"(市/区)";
					String exam = spinner_exam.getSelectedItem().toString();
					String trueName = name.getText().toString();
					try{
						area = URLEncoder.encode(area, "UTF-8");
						exam = URLEncoder.encode(exam, "UTF-8");
						trueName = URLEncoder.encode(trueName,"UTF-8");
					}catch(Exception e)
					{
						
					}
					String params = "username=" + username.getText().toString()
							+ "&pwd=" + pwd.getText().toString() + "&email="
							+ email.getText().toString() + "&phone="
							+ phone.getText().toString() + "&qq="
							+ qq.getText().toString()+"&area="+area+"&exam="+exam+"&name="+trueName;
					RegisterTask register = new RegisterTask();
					register.execute(Constant.DOMAIN_URL + "mobile/register?"
							+ params);
				}
			}
		});
		treatyBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(RegisterActivity.this,
						TreatyActivity.class);
				RegisterActivity.this.startActivity(intent);
			}
		});
		spinner_prov.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				if (jArray == null)
					return;
				String prov = spinner_prov.getSelectedItem().toString();
				int length = jArray.length();
				String city = "";
				try {
					for (int i = 0; i < length; i++) {
						if (prov.equals(jArray.getJSONObject(i).getString(
								"prov"))) {
							city = jArray.getJSONObject(i).getString("city");
							break;
						}
					}
					citys.clear();
					citys.addAll(Arrays.asList(city.split("\\|")));
					if (cityAdapter != null)
						cityAdapter.notifyDataSetChanged();
					else {
						cityAdapter = new ArrayAdapter<String>(
								RegisterActivity.this,
								android.R.layout.simple_spinner_item, citys);
						cityAdapter
								.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						spinner_city.setAdapter(cityAdapter);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		code.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View arg0, boolean arg1) {
				// TODO Auto-generated method stub
				if (!code.isFocused())
				{
					String msgCode = code.getText().toString();
					if(msgCode==null||"".equals(msgCode))
					{
						showMsg("请填写验证码");
						check_phone = 0;
						return;
					}
					//开一个线程验证验证码
					new CheckThread(msgCode).start();
				}
			}
		});
		getCodeBtn.setOnClickListener(this);
	}

	private void setHandler() {
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				switch (msg.what) {
				case GET_CODE_EXCEPTION:
					getCodeBtn.setText("获取验证码");
					showMsg("获取验证码失败，请检查网络");
					getCodeBtn.setEnabled(true);
					break;
				case GET_CODE_SUCCESS:
					getCodeBtn.setText("获取验证码");
					getCodeBtn.setEnabled(false);
					getCodeBtn.setTextColor(getResources().getColor(
							R.color.grey));
					flag = true;
					new Thread3().start();
					break;
				case GET_CODE_FAILED:
					showMsg("获取验证码失败,请重试");
					getCodeBtn.setEnabled(true);
					break;
				case CHECK_CODE_SUCCESS:
					code.setCompoundDrawablesWithIntrinsicBounds(0, 0,
							(R.drawable.can_regeist), 0);
					check_phone=2;
					break;
				case CHECK_CODE_FAILED:
					code.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
					showMsg("验证码错误,请重新获取");
					check_phone=-2;
					break;
				case CHECK_CODE_EXCEPTION:
					showMsg("暂时连不上服务器");
					check_phone=-1;
					break;
				case COUNT:
					System.out.println("count === "+count);
					if (count == 60) {
						getCodeBtn.setText("获取验证码");
						getCodeBtn.setEnabled(true);
						getCodeBtn.setTextColor(getResources().getColor(
								R.color.white));
					}else{
						getCodeBtn.setText("获取验证码 " + count);
					}
					break;
				case GET_EXAM_JSON:
					String[] exam = (String[]) msg.obj;
					ArrayAdapter<String> s3Adapter = new ArrayAdapter<String>(
							RegisterActivity.this,
							android.R.layout.simple_spinner_item, exam);
					s3Adapter
							.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					spinner_exam.setAdapter(s3Adapter);
					break;
				case GET_AREA_JSON:
					setAreaSpinner();
					break;
				case EXCEPTION:
					showMsg("获取数据出错");
					break;
				}
			}
		};
	}

	private void setAreaSpinner() {
		int length = jArray.length();
		String[] prov = new String[length];
		try {
			for (int i = 0; i < length; i++) {
				prov[i] = jArray.getJSONObject(i).getString("prov");
			}
			ArrayAdapter<String> provAdapter = new ArrayAdapter<String>(
					RegisterActivity.this,
					android.R.layout.simple_spinner_item, prov);
			provAdapter
					.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner_prov.setAdapter(provAdapter);
		} catch (Exception e) {

		}
	}

	// 验证输入
	private boolean checkInput() {
		String str1 = this.username.getText().toString().trim();
		String str2 = this.email.getText().toString().trim();
		String str3 = this.pwd.getText().toString().trim();
		String str4 = this.phone.getText().toString().trim();
		String qqq = this.qq.getText().toString().trim();
		String tName = this.name.getText().toString().trim();
		boolean bool2 = Pattern.compile("^\\d+$").matcher(str1).matches();
		boolean bool3 = Pattern.compile(".*[\u4e00-\u9fa5]+.*").matcher(str1)
				.matches();
		boolean bool4 = Pattern.compile(".{0,2}$").matcher(str1).matches();
		boolean bool5 = Pattern.compile("^[-,_]{1}.*").matcher(str1).matches();
		boolean bool6 = Pattern.compile("^[-,_,0-9,a-z,A-Z]+$").matcher(str1)
				.matches();
		boolean bool = Pattern.compile("^1[3,4,5,6,8]{1}[0-9]{9}$")
				.matcher(str4).matches();
		boolean bool_qq = Pattern.compile("^[1-9]{1}[0-9]{4,}$").matcher(qqq)
				.matches();
		boolean bool_name = Pattern.compile("^[\u4e00-\u9fa5]{2,4}$").matcher(tName)
				.matches();
		if (str1.equals("")) {
			showMsg("用户名不能为空");
			return false;
		}
		if (bool2) {
			showMsg("用户名不能全部为数字!");
			return false;
		}
		if (bool3) {
			showMsg("用户名不能包含汉字");
			return false;
		}
		if (bool4) {
			showMsg("用户名至少3位字符");
			return false;
		}
		if (bool5) {
			showMsg("用户名不能以‘-’或‘_’开头");
			return false;
		}
		if (!bool6) {
			showMsg("用户名只能使用字母,数字,下划线'-'和'_'组成!");
			return false;
		}
		boolean bool7 = Pattern.compile("^.{6,}$").matcher(str3).matches();
		if (str3.equals("")) {
			showMsg("密码不能为空!");
			return false;
		}
		if (!bool7) {
			showMsg("密码至少6位字�");
			return false;
		}
		boolean bool1 = Pattern
				.compile(
						"^([a-z0-9A-Z]+[-|_\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$")
				.matcher(str2).matches();
		if (str2.equals("")) {
			showMsg("邮箱不能为空!");
			return false;
		}
		if (!bool1) {
			showMsg("邮箱格式错误!");
			return false;
		}
		if("".equals(tName))
		{
			showMsg("请填写姓名");
			return false;
		}
		if(!bool_name)
		{
			showMsg("请填写中文姓名");
			return false;
		}
		if (str4.equals("")) {
			showMsg("手机号不能为空");
			return false;
		}
		if (!bool) {
			showMsg("请输入正确的手机号码");
			return false;
		}
		if (("".equals(qqq))) {
			showMsg("QQ号不能为空");
			return false;
		}
		if (!bool_qq) {
			showMsg("请输入正确的QQ号码");
			return false;
		}
		if (!checkBox.isChecked()) {
			showMsg("请仔细阅读网校协议");
			return false;
		}
		if(!check_phone.equals(2)){
			showMsg("验证码不正确");
			return false;
		}
		return true;
	}

	private void showMsg(String msg) {
		Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
	}

	private class Thread1 extends Thread {
		public void run() {
			// TODO Auto-generated method stub
			try {
				InputStream is = RegisterActivity.this.getResources()
						.getAssets().open("area.json");
				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(is));
				StringBuilder sbStr = new StringBuilder();
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					sbStr.append(line);
				}
				bufferedReader.close();
				jArray = new JSONArray(sbStr.toString());
				handler.sendEmptyMessage(GET_AREA_JSON);
			} catch (Exception e) {
				handler.sendEmptyMessage(EXCEPTION);
			}
		}
	}

	private class Thread2 extends Thread {
		public void run() {
			// TODO Auto-generated method stub
			try {
				String url = Constant.DOMAIN_URL + "mobile/exam.json";
				String result = HttpConnectUtil.httpGetRequest(appContext, url);
				System.out.println(result);
				JSONObject json = new JSONObject(result);
				String exam[] = json.getString("exam").split(",");
				Message msg = handler.obtainMessage();
				msg.what = GET_EXAM_JSON;
				msg.obj = exam;
				handler.sendMessage(msg);
			} catch (Exception e) {
				handler.sendEmptyMessage(EXCEPTION);
			}
		}
	}

	private class Thread3 extends Thread {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				while (flag) {
					count--;
					if (count == 0) {
						count = 60;
						handler.sendEmptyMessage(COUNT);
						return;
					}
					handler.sendEmptyMessage(COUNT);
					Thread.sleep(1000);
				}
			} catch (Exception e) {
			}
		}
	}
	private class CheckThread extends Thread{
		private String msgCode;
		public CheckThread(String code) {
			msgCode = code;
		}
		@Override
		public void run() {
			try{
				check_phone = 1; //正在验证
				String result = HttpConnectUtil.httpGetRequest(appContext,Constant.DOMAIN_URL + "user/checkSmsCode?msgCode="+msgCode);
				if ("false".equals(result)) {
					handler.sendEmptyMessage(CHECK_CODE_FAILED);
					return;
				}
				if ("true".equals(result)) {
					handler.sendEmptyMessage(CHECK_CODE_SUCCESS);
					return;
				}
			}catch(Exception e)
			{
				e.printStackTrace();
				handler.sendEmptyMessage(CHECK_CODE_EXCEPTION);
				check_phone = -1;
			}
		}
	}
	private class RegisterTask extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			try {
				return HttpConnectUtil.httpGetRequest(appContext, params[0]);
			} catch (Exception e) {
				e.printStackTrace();
				return "暂时连不上服务器";
			}
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			dialog.dismiss();
			try {
				JSONObject json = new JSONObject(result);
				int ok = json.getInt("OK");
				String msg = json.optString("msg", "");
				String username = json.optString("username", "");
				if (ok == 0)// 注册失败
				{
					Toast.makeText(RegisterActivity.this, msg,
							Toast.LENGTH_LONG).show();
				} else {
					abfile.edit().putString("n", username).commit();
					abfile.edit().putString("p", "").commit();
					Intent intent = new Intent(RegisterActivity.this,
							RegSuccessActivity.class);
					intent.putExtra("username", username);
					RegisterActivity.this.startActivity(intent);
					RegisterActivity.this.finish();
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				if ("暂时连不上服务器".equals(result)) {
					Toast.makeText(RegisterActivity.this, result,
							Toast.LENGTH_LONG).show();
				}
			}

		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if (dialog != null) {
			dialog.dismiss();
		}
		flag = false;
		super.onDestroy();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	};
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		MobclickAgent.onResume(this);

	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		getCodeBtn.setText("获取验证码");
		getCodeBtn.setEnabled(true);
		getCodeBtn.setTextColor(getResources().getColor(R.color.white));
		super.onStart();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.reg_getCode:
			getCode();
			break;
		}
	}

	private void getCode() {
		final String tel = phone.getText().toString();
		if (tel == null || "".equals(tel.trim())) {
			showMsg("请先输入手机号码");
			return;
		}
		boolean bool = Pattern.compile("^1[3,4,5,6,8]{1}[0-9]{9}$")
				.matcher(tel).matches();
		if (!bool) {
			showMsg("请输入正确的手机号码");
			return;
		}
		getCodeBtn.setEnabled(false);
		getCodeBtn.setText("正在获取...");
		new Thread() {
			public void run() {
				try {
					String result = HttpConnectUtil.baseHttpGet(appContext,
							Constant.DOMAIN_URL
									+ "user/requestSmsCode?userPhone=" + tel,30000);
					if ("false".equals(result)) {
						handler.sendEmptyMessage(GET_CODE_FAILED);
						return;
					}
					if ("true".equals(result)) {
						handler.sendEmptyMessage(GET_CODE_SUCCESS);
						return;
					}
				} catch (Exception e) {
					e.printStackTrace();
					handler.sendEmptyMessage(GET_CODE_EXCEPTION);
				}
			};
		}.start();
	}
}
