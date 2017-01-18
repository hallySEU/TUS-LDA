package monash.edu.hally.model;

import monash.edu.hally.index.Documents;

public class Test {
	
	public static void main(String[] args) {
		Documents docs=new Documents();
		docs.indexAllDocuments();
		TUSLDAModel tsldaModel=new TUSLDAModel(docs);
		tsldaModel.initialiseModel();
	}

}
