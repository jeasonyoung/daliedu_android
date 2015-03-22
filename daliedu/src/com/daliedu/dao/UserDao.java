package com.daliedu.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.daliedu.db.MyDBHelper;
import com.daliedu.entity.User;

public class UserDao {
	private static final String TAG = "UserDao";
	private MyDBHelper dbhelper;
	public UserDao(MyDBHelper helper)
	{
		this.dbhelper = helper;
	}
	public long addUser(User user) throws IllegalArgumentException, IllegalAccessException
	{
		long i = 0;
		SQLiteDatabase db = dbhelper.getDatabase(MyDBHelper.WRITE);
		Log.d(TAG, "addUser�����������ݿ�����");
		db.beginTransaction();
		try{
			i = db.insert("UserTab", null,ContentValuesBuilder.getInstance().bulid(user));
			db.setTransactionSuccessful();
		}finally
		{
			db.endTransaction();
		}
		dbhelper.closeDb();
		Log.d(TAG, "addUser�����ر������ݿ�����");
		return i;
	}
	public User findByUsername(String username)
	{
		User user = null;
		SQLiteDatabase db = dbhelper.getDatabase(MyDBHelper.READ);
		Log.d(TAG, "findByUsername�����������ݿ�����");
		Cursor cursor= db.rawQuery("select uid,username,password from UserTab where username = ?", new String[]{username});
		if(cursor.moveToNext())
		{
			user = new User();
			user.setUid(cursor.getString(0));
			user.setUsername(cursor.getString(1));
			user.setPassword(cursor.getString(2));
		}
		cursor.close();
		dbhelper.closeDb();
		Log.d(TAG, "findByUsername�����ر������ݿ�����");
		return user;
	}
	public void update(User user) throws IllegalArgumentException, IllegalAccessException
	{
		SQLiteDatabase db = dbhelper.getDatabase(MyDBHelper.WRITE);
		Log.d(TAG, "update�����������ݿ�����");
		db.update("UserTab", ContentValuesBuilder.getInstance().bulid(user), "username=?", new String[]{user.getUsername()});
		dbhelper.closeDb();
		Log.d(TAG, "update�����ر������ݿ�����");
	}
	public void saveOrUpdate(User user) throws IllegalArgumentException, IllegalAccessException
	{
		User user1 = findByUsername(user.getUsername());
		if(user1!=null)
		{
			if(!user1.getPassword().equals(user.getPassword()))
			{
				update(user);
			}
		}else
		{
			addUser(user);
		}
	}
	public void closeDB()
	{
		dbhelper.closeDb();
	}
}