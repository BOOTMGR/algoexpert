package hp.bootmgr.algoexpert.utils;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

public class FileUtils {
	public static String readClassPathFile(String name) throws IOException {
		InputStream in  = FileUtils.class.getResourceAsStream(name);
		return readStream(in);
	}
	
	public static String readStream(InputStream in) throws IOException {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		while ((length = in.read(buffer)) != -1) {
		    byteOut.write(buffer, 0, length);
		}
		return byteOut.toString("UTF-8");
	}
	
	public static void writeFile(String filePath, String content) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(filePath);
			writer.write(content);
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Can not file");
		} finally {
			if(writer != null)
				writer.close();
		}
	}
}
