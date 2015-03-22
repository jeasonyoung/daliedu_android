package com.daliedu.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Environment;
import android.os.StatFs;

public class FileUtil {
	public static boolean checkSDCard(long fileSize)
	{
		File pathFile = Environment.getExternalStorageDirectory();
		if(!pathFile.exists())
		{
			return false;
		}
		if(fileSize>0)
		{
			StatFs statfs = new StatFs(pathFile.getPath());
			//获得可供程序使用的Block数量
			long nAvailaBlock = statfs.getAvailableBlocks();
			//获得SDCard上每个block的SIZE
			long nBlocSize = statfs.getBlockSize();
			//计算SDCard剩余大小 Byte
			long nSDFreeSize = nAvailaBlock * nBlocSize;
			return nSDFreeSize > fileSize;
		}else
		{
			return true;
		}
	}
	public static void encode(File resFile,int offset,String filePath) throws IOException
	 {
	  byte[] buf = new byte[1024*4];
	  try {
	   BufferedInputStream in = new BufferedInputStream(new FileInputStream(resFile));
	   BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filePath));
	   int count = 0;
	   //String code = "abcdefg";
	  // byte[] codeByte = code.getBytes();
	  // System.out.println("code 的长度："+codeByte.length);
	  // out.write(codeByte);
	   in.read(new byte[offset]);
	   while((count = in.read(buf))!=-1)
	   {
	    out.write(buf, 0, count);
	   }
	   out.flush();
	   out.close();
	   in.close();
	  } catch (FileNotFoundException e1) {
	   // TODO Auto-generated catch block
	   e1.printStackTrace();
	  }
	 }
}
