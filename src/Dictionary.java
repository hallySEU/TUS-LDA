import java.io.IOException;
import java.util.ArrayList;

import monash.edu.hally.nlp.FilesUtil;

public class Dictionary {
	
	public static ArrayList<String> lines=new ArrayList<>();
	
	// 0	0.75	assimilatory#1 assimilative#2 assimilating#1
	public static void getSentiWordNet(ArrayList<String> contents)
	{
		for (String line : contents) {
			String newLine="";
			String iterms[]=line.split("\t");
			if(Double.valueOf(iterms[0])>0.6)	//pos
				newLine = "0\t"+Double.valueOf(iterms[0])+"\t0";
			else
				newLine = "0\t0\t"+Double.valueOf(iterms[1]);
			String tokens[]=iterms[2].split(" ");
			for (int i = 0; i < tokens.length; i++) {
				String token=tokens[i].split("#")[0];
				String nl= token+"\t"+newLine;
				lines.add(nl);
			}
		}
	}

	public static void segSentiDictionary(String line)
	{
		try {
		if(line.startsWith("a")||line.startsWith("n")||line.startsWith("r")||line.startsWith("v"))
		{
			String iterms[]=line.split("\t");
			if(Double.valueOf(iterms[2])>0.6 || Double.valueOf(iterms[3])>0.6){
				line=iterms[2]+"\t"+iterms[3]+"\t"+iterms[4];
				lines.add(line);
			}		
		}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static ArrayList<String> removeForJST()
	{
		ArrayList<String> newContents=new ArrayList<>();
		ArrayList<String> contents=FilesUtil.readDocument("data/sentiment140_user10_length_5.data");
		for (String string : contents) {
			String tokens[]=string.split("&&&");
			String newString=tokens[0]+" "+tokens[3];
			newContents.add(newString);
		}
		return newContents;
	}
	
	
	public static void main(String[] args) throws IOException  {
		
		System.out.println(FilesUtil.isNoiseWord("W"));
		
		ArrayList<String> contents=removeForJST();
		Preprocessing.writeFile("data/sentiment140_jst_length_5.data", contents);
		
//		ArrayList<String> contents=FilesUtil.readDocument("data/SentiWordNet_3.0.0");
//		getSentiWordNet(contents);
//		
////		ArrayList<String> contents=FilesUtil.readDocument("data/SentiWordNet_3.0.0_20130122.txt");
////		for (String line : contents) {
////			segSentiDictionary(line);
////		}
//		try {
//			Preprocessing.writeFile("data/dictionary/sentiwordnet.constraint", lines);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

}
