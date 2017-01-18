import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import monash.edu.hally.constant.ModelConstants;
import monash.edu.hally.nlp.FilesUtil;

public class Preprecessing2 {
	
	
	public static Map<String, ArrayList<String>> data2Strings=new HashMap<>();
	
	public static void countDataString() throws IOException
	{
		ArrayList<String> contents=Preprocessing.readDocument(
				"data/originalDocs/tweet2011_porter_fre3_dlen3_wlen3.data");
		for (String content : contents) {
			String time=FilesUtil.formalizeTweetTime(content.split("&&&")[0]);
			if(!data2Strings.containsKey(time)){
				ArrayList<String> strings=new ArrayList<>();
				strings.add(content);
				data2Strings.put(time, strings);
			}
			else{
				ArrayList<String> strings=data2Strings.get(time);
				strings.add(content);
				data2Strings.put(time, strings);
			}
		}
		
		contents = new ArrayList<>();
		for (Entry<String, ArrayList<String>> entry : data2Strings.entrySet()) {
			if(entry.getValue().size()>100000 && entry.getKey().startsWith("2011-feb"))
				contents.addAll(entry.getValue());
			System.out.println(entry.getKey()+"\t"+entry.getValue().size());
		}
//		2011-feb
		Preprocessing.writeFile("data/tweet2011_porter_fre3_dlen3_wlen3_(2011-feb).data",contents);
	}
	
	public static ArrayList<String> removeWord() throws IOException
	{
		ArrayList<String> contents=Preprocessing.readDocument(
				"data/originalDocs/tweet2011_porter_fre3_len3.data");
		ArrayList<String> newContents= new ArrayList<>();
		//¶Á±íÇé×Öµä
		ArrayList<String> dictionary=FilesUtil.readDocument(
				ModelConstants.SENTIMENT_EMOTICONS);
		ArrayList<String> newDictionary=new ArrayList<>();
		for (String string : dictionary) {
			newDictionary.add(string.split("\t")[0]);
		}
		for (String content : contents) {
			String items[] = content.split("&&&");
			String newLine="";
			int length=0;
			for (String item : items[2].split("\t")) {
				
				if(item.equals("wasn't")||item.equals("wouldn't")||item.equals("ain't")) continue;
				if(item.length()<=2 && !newDictionary.contains(item)) continue;	
				length++;	
				newLine += item+"\t";	
			}
			if(length<3) continue;
			newLine = items[0]+"&&&"+items[1]+"&&&"+newLine;
//			System.out.println(newLine);
			newContents.add(newLine);
		}
		return newContents;
	}

	public static void main(String[] args) throws IOException {
//		ArrayList<String> a=removeWord();
//		Preprocessing.writeFile("data/tweet2011_porter_fre3_dlen3_wlen3.data",a);
		
		countDataString();
		
	}

}
