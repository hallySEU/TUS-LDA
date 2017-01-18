package monash.edu.hally.nlp;

import java.io.File;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import monash.edu.hally.constant.ModelConstants;

public class Stopwords {
	
	private static ArrayList<String> stopwordList=new ArrayList<String>();

	static
	{
		readStopwords();
	}
	
	public static void add(String stopword)
	{
		stopwordList.add(stopword);
	}
	
	public static boolean isContains(String stopword)
	{
		if(stopwordList.contains(stopword.toLowerCase().trim()))
			return true;
		return false;
	}
	
	/**
	 * 作用：从指定的目录读取停用词
	 */
	public static void readStopwords()
	{
		File fileDir=new File(ModelConstants.STOPWORDS_PATH);
		if(fileDir.listFiles().length==0){
			JOptionPane.showMessageDialog(null, "The lists of stopwords are null, please add.", "Warn", JOptionPane.WARNING_MESSAGE);
			System.err.println("The lists of stopwords are null, please add.");
		}
		for (File file : fileDir.listFiles()) {
			ArrayList<String> stopwordTable=FilesUtil.readDocument(file.getAbsolutePath());
			stopwordList.addAll(stopwordTable);
		}
	}
	
	public static void main(String[] args) {
		
		System.out.println(Stopwords.isContains("a"));;
	}
}
