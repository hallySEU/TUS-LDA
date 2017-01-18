package monash.edu.hally.main;

import monash.edu.hally.index.Documents;
import monash.edu.hally.model.JSTModel;
import monash.edu.hally.model.TUSLDAModel;
import monash.edu.hally.nlp.FilesUtil;

public class Main {
	
	public void start()
	{
		long startTime=System.currentTimeMillis();
		//索引化所有语料库中所有文档
		Documents docs=new Documents();
		docs.indexAllDocuments();
		
		
//		JSTModel jstModel=new JSTModel(docs);
//		jstModel.initialiseModel();
//		//推断和保存模型的潜在变量
//		jstModel.inferenceModel();
		
		//训练模型
		TUSLDAModel tsldaModel=new TUSLDAModel(docs);
//		tsldaModel.initialiseModel2(false);
		tsldaModel.initialiseModel();
		//推断和保存模型的潜在变量
		tsldaModel.inferenceModel();
		
		long endTime=System.currentTimeMillis();
		System.out.println("Runtime "+(endTime-startTime)/1000+"s.");
		
		FilesUtil.printSuccessMessage();
	}

	public static void main(String[] args) {
		
		new Main().start();
	}

}
