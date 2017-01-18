package monash.edu.hally.model;

import java.util.Map.Entry;

import monash.edu.hally.global.ModelParameters;
import monash.edu.hally.global.ModelVariables;
import monash.edu.hally.index.Document;
import monash.edu.hally.index.Documents;
import monash.edu.hally.nlp.FilesUtil;

public class TUSLDAModel{

	private double alpha,gama,lambda,eta,tao;	//超参(dirichlet分布函数的参数)
	private double [][][]beta;	//***先验信息***每一项为：情感s,主题k和词term对应的dirichlet超参   维度 S*K*V
	private double [][]betaSum;	//***先验信息***每一项为：情感s,主题k和词term对应的dirichlet超参和   维度 S*K
	private int []termSenti;	//***先验信息***每一项为：词term的情感标签   维度 V
	private int M,K,V,U,T,S;	//分别表示文档集中文档的个数，主题个数，字典中的词汇个数
	
//	private double[] phiB;	//背景词分布  维度V
	private double[][][] phi;	//{情感-主题}对下的词汇分布  维度 S*K*V
	private double[][][] theta;	//{时间-情感}对下的主题分布  维度 T*S*K
	private double[][][] delta;	//{用户-情感}对下的主题分布  维度 U*S*K
	private double[][] pi; //特定文档下的情感分布  维度M*S

	private int[] ny;	//每一项为：选择特定主题类型生成的次数   维度 2
//	private int[] nx;	//每一项为：选择特定词汇类型生成的次数   维度 2
//	private int[] nb;	//每一项为：特定背景词出现的次数   维度 V
//	private int nbSum;	//背景词出现的次数和
	private int[][] nms;	//每一项为：特定文档下的特定情感出现的次数   维度 M*S
	private int[] nmsSum;	//每一项为：特定文档下的所有情感出现的次数和
	private int[][][] nskv;	//每一项为：{情感-主题}对下的特定词汇出现的次数   维度 S*K*V
	private int[][] nskvSum;	//每一项为：{情感-主题}对下的所有词汇出现的次数和
	private int[][][] ntsk;	//每一项为：{时间-情感}对下的特定主题出现的次数   维度 T*S*K
	private int[][] ntskSum;	//每一项为：{时间-情感}对下的所有主题出现的次数和
	private int[][][] nusk;	//每一项为：{用户-情感}对下的特定主题出现的次数   维度 U*S*K
	private int[][] nuskSum;	//每一项为：{用户-情感}对下的所有主题出现的次数和
	
	
	private int[] z;		//每一项为：特定文档下的主题
	private int[] y;		//每一项为：特定文档下的主题
	private int[][] s;		//每一项为：特定文档特定词下的情感标签  维度 M*Nm
	private int[] s_d;		//每一项为：特定文档的情感标签  维度 M
//	private int[][] x;		//每一项为：特定文档下的选择词汇的变量
	
	private int iterations;	//迭代次数
	private int burn_in;	//burn-in 时期
	private int saveStep;	//burn-in 过后，每saveStep次迭代保存一次结果
	private int topNum;	//显示主题下概率最高的前topNum词项
	
	private Documents documents;	//文档集
	
//	private int saveTime=0;	//保存次数计数变量
	
	
	
	public TUSLDAModel(Documents documents)
	{
		this.documents=documents;
		setModelParameters();
	}
	
	/**
	 * 作用：设置需要预先指定的参数
	 */
	private void setModelParameters()
	{
		System.out.println("Read model parameters.");//另一种读取参数的方式
		ModelParameters modelParameters=FilesUtil.readParametersFile();
	
		K=modelParameters.getK();
		alpha=0.1;	//一般为 50/K
		tao=0.1;	//一般为 0.1
		gama=0.1;
		lambda=eta=0.1;
		iterations=modelParameters.getIterations();
		burn_in=modelParameters.getBurn_in();
		saveStep=modelParameters.getSaveStep();
		topNum=modelParameters.getTopNum();
	}
	
	
	/**
	 * 作用：初始化模型变量参数
	 */
	private void allocateMemoryForVariables()
	{
		M=documents.docs.size();
		V=ModelVariables.g_termDictionary.size();
		U=ModelVariables.g_usersSet.size();
		T=ModelVariables.g_timesSet.size();
		S=3; //情感类型
		
		phi=new double[S][K][V];
		theta=new double[T][S][K];
		delta=new double[U][S][K];
		pi=new double[M][S];
//		phiB=new double[V];
		
		ny=new int[2];
//		nx=new int[2];
//		nb=new int[V];
		nms=new int[M][S];
		nmsSum=new int[M];
		nskv=new int[S][K][V];
		nskvSum=new int[S][K];
		ntsk=new int[T][S][K];
		ntskSum=new int[T][S];
		nusk=new int[U][S][K];
		nuskSum=new int[U][S];
		
		z=new int[M];
		y=new int[M];
		s_d=new int[M];
		s=new int[M][];
//		x=new int[M][];
	}
	
	/**
	 * 作用：根据字典获取先验信息
	 */
	private void setPrior()
	{
		IntegratePrior integratePrior = new IntegratePrior(S, K, V);
		integratePrior.setSentiPrior();
//		integratePrior.print();
		beta=integratePrior.getBeta();
		betaSum=integratePrior.getBetaSum();
		termSenti=integratePrior.getTermSenti();
	}
	
	
	/**
	 * 作用：初始化模型
	 * 1.初始化模型参数（根据需要学习的文档集得到）
	 * 2.给文档中的词汇随机分配主题
	 */
	public void initialiseModel()
	{
		allocateMemoryForVariables();
		setPrior();
		System.out.println("Model begins learning.");
		
		for (int m = 0; m < M; m++) {
			
			int ym=(int) (Math.random()*(2));	//随机
			y[m]=ym;
			int zm=(int) (Math.random()*(K));	//随机分配主题
			z[m]=zm;
			int sm=(int) (Math.random()*(S));	//随机分配情感
			s_d[m]=sm;	
			nms[m][sm]++;
			nmsSum[m]++;
						
			Document document=documents.docs.get(m);				
			int Nm=document.docWords.length;	//第m篇文档的词数（长度）
			s[m]=new int[Nm];
			if(ym==0){
				ny[0]++;
				nusk[document.userIndex][sm][zm]++;
				nuskSum[document.userIndex][sm]++;
			}
			else{
				ny[1]++;
				ntsk[document.timeIndex][sm][zm]++;
				ntskSum[document.timeIndex][sm]++;
			}
			
			for (int n = 0; n < Nm; n++) {	
				
				int smn;
				if(termSenti[document.docWords[n]]!=-1)
					smn = termSenti[document.docWords[n]];
				else
					smn=(int) (Math.random()*(S));
				s[m][n]=smn;
						
				nms[m][smn]++;	
				nmsSum[m]++;
				nskv[smn][zm][document.docWords[n]]++;
				nskvSum[smn][zm]++;
				
			}		
		}
	}
	
	public void initialiseModel2(boolean isUser)
	{
		allocateMemoryForVariables();
		setPrior();
		System.out.println("Model begins learning.");
		
		for (int m = 0; m < M; m++) {
			
			int zm=(int) (Math.random()*(K));	//随机分配主题
			z[m]=zm;
			int sm=(int) (Math.random()*(S));	//随机分配情感
			s_d[m]=sm;	
			nms[m][sm]++;
			nmsSum[m]++;
						
			Document document=documents.docs.get(m);				
			int Nm=document.docWords.length;	//第m篇文档的词数（长度）
			s[m]=new int[Nm];
			if(isUser){
				nusk[document.userIndex][sm][zm]++;
				nuskSum[document.userIndex][sm]++;
			}
			else{
				ntsk[document.timeIndex][sm][zm]++;
				ntskSum[document.timeIndex][sm]++;
			}
			
			for (int n = 0; n < Nm; n++) {	
				
				int smn;
				if(termSenti[document.docWords[n]]!=-1)
					smn = termSenti[document.docWords[n]];
				else
					smn=(int) (Math.random()*(S));
				s[m][n]=smn;
						
				nms[m][smn]++;	
				nmsSum[m]++;
				nskv[smn][zm][document.docWords[n]]++;
				nskvSum[smn][zm]++;
				
			}		
		}
	}
	
	/**
	 * 作用：采用Gibbs采样算法，来推断模型参数
	 */
	public void inferenceModel()
	{
		
		for (int currentIteration = 1; currentIteration <= iterations; currentIteration++) {
			System.out.println("Iteration "+currentIteration);
			if(currentIteration == iterations)
				saveLatentVariables();
			else if((currentIteration >= burn_in) && (currentIteration % saveStep==0))
				calLatentVariables(false);
			else
			{	//不停的采样，直到过了burn-in时期
				for (int m = 0; m < M; m++) {
					Document document=documents.docs.get(m);
//					sampleForDoc2(m, false);
					sampleForDoc(m);
					for (int n = 0; n < document.docWords.length; n++) {
						sampleForWord(m,n);
					}
				}
			}
		}
		System.out.println("Learn over!");
	}
	
	/**
	 * Gibbs采样算法，给当前词汇重新分配新的主题
	 * @return 新的主题
	 */
	
	private double calCondiTionalPro(Document document, int s, int k)
	{
		double p1=1;
  		for (Entry<Integer, Integer> entry : document.indexToCountMap.entrySet()) {
  			if(entry.getValue()==1)
  				p1 = p1 *(nskv[s][k][entry.getKey()]+beta[s][k][entry.getKey()]);
  			else{
  				for (int i = 0; i < entry.getValue()-1; i++) {
  					p1 = p1 * (nskv[s][k][entry.getKey()]+i+beta[s][k][entry.getKey()]);
  				}
  			}
			
		}
  		double p2=1;
  		for (int i = 0; i < document.docWords.length-1; i++) {
			p2 = p2 * (betaSum[s][k]+nskvSum[s][k]+i);
		}
  		
  		return p1/p2;
	}
	
	private void sampleForDoc2(int m, boolean isUser)
	{
		Document document=documents.docs.get(m);
		int oldTopic=z[m];
		int Nm=document.docWords.length;	//第m篇文档的词数（长度）		
		for (int n = 0; n < Nm; n++) {
			nskv[s[m][n]][oldTopic][document.docWords[n]]--;
			nskvSum[s[m][n]][oldTopic]--;
		}

		int oldSentiment=s_d[m];
		nms[m][oldSentiment]--;
		nmsSum[m]--;
		
		if(isUser){
			nusk[document.userIndex][oldSentiment][oldTopic]--;
			nuskSum[document.userIndex][oldSentiment]--;
		}
		else{
			ntsk[document.timeIndex][oldSentiment][oldTopic]--;
			ntskSum[document.timeIndex][oldSentiment]--;
		}
				
		double[][] p=new double[S][K];
				
		for (int s = 0; s < S; s++) {
	  	    for (int k = 0; k < K; k++) {
	  	    	double pro=calCondiTionalPro(document,s,k); 
	  	    	if(isUser){
	  	    		p[s][k] = (nms[m][s]+lambda)/(nmsSum[m]+S*lambda)*
	 						(nusk[document.userIndex][s][k]+alpha)/
	 						(nuskSum[document.userIndex][s]+K*alpha)*pro;
	  	    	}
	  	    	else{
	  	    		p[s][k] = (nms[m][s]+lambda)/(nmsSum[m]+S*lambda)*
	 						(ntsk[document.timeIndex][s][k]+alpha)/
	 						(ntskSum[document.timeIndex][s]+K*alpha)*pro;
	  	    	}  	    	
			}
		}
	
		// accumulate multinomial parameters
		for (int s = 0; s < S; s++) {
			for (int k = 0; k < K; k++) {
				if (k==0) {
				    if (s==0) continue;
			        else p[s][k] += p[s-1][K-1];
				}
				else p[s][k] += p[s][k-1];
		    }
		}
		// probability normalization
		double u = Math.random()* p[S-1][K-1];
		int newSentiment = 0,newTopic = 0;
		
		boolean loopBreak=false;
		for (int s = 0; s < S; s++) {
			for (int k = 0; k < K; k++) {
			    if (p[s][k] > u) {
			    	newSentiment=s;
			    	newTopic=k;
			    	loopBreak=true;
			    	break;
			    }
			}
			if (loopBreak == true) {
				break;
			}
		}
		
		
		for (int n = 0; n < Nm; n++) {
			nskv[s[m][n]][newTopic][document.docWords[n]]++;
			nskvSum[s[m][n]][newTopic]++;
		}

		s_d[m]=newSentiment;
		z[m]=newTopic;

		nms[m][newSentiment]++;
		nmsSum[m]++;
		if(isUser){
			nusk[document.userIndex][newSentiment][newTopic]++;
			nuskSum[document.userIndex][newSentiment]++;

		}
		else{
			ntsk[document.timeIndex][newSentiment][newTopic]++;
			ntskSum[document.timeIndex][newSentiment]++;

		}
	}
	
	/**
	 * 作用：给当前文档m采样
	 * @param m: 文档索引
	 */
	private void sampleForDoc(int m)
	{
		Document document=documents.docs.get(m);
		int oldTopic=z[m];
		int Nm=document.docWords.length;	//第m篇文档的词数（长度）		
		for (int n = 0; n < Nm; n++) {
			nskv[s[m][n]][oldTopic][document.docWords[n]]--;
			nskvSum[s[m][n]][oldTopic]--;
		}

		int oldSentiment=s_d[m];
		nms[m][oldSentiment]--;
		nmsSum[m]--;
		if(y[m]==0){
			nusk[document.userIndex][oldSentiment][oldTopic]--;
			nuskSum[document.userIndex][oldSentiment]--;
			ny[0]--;
		}
		else{
			ntsk[document.timeIndex][oldSentiment][oldTopic]--;
			ntskSum[document.timeIndex][oldSentiment]--;
			ny[1]--;
		}
		
		double[][][] p=new double[2][S][K];
		
		for (int y = 0; y < 2; y++) {
			for (int s = 0; s < S; s++) {
		  	    for (int k = 0; k < K; k++) {
		  	    	double pro=calCondiTionalPro(document,s,k);  
		  	    	if(y==0){
		  	    		p[y][s][k] = (ny[y]+gama)/(ny[0]+ny[1]+2*gama)*
		 						(nms[m][s]+lambda)/(nmsSum[m]+S*lambda)*
		 						(nusk[document.userIndex][s][k]+alpha)/
		 						(nuskSum[document.userIndex][s]+K*alpha)*pro;
		  	    	}
		  	    	else{
		  	    		p[y][s][k] = (ny[y]+gama)/(ny[0]+ny[1]+2*gama)*
		 						(nms[m][s]+lambda)/(nmsSum[m]+S*lambda)*
		 						(ntsk[document.timeIndex][s][k]+alpha)/
		 						(ntskSum[document.timeIndex][s]+K*alpha)*pro;
		  	    	}
				}
			}
		}
		
		// accumulate multinomial parameters
		for (int y = 0; y < 2; y++) {
			for (int s = 0; s < S; s++) {
				for (int k = 0; k < K; k++) {
					if(y==0){
						if (k==0) {
						    if (s==0) continue;
					        else p[y][s][k] += p[y][s-1][K-1];
						}
						else p[y][s][k] += p[y][s][k-1];
					}
					else {
						if (k==0) {
						    if (s==0) 	p[y][0][0] += p[y-1][S-1][K-1];
					        else p[y][s][k] += p[y][s-1][K-1];
						}
						else p[y][s][k] += p[y][s][k-1];
					}
			    }
			}
		}
		// probability normalization
		double u = Math.random()* p[1][S-1][K-1];
		int newSentiment = 0,newTopic = 0,newY=0;
		boolean loopBreak=false;
		for (int y = 0; y < 2; y++) {
			for (int s = 0; s < S; s++) {
				for (int k = 0; k < K; k++) {
				    if (p[y][s][k] > u) {
				    	newY=y;
				    	newSentiment=s;
				    	newTopic=k;
				    	loopBreak=true;
				    	break;
				    }
				}
				if(loopBreak==true)
					break;
			}
			if(loopBreak==true)
				break;
		}
		
		for (int n = 0; n < Nm; n++) {
			nskv[s[m][n]][newTopic][document.docWords[n]]++;
			nskvSum[s[m][n]][newTopic]++;
		}

		s_d[m]=newSentiment;
		z[m]=newTopic;
		y[m]=newY;
		nms[m][newSentiment]++;
		nmsSum[m]++;
		if(y[m]==0){
			nusk[document.userIndex][newSentiment][newTopic]++;
			nuskSum[document.userIndex][newSentiment]++;
			ny[0]++;
		}
		else{
			ntsk[document.timeIndex][newSentiment][newTopic]++;
			ntskSum[document.timeIndex][newSentiment]++;
			ny[1]++;
		}
		
	}
	
	/**
	 * 作用：给当前词采样
	 */
	private void sampleForWord(int m, int n)
	{

		int termIndex=documents.docs.get(m).docWords[n];
		
		int oldSentiment=s[m][n];
		nms[m][oldSentiment]--;
		nskv[oldSentiment][z[m]][termIndex]--;
		nskvSum[oldSentiment][z[m]]--;
		nmsSum[m]--;
				
		
		double[] p=new double[S];
		
//		for (int s = 0; s < S; s++) {		
//			double prior=1;
//			if(termSenti[termIndex]!=-1)
//				prior=(termSenti[termIndex]==s)?0.8:0.2;
//			
//			p[s]=((nms[m][s]+lambda)/(nmsSum[m]+S*lambda))*
//					((nskv[s][z[m]][termIndex]+beta[s][z[m]][termIndex])/
//					(nskvSum[s][z[m]]+betaSum[s][z[m]]))*prior;
//			
//		}
		
		for (int s = 0; s < S; s++) {					
			p[s]=((nms[m][s]+lambda)/(nmsSum[m]+S*lambda))*
					((nskv[s][z[m]][termIndex]+beta[s][z[m]][termIndex])/
					(nskvSum[s][z[m]]+betaSum[s][z[m]]));
		}
		
		//模拟分配新的主题
		//现在各给主题的概率已经生成，现在用轮盘概率的方式,判断随机数落入的概率区间，例如随机数落入(0,p[1]),那么K=1.
		for (int s= 1; s < S; s++) {
			p[s]+=p[s-1];
		}
		double u= Math.random()*p[S-1];
		int newSenti = 0;
		for (int s = 0; s < S; s++) {
			if(p[s]>u){
				newSenti=s;
				break;
			}
		}
		
		s[m][n]=newSenti;
		nskv[newSenti][z[m]][termIndex]++;
		nskvSum[newSenti][z[m]]++;
		nms[m][newSenti]++;
		nmsSum[m]++;
	}
	
	/**
	 * 作用：根据计数变量来更新模型变量
	 * @param isFinalIteration 是否是最后一次迭代，如果是就要把前面几次保存的结果求平均
	 */
	private void calLatentVariables(boolean isFinalIteration)
	{
		// 计算文档情感分布
		for (int m = 0; m < M; m++) {
			for (int s = 0; s < S; s++) {
				pi[m][s] += (nms[m][s]+lambda)/(nmsSum[m]+S*lambda);
				if(isFinalIteration)
					pi[m][s] = pi[m][s] / ((iterations-burn_in) / saveStep + 1); //saveTime;
			}
		}
		
		// 计算{情感-主题}对下的词汇分布
		for (int s = 0; s < S; s++) {
			for (int k = 0; k < K; k++) {
				for (int v = 0; v < V; v++) {
					phi[s][k][v] += (nskv[s][k][v]+beta[s][k][v])/(nskvSum[s][k]+betaSum[s][k]);
					if(isFinalIteration)
						phi[s][k][v] = phi[s][k][v] / ((iterations-burn_in) / saveStep + 1);//saveTime;
				}
			}
		}
		
		// 计算{用户-情感}对下的主题分布
		for (int u = 0; u < U; u++) {
			for (int s = 0; s < S; s++) {
				for (int k = 0; k < K; k++) {
					delta[u][s][k] += (nusk[u][s][k]+alpha)/(nuskSum[u][s]+K*alpha);
					if(isFinalIteration)
						delta[u][s][k] = delta[u][s][k] / ((iterations-burn_in) / saveStep + 1);//saveTime;
				}
			}
		}
		
		// 计算{时间-情感}对下的主题分布
		for (int t = 0; t < T; t++) {
			for (int s = 0; s < S; s++) {
				for (int k = 0; k < K; k++) {
					theta[t][s][k] += (ntsk[t][s][k]+alpha)/(ntskSum[t][s]+K*alpha);
					if(isFinalIteration)
						theta[t][s][k] = theta[t][s][k] / ((iterations-burn_in) / saveStep + 1);//saveTime;
				}
			}
		}
		
	}
	
	/**
	 * 作用：保存当前迭代次数学习到的模型变量
	 * @param currentIterition： 当前迭代次数
	 */
	private void saveLatentVariables()
	{
		System.out.println("Save results at iteration ("+iterations+").");
		calLatentVariables(true);
		FilesUtil.saveSentiDist(pi);
		FilesUtil.saveSentiPrecision(pi, documents);
//		FilesUtil.saveCount(nms);
		FilesUtil.saveDistributions(delta, theta);
//		FilesUtil.saveTopicAssignment(documents, z);
		FilesUtil.saveTopTopicWords(phi, topNum);
	}
	
}
