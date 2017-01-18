package monash.edu.hally.model;

import monash.edu.hally.global.ModelParameters;
import monash.edu.hally.global.ModelVariables;
import monash.edu.hally.index.Document;
import monash.edu.hally.index.Documents;
import monash.edu.hally.nlp.FilesUtil;

public class JSTModel{

	private double alpha,lambda;	//超参(dirichlet分布函数的参数)
	private double [][][]beta;	//***先验信息***每一项为：情感s,主题k和词term对应的dirichlet超参   维度 S*K*V
	private double [][]betaSum;	//***先验信息***每一项为：情感s,主题k和词term对应的dirichlet超参和   维度 S*K
	private int []termSenti;	//***先验信息***每一项为：词term的情感标签   维度 V
	private int M,K,V,S;	//分别表示文档集中文档的个数，主题个数，字典中的词汇个数
	private double []gama;
	private double[][][] phi;	//{情感-主题}对下的词汇分布  维度 S*K*V

	private double[][] pi; //特定文档下的情感分布  维度M*S

	private int[][] nms;	//每一项为：特定文档下的特定情感出现的次数   维度 M*S
	private int[] nmsSum;	//每一项为：特定文档下的所有情感出现的次数和
	private int[][][] nskv;	//每一项为：{情感-主题}对下的特定词汇出现的次数   维度 S*K*V
	private int[][] nskvSum;	//每一项为：{情感-主题}对下的所有词汇出现的次数和
	
	private int[][][] nmsk;	//每一项为：{时间-情感}对下的特定主题出现的次数   维度 T*S*K
	private int[][] nmskSum;	//每一项为：{时间-情感}对下的所有主题出现的次数和
	
	private int[][] s;		//每一项为：特定文档特定词下的情感标签  维度 M*Nm
	private int[][] z;		//每一项为：特定文档下的选择词汇的变量
	
	private int iterations;	//迭代次数
	private int burn_in;	//burn-in 时期
	private int saveStep;	//burn-in 过后，每saveStep次迭代保存一次结果
	private int topNum;	//显示主题下概率最高的前topNum词项
	
	private Documents documents;	//文档集
	
//	private int saveTime=0;	//保存次数计数变量
	
	
	
	public JSTModel(Documents documents)
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
		S=3; //情感类型
		
		phi=new double[S][K][V];
		pi=new double[M][S];
		gama=new double[S];
		gama[0]=gama[1]=0.01;
		gama[2]=0.011;
		
		nmsk=new int[M][S][K];
		nmskSum=new int[M][S];
		
		nms=new int[M][S];
		nmsSum=new int[M];
		nskv=new int[S][K][V];
		nskvSum=new int[S][K];
		
		s=new int[M][];
		z=new int[M][];
	}
	
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
						
			Document document=documents.docs.get(m);				
			int Nm=document.docWords.length;	//第m篇文档的词数（长度）
		
			s[m]=new int[Nm];
			z[m]=new int[Nm];
			for (int n = 0; n < Nm; n++) {
				int smn;
				
				if(termSenti[document.docWords[n]]!=-1)
					smn = termSenti[document.docWords[n]];
				else
					smn=(int) (Math.random()*(S));
				
				int zmn=(int) (Math.random()*(K));	//随机分配主题
				z[m][n]=zmn;
				s[m][n]=smn;
				nms[m][smn]++;
				nmsSum[m]++;
				nmsk[m][smn][zmn]++;
				nmskSum[m][smn]++;
				nskv[smn][zmn][document.docWords[n]]++;
				nskvSum[smn][zmn]++;
				
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
//					sampleForDoc(m);
					for (int n = 0; n < document.docWords.length; n++) {
						sampleForWord(m,n);
					}
				}
			}
		}
		System.out.println("Learn over!");
	}
	
	
	private void sampleForWord(int m, int n)
	{

		int termIndex=documents.docs.get(m).docWords[n];
		
		int oldSentiment=s[m][n];
		int oldTopic=z[m][n];
		nms[m][oldSentiment]--;
		nmsSum[m]--;
		nskv[oldSentiment][oldTopic][termIndex]--;
		nskvSum[oldSentiment][oldTopic]--;
		nmsk[m][oldSentiment][oldTopic]--;
		nmskSum[m][oldSentiment]--;
		
		double[][] p=new double[S][K];
		for (int s = 0; s < S; s++) {
	  	    for (int k = 0; k < K; k++) {	
  	    		p[s][k] = (nmsk[m][s][k]+gama[s])/(nmskSum[m][s]+K*0.01)*
 						(nms[m][s]+lambda)/(nmsSum[m]+S*lambda)*
 						(nskv[s][k][termIndex]+beta[s][k][termIndex])/(nskvSum[s][k]+betaSum[s][k]);
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
		z[m][n]=newTopic;
		s[m][n]=newSentiment;
		nskv[newSentiment][newTopic][termIndex]++;
		nskvSum[newSentiment][newTopic]++;
		nms[m][newSentiment]++;
		nmsSum[m]++;
		nmsk[m][newSentiment][newTopic]++;
		nmskSum[m][newSentiment]++;
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
		FilesUtil.saveCount(nms);
		FilesUtil.saveTopTopicWords(phi, topNum);
	}
	
}
