package monash.edu.hally.nlp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;


import monash.edu.hally.constant.ModelConstants;
import monash.edu.hally.global.ModelParameters;
import monash.edu.hally.global.ModelVariables;
import monash.edu.hally.index.Document;
import monash.edu.hally.index.Documents;



public class FilesUtil {
	
	
	
	/**
	 * 作用：将一篇文档以一行数据为单位存入ArrayList中，目的为了分词。
	 * @param documentName:源文件的绝对路径名
	 */
	public static ArrayList<String> readDocument(String documentName)
	{
		try {
			BufferedReader reader=new BufferedReader(new FileReader(new File(documentName)));
			String line;
			ArrayList<String> documentLines=new ArrayList<String>();
			while((line=reader.readLine())!=null)
			{
				documentLines.add(line.trim());
			}
			return documentLines;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * @param tweetData: a tweet which contains time and user
	 * @return
	 */
	public static ArrayList<String> tokenize(String tweetData)
	{
		
		String items[]=tweetData.split(ModelConstants.LINK_FLAG);		
		ArrayList<String> tokens=new ArrayList<String>();
		tokens.add(items[0]); //add sentiment
		tokens.add(formalizeTweetTime(items[1])); //add time
		tokens.add(items[2]); //add user
		StringTokenizer tokenizer=new StringTokenizer(items[3]);
		while(tokenizer.hasMoreTokens())
		{
			String token=tokenizer.nextToken();	
			tokens.add(token.toLowerCase().trim());
		}	
		return tokens;
	}
	
	/**
	 * @param time: the time of a tweet, such as: Mon Apr 06 22:19:45 PDT 2009
	 * @return the formalized time, such as: 2009-Apr-06 
	 */
	public static String formalizeTweetTime(String time)
	{
		String items[]=time.split(" ");	
		String formalizedTime=items[5]+"-"+items[1]+"-"+items[2];
		return formalizedTime;	
	}
	
	/**
	 * 作用：移除不合规格的词
	 * @param documentLines
	 */
	public static String removeWords(String line)
	{
		String newline="";
		StringTokenizer tokenizer=new StringTokenizer(line);
		
		while(tokenizer.hasMoreTokens())
		{
			String token=tokenModify(tokenizer.nextToken());	//去掉词汇最后部分的标点符号	
			if(!isNoiseWord(token)&&!Stopwords.isContains(token)){//去噪和去停用词
				newline += token.toLowerCase().trim()+"\t";
				
			}
		}	
		return newline;
	}
	
	/**
	 * 作用：去掉词汇最后部分的标点符号	
	 */
	public static String tokenModify(String token)
	{
		String subToken=token.substring(0, token.length()-1);
		return subToken+token.substring(token.length()-1).replaceAll("\\pP|\\pS", "");
	}

	/**
	 * 作用：去噪
	 * @param token
	 */
	public static boolean isNoiseWord(String token) {
		token = token.toLowerCase().trim();
		// filter @xxx and URL
		if(token.matches(".*www\\..*") || token.matches(".*\\.com.*") || 
				token.matches(".*http:.*") )
			return true;
		//filter mention
		if(token.matches("[@＠][a-zA-Z0-9_]+"))
			return true;
		//filter space
		if(token.matches("\\s*"))
			return true;
		//filter digit
		if(token.matches("\\d*"))
			return true;
		//filter signal
		if(token.matches("\\pP*")) 
			return true;
		//filter w
		if(token.matches("[a-z]"))  
			return true;
		return false;
	}
	
	public static void saveSentiPrecision(double[][]pi, Documents documents)
	{
		int TP=0,TN=0,FP=0,FN=0;
		try {
			BufferedWriter writer=new BufferedWriter(new FileWriter(new File(
					ModelConstants.RESULTS_PATH+ModelConstants.MODEL_NAME+".precision")));
			for (int m = 0; m < pi.length; m++) {
				//4 表示真实的正例  1表示真实的负例
				String sentiDoc=documents.docs.get(m).getSentiment();
				if(sentiDoc.equals("0") && pi[m][1] > pi[m][2]) FP++;
				if(sentiDoc.equals("4") && pi[m][1] < pi[m][2]) FN++;
				if(sentiDoc.equals("4") && (pi[m][1] > pi[m][2])) TP++;	// p(pos) > p(neg)
				if(sentiDoc.equals("0") && (pi[m][1] < pi[m][2])) TN++;	
			}			
			double precision= (double) TP / (TP+FP);
			double recall= (double) TP / (TP+FN);
			double accuracy = (double) (TP+TN) / (TP+TN+FP+FN);
			double F1 = (double) (2*precision*recall) / (precision+recall);
			writer.write("PositiveNum + NegativeNum = "+(TP+TN+FP+FN)+"\t"+"PositiveNum = "+(TP+FN)+
					"\t"+"NegativeNum = "+(TN+FP)+ModelConstants.CR_LF);
			writer.write("TP="+TP+"\t"+"FN="+FN+"\t"+"TN="+TN+"\t"+"FP="+FP+"\t"+ModelConstants.CR_LF);
			writer.write("Accuracy: "+accuracy+ModelConstants.CR_LF);
			writer.write("Precision: "+precision+ModelConstants.CR_LF);
			writer.write("Recall: "+recall+ModelConstants.CR_LF);
			writer.write("F1: "+F1+ModelConstants.CR_LF);
			writer.close();		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void saveSentiDist(double[][]pi)
	{
		try {
			BufferedWriter writer=new BufferedWriter(new FileWriter(new File(
					ModelConstants.RESULTS_PATH+ModelConstants.MODEL_NAME+".dsdist")));
			for (int m = 0; m < pi.length; m++) {
				for (int s = 0; s < pi[0].length; s++) {
					writer.write(pi[m][s]+"\t");
				}
				writer.write(ModelConstants.CR_LF);
			}
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	public static void saveCount(int nms[][])
	{
		try {
			BufferedWriter writer=new BufferedWriter(new FileWriter(new File(
					ModelConstants.RESULTS_PATH+ModelConstants.MODEL_NAME+".senti_count")));
		
			for (int i = 0; i < nms.length; i++) {
				for (int j = 0; j < nms[0].length; j++) {
					writer.write(nms[i][j]+"\t");
				}
				writer.write(ModelConstants.CR_LF);
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 作用：保存主题分布
	 * @param delta: {用户-情感}对下的主题分布
	 * @param theta: {时间-情感}对下的主题分布
	 */
	public static void saveDistributions(double[][][]delta,double[][][]theta)
	{
		try {
			BufferedWriter writer=new BufferedWriter(new FileWriter(new File(
					ModelConstants.RESULTS_PATH+ModelConstants.MODEL_NAME+".userskdist")));
			String sentiLable[] = {"neutral","positive","negative"};
			for (int u = 0; u < delta.length; u++) {
				//如果用户发的帖子小于5，就不输出该用户的信息
				if(ModelVariables.g_userToCountMap.get(ModelVariables.g_usersSet.get(u)) < 5) continue; 
				for (int s = 0; s < delta[0].length; s++) {
					writer.write("["+ModelVariables.g_usersSet.get(u)+","+sentiLable[s]+"] : ");
					for (int k = 0; k < delta[0][0].length; k++) {
						writer.write(delta[u][s][k]+"\t");
					}
					writer.write(ModelConstants.CR_LF);
				}
			}		
			writer.close();
			
			writer=new BufferedWriter(new FileWriter(new File(
					ModelConstants.RESULTS_PATH+ModelConstants.MODEL_NAME+".timeskdist")));
			for (int t = 0; t < theta.length; t++) {
				for (int s = 0; s < theta[0].length; s++) {
					writer.write("["+ModelVariables.g_timesSet.get(t)+","+sentiLable[s]+"] : ");
					for (int k = 0; k < theta[0][0].length; k++) {
						writer.write(theta[t][s][k]+"\t");
					}
					writer.write(ModelConstants.CR_LF);
				}
			}				
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * @param phiB: 背景词分布
	 * @param topNum: 排名前topNum个词
	 */
	public static void saveTopBGWords(double[]phiB,int topNum)
	{
		try {
			BufferedWriter writer=new BufferedWriter(new FileWriter(new File(
					ModelConstants.RESULTS_PATH+ModelConstants.MODEL_NAME+".bg_topwords")));
		
			ArrayList<Integer> arrayList=new ArrayList<Integer>();
			for (int v = 0; v < phiB.length; v++) {
				arrayList.add(new Integer(v));				
			}
			Collections.sort(arrayList,new TopWordComparable(phiB));
			for (int i = 0; i < topNum; i++) {
				writer.write(ModelVariables.g_termDictionary.get(arrayList.get(i))+"\t");
				writer.write(phiB[arrayList.get(i)]+"\t");
			}
			writer.write(ModelConstants.CR_LF);	
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 作用：保存所有主题下，概率最高的TOP_NUM（默认为10）个词项
	 * @param phi {情感，主题}下的词汇分布
	 */
	public static void saveTopTopicWords(double[][][]phi,int topNum)
	{
		try {
			BufferedWriter writer=new BufferedWriter(new FileWriter(new File(
					ModelConstants.RESULTS_PATH+ModelConstants.MODEL_NAME+".sk_topwords")));
			String sentiLable[] = {"neutral","positive","negative"};
			for (int s = 0; s < phi.length; s++) {
				for (int k = 0; k < phi[0].length; k++) {
					ArrayList<Integer> arrayList=new ArrayList<Integer>();
					for (int v = 0; v < phi[0][0].length; v++) {
						arrayList.add(new Integer(v));				
					}
					Collections.sort(arrayList,new TopWordComparable(phi[s][k]));
					
					writer.write(sentiLable[s]+" topic_"+k+" : ");
					for (int i = 0; i < topNum; i++) {
						writer.write(ModelVariables.g_termDictionary.get(arrayList.get(i))+"\t");
//						writer.write(phi[s][k][arrayList.get(i)]+"\t");
					}
					writer.write(ModelConstants.CR_LF);
				}
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void saveSentiment(ArrayList<Document> docs)
	{
		try {
			BufferedWriter writer=new BufferedWriter(new FileWriter(new File(
					ModelConstants.RESULTS_PATH+ModelConstants.MODEL_NAME+".senti")));
		
			for (Document doc : docs) {
				writer.write(doc.getSentiment());
				writer.write(ModelConstants.CR_LF);
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void saveDictionary()
	{
		try {
			BufferedWriter writer=new BufferedWriter(new FileWriter(new File(
					ModelConstants.RESULTS_PATH+ModelConstants.MODEL_NAME+".dictionary")));
		
			for (String term : ModelVariables.g_termDictionary) {
				writer.write(term+ModelConstants.CR_LF);
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 作用：保留文档的词汇index
	 */
	public static void saveDocWordIndex(ArrayList<Document> docs)
	{
		try {
			BufferedWriter writer=new BufferedWriter(new FileWriter(new File(
					ModelConstants.RESULTS_PATH+ModelConstants.MODEL_NAME+".doc_wordindex")));
				
			for (Document doc : docs) {
				for (int index : doc.docWords) {
					writer.write(index+" ");
				}
				writer.write(ModelConstants.CR_LF);
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 作用：打印成功的信息
	 */
	public static void printSuccessMessage()
	{
		String resultPath=System.getProperty("user.dir")+"\\data\\results";
//		JOptionPane.showMessageDialog(null, "Results are reserved in "+resultPath);
		System.out.println("Results are reserved in "+resultPath);
		try {	
			int choice=JOptionPane.showConfirmDialog(null, "Results are reserved in "+resultPath+
					"\nDo you want to open the dir of results ?", "Make a choice", JOptionPane.YES_NO_OPTION);
			if(choice==JOptionPane.OK_OPTION)
				java.awt.Desktop.getDesktop().open(new File(resultPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 作用：创建默认的参数文件 （从文件中读参数，用于非图形化界面）
	 */
	private static void createParametersFile()
	{
		File file=new File(ModelConstants.PARAMETERS_PATH+ModelConstants.PARAMETERS_FILE);
		try {
			file.createNewFile();
			PrintWriter writer=new PrintWriter(file);
			
			writer.print("K (Number of topics):"+ModelConstants.SPLIT_FLAG+"5"+ModelConstants.CR_LF);
			writer.print("Top number:"+ModelConstants.SPLIT_FLAG+"10"+ModelConstants.CR_LF);
			writer.print("Iterations:"+ModelConstants.SPLIT_FLAG+"100"+ModelConstants.CR_LF);
			writer.print("Burn_in:"+ModelConstants.SPLIT_FLAG+"80"+ModelConstants.CR_LF);
			writer.print("SaveStep:"+ModelConstants.SPLIT_FLAG+"10"+ModelConstants.CR_LF);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	/**
	 * 作用：从参数文件中读取模型参数（从文件中读参数，用于非图形化界面）
	 */
	public static ModelParameters readParametersFile()
	{
		File file=new File(ModelConstants.PARAMETERS_PATH+ModelConstants.PARAMETERS_FILE);
		ModelParameters modelParameters=new ModelParameters();
		if(!file.exists())
			createParametersFile();
		
		ArrayList<String> lines = readDocument(file.getAbsolutePath());
		for (String line : lines) {
			String[] para=line.split(ModelConstants.SPLIT_FLAG);
			switch (para[0]) {
			case "Iterations:":
				modelParameters.setIterations(Integer.valueOf(para[1]));
				break;
			case "Burn_in:":
				modelParameters.setBurn_in(Integer.valueOf(para[1]));
				break;
			case "SaveStep:":
				modelParameters.setSaveStep(Integer.valueOf(para[1]));
				break;
			case "Top number:":
				modelParameters.setTopNum(Integer.valueOf(para[1]));
				break;
			default:
				modelParameters.setK(Integer.valueOf(para[1]));
				break;
			}
		}
		return modelParameters;
	}
	
}
