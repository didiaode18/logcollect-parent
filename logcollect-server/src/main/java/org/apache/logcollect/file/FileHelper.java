package org.apache.logcollect.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileHelper {
	public static void createFile(String path,String params) throws IOException{
		File file = new File(path);
	    if(!file.getParentFile().exists()){
	    	System.out.println(file.getParentFile());
	    	file.getParentFile().mkdirs();
	    }
	    file.createNewFile();
	    FileWriter fw = new FileWriter(file,false);
	    BufferedWriter bw = new BufferedWriter(fw);
	    bw.write(params);
	    bw.flush();
	    bw.close();
	    fw.close();
	}
	
	public static String readFile(File fileName) throws IOException,FileNotFoundException{
		String result = "";  
		FileReader fileReader=null;  
		BufferedReader bufferedReader=null;  
		fileReader=new FileReader(fileName);  
		bufferedReader=new BufferedReader(fileReader);  
		try{  
			String read=null;  
			while((read=bufferedReader.readLine())!=null){  
				result += read;  
			}  
		}catch(Exception e){  
			e.printStackTrace();  
		}finally{  
			if(bufferedReader!=null){  
				bufferedReader.close();  
			}  
			if(fileReader!=null){  
				fileReader.close();  
			}  
		}
		return result;  
	}
}
