import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.opencsv.CSVReader;


public class Preprocessing {
	
	static Map<String, Integer> userCountMap=new HashMap<>();
	
	static ArrayList<String> contentsList=new ArrayList<>();
	
	public static ArrayList<String> readDocument(String fileName) throws IOException
	{
		BufferedReader reader=new BufferedReader(new FileReader(new File(fileName)));
		String line;
		ArrayList<String> documentLines=new ArrayList<String>();
		while((line=reader.readLine())!=null)
		{
			documentLines.add(line.trim());
		}
		return documentLines;
	}
	
	public static void countUser(ArrayList<String> contents)
	{
		int i=0;
		for (String content : contents) {
			String items[]=content.split("&&&");
//			if(items.length!=4) continue;
//			contentsList.add(content);
			String user=items[2];
			++i;
			if(userCountMap.containsKey(user)){
				int count=userCountMap.get(user)+1;
				userCountMap.put(user, count);
			}
			else
				userCountMap.put(user, 1);
		}
		System.out.println(i);
	}
	
	public static void writer(String fileName) throws IOException
	{
		BufferedWriter writer=new BufferedWriter(new FileWriter(new File(fileName)));
		for (Entry<String, Integer> entry : userCountMap.entrySet()) {
			if(Integer.valueOf(entry.getValue())>=10)
				writer.write(entry.getKey()+"\t"+entry.getValue()+"\n");
		}
		writer.close();
	}
	
	public static ArrayList<String> readCVS(String fileName) throws IOException
	{
		ArrayList<String> arrayList=new ArrayList<>();
        File file = new File(fileName);  
        FileReader fReader = new FileReader(file);  
        CSVReader csvReader = new CSVReader(fReader);  
        List<String[]> list = csvReader.readAll();  
        for(String[] line : list){
        	if(line[0].equals("2")) continue;
        	String text=line[0]+"&&&"+line[2]+"&&&"+line[4]+"&&&"+line[5];
        	arrayList.add(text);
//            System.out.println(text);  
        }  
        csvReader.close();
		return arrayList;  
	} 
	
	public static void writeFile(String fileName, ArrayList<String> contents) throws IOException
	{
		BufferedWriter writer=new BufferedWriter(new FileWriter(new File(fileName)));
		for (String string : contents) {
			writer.write(string+"\n");
		}
		writer.close();
	}

		
	public static void main(String[] args) throws Exception {
		
//		ArrayList<String> contents=readCVS("data/training.1600000.processed.noemoticon.csv");
//		writeFile("data/sentiment140.data", contents);

		ArrayList<String> contents=readDocument("data/sentiment140.data");
		countUser(contents);
		writer("data/userCount");
//		writeFile("data/sentiment140.data(m)", contentsList);
	}

}
