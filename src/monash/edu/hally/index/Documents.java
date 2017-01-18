package monash.edu.hally.index;

import java.io.File;
import java.util.ArrayList;
import java.util.Map.Entry;

import monash.edu.hally.constant.ModelConstants;
import monash.edu.hally.global.ModelVariables;
import monash.edu.hally.nlp.FilesUtil;

public class Documents {
	
	public ArrayList<Document> docs=new ArrayList<Document>();
		
	/**
	 * 作用：对所有文档索引化
	 */
	public void indexAllDocuments()
	{
		if(new File(ModelConstants.DOCUMENTS_PATH).listFiles().length==0){
			System.err.println("Original documents are null, please add documents.");
			System.exit(0);
		}
		System.out.println("Begin to extend dictionary and index documents.");
		
		ArrayList<String> tweetsData=FilesUtil.readDocument(
				ModelConstants.DOCUMENTS_PATH+ModelConstants.DOCUMENT_NAME);
		
		for (int i = 0; i < tweetsData.size(); i++) {
			System.out.println("Indexing document["+(i+1)+"]");
			Document document=new Document(tweetsData.get(i));
			document.indexDocument();
			ModelVariables.g_wordCount += document.docWords.length;
			docs.add(document);
		}
		FilesUtil.saveDictionary();
		FilesUtil.saveDocWordIndex(docs);
		FilesUtil.saveSentiment(docs);
	}

	
	
	public static void main(String[] args) {
		
		Documents docs=new Documents();
		docs.indexAllDocuments();
		System.out.println("UserSize:\t"+ModelVariables.g_usersSet.size()+"\t"+ModelVariables.g_userToIndexMap.size()
				+"\tTimeSize:\t"+ModelVariables.g_timesSet.size()
				+"\tTermSize:\t"+ModelVariables.g_termDictionary.size()
				+"\tWordSize:\t"+ModelVariables.g_wordCount);
//		
		int i=0;
//		for (Document docment : docs.docs) {
//			System.out.print(++i+"=> ");
//			System.out.println(docment.timeIndex+"\t"+docment.userIndex);
////			for (int j = 0; j < docment.docWords.length; j++) {
////				System.out.print(docment.docWords[j]+":"+ModelVariables.g_termDictionary.get(docment.docWords[j])+"\t");
////			}
////			System.out.println();
////			System.out.print(++i+"\t");
////			for (Entry<Integer, Integer> entry : docment.indexToCountMap.entrySet()) {
////				System.out.print(ModelVariables.g_termDictionary.get(entry.getKey())+"\t"+entry.getValue()+"\t");
////			}
////			System.out.println();
//		}
//		
////		for (Entry<String, Integer> entry : ModelVariables.g_userToCountMap.entrySet()) {
////			System.out.println(entry.getKey()+"\t"+entry.getValue()+"\t");
////		}
	}

}
	
