import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import monash.edu.hally.nlp.FilesUtil;

public class Preprocessing1 {
	
	static ArrayList<String> users=new ArrayList<>();
	static ArrayList<String> contentsModification=new ArrayList<>();
	
	static Map<String, Integer> wordCountMap=new HashMap<>();
	
	
	public static ArrayList<String> removeNoiseWord(String fileName)throws IOException
	{
		ArrayList<String> contents=Preprocessing.readDocument(fileName);
		ArrayList<String> newContents= new ArrayList<>();
		int size=0;
		for (String content : contents) {
			String items[] = content.split("#\\*\\*\\*#");
			String newLine="";
			int length=0;
			System.out.println(++size);
			if(items[1].split("\t").length<3) continue;
			for (String item : items[1].split("\t")) {
				if(monash.edu.hally.nlp.Preprocessing.isReservedTerms(item)){
					length++;
					
					newLine += item+"\t";
				}
			}
			if(length<3) continue;
			newLine = items[0]+"&&&"+newLine+"&&&"+items[2];
			System.out.println(newLine);
			newContents.add(newLine);
		}
		return newContents;
	}
	
	public static void readUser(String fileName) throws IOException
	{
		BufferedReader reader=new BufferedReader(new FileReader(new File(fileName)));
		String line;
		while((line=reader.readLine())!=null)
		{
			users.add(line.split("\t")[0]);
		}
	}
	
	public static void compare() throws IOException
	{
		int i=0;
		ArrayList<String> contents=Preprocessing.readDocument("data/sentiment140.data");
		for (String content : contents) {
			boolean isSave=false;
			String items[]=content.split("&&&");
			for (String user : users) {
				if(items[2].equals(user)){
					isSave=true;
					break;
				}
			}
			if(isSave){
				contentsModification.add(content);
				System.out.println(++i);
			}
		}	
	}
	
	
	// remove stopword disturb word
	public static void removeWords() throws IOException
	{
		ArrayList<String> contents=Preprocessing.readDocument("data/sentiment140_user10.data");
		for (String content : contents) {
			String items[]=content.split("&&&");
			String newContent=FilesUtil.removeWords(items[3]);
			if(newContent.split("\t").length<=2) continue;
			String newline=items[0]+"&&&"+items[1]+"&&&"+items[2]+"&&&"+newContent;
			contentsModification.add(newline);
			System.out.println(newline);
		}
	}
	
	// remove stopword disturb word
		public static void countHashtag() throws IOException
		{
			int count=0;
			ArrayList<String> contents=Preprocessing.readDocument("data/sentiment140_user10_porter_removed.data");
			for (String content : contents) {
				String items[]=content.split("&&&");
				String newitems[]=items[3].split("\t");
				for (int i = 0; i < newitems.length; i++) {
					if(newitems[i].startsWith("#"))
						++count;
				}
				
				
			}
			System.out.println("hashtag: "+count);
		}
	
	//remove low-frequency word, only save frequency >= 5
	public static void removeLowFrequency() throws IOException
	{
		ArrayList<String> contents=Preprocessing.readDocument("data/sentiment140_user10_removed.data");
		for (String content : contents) {
			String items[]=content.split("&&&");
			String line=items[3];
			String words[]=line.split("\t");
			for (int i = 0; i < words.length; i++) {
				if(wordCountMap.containsKey(words[i]))
				{
					int count=wordCountMap.get(words[i])+1;
					wordCountMap.put(words[i], count);
				}
				else
					wordCountMap.put(words[i], 1);
			}	
		}
		
		for (String content : contents) {
			String items[]=content.split("&&&");
			String line=items[3];
			String words[]=line.split("\t");
			String newContent="";
			int size=0;
			for (int i = 0; i < words.length; i++) {
				if(wordCountMap.get(words[i])>=5){
					++size;
					newContent += words[i]+"\t";
				}
			}
			if(size>=3){
				String newline=items[0]+"&&&"+items[1]+"&&&"+items[2]+"&&&"+newContent;
				contentsModification.add(newline);
				System.out.println(newline);
			}
				
		}
		
	}
	
	public static ArrayList<String> removeLowWordDoc(String docName, int length)
	{
		ArrayList<String> reserveContents=new ArrayList<>();
		ArrayList<String> contents =FilesUtil.readDocument(docName);
		for (String content : contents) {
			String items[]=content.split("&&&");
			String line=items[3];
			String words[]=line.split("\t");
			if(words.length >= length){
				reserveContents.add(content);
			}
		}
		return reserveContents;
	}

	public static void main(String[] args) throws IOException {
		
//		String ss="29743199396306945#***#i/O	love/V	ilda/^	and/&	nevada/^	./,	#***#Tue Jan 25 03:31:45 +0000 2011";
//		System.out.println(ss.split("#\\*\\*\\*#").length);
		
//		readUser("data/userCount");
//		compare();
		
//		removeWords();
//		Preprocessing.writeFile("data/sentiment140_user10_removed.data",contentsModification);
//		removeLowFrequency();
//		Preprocessing.writeFile("data/sentiment140_user10_removedLow.data",contentsModification);
		
//		countHashtag();
		
//		ArrayList<String> contents =removeLowWordDoc("data/originalDocs/sentiment140_user10_removed_porter.data", 8);
		ArrayList<String> contents = removeNoiseWord("data/Pos_english_Tweet_data.txt");
		Preprocessing.writeFile("data/tweet2011.data",contents);
	}

}
