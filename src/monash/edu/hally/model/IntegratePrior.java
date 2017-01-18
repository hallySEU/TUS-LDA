package monash.edu.hally.model;

import java.io.File;
import java.util.ArrayList;

import monash.edu.hally.constant.ModelConstants;
import monash.edu.hally.global.ModelVariables;
import monash.edu.hally.nlp.FilesUtil;

public class IntegratePrior {
	
	private double [][][]beta;	//***先验信息***每一项为：情感s,主题k和词term对应的dirichlet超参   维度 S*K*V
	private double [][]omega;	//***先验信息***每一项为：情感s和词term对应的dirichlet超参   维度 S*V
	private double [][]betaSum;	//***先验信息***每一项为：情感s,主题k和词term对应的dirichlet超参和   维度 S*K
	
	private int []termSenti;	//***先验信息***每一项为：词term的情感标签   维度 V
	
	private int S,K,V;
	
	private boolean isExitDictionary=true;
	
	int size=0;
	
	public IntegratePrior(int S, int K, int V)
	{
		this.S=S;
		this.K=K;
		this.V=V;
		setDefaultPrior();
	}
		
	private void setDefaultPrior()
	{
		beta=new double[S][K][V];
		betaSum=new double[S][K];
		for (int s = 0; s < S; s++) {
			for (int k = 0; k < K; k++) {
				for (int v = 0; v < V; v++) {
					beta[s][k][v] = 0.01;
					betaSum[s][k] += beta[s][k][v];
				}
			}
		}
		omega=new double[S][V];
		for (int s = 0; s < S; s++) {	
			for (int v = 0; v < V; v++) {
				omega[s][v]=1;
			}	
		}
		termSenti=new int[V];
		for (int v = 0; v < V; v++) {
			termSenti[v]=-1;
		}	
	}
	
	/**
	 * 如果不存在字典，相当于使用默认先验参数值
	 */
	public void setSentiPrior()
	{
		integration();
		if(isExitDictionary){
			for (int s = 0; s < S; s++) {
				for (int k = 0; k < K; k++) {
					betaSum[s][k]=0;
					for (int v = 0; v < V; v++) {
						beta[s][k][v] = beta[s][k][v] * omega[s][v];
						betaSum[s][k] += beta[s][k][v];
					}
				}
			}
		}
	}
	
	
	//sentiItems数组：term 0, neutral 1, positive 2, negative 3
	//termSenti数组中0表示中性，1表示积极，2表示消极
	private void setTermSentiPrior(String sentiItems[], int termIndex)
	{
//		System.out.println(++size);
		if(Double.valueOf(sentiItems[1])>0.8){
			omega[1][termIndex]=0;
			omega[2][termIndex]=0;
			termSenti[termIndex]=0;	
		}
		else if(Double.valueOf(sentiItems[2])>0.8){
			omega[0][termIndex]=0;
			omega[2][termIndex]=0;
			termSenti[termIndex]=1;	
		}
		else if(Double.valueOf(sentiItems[3])>0.8){
			omega[0][termIndex]=0;
			omega[1][termIndex]=0;
			termSenti[termIndex]=2;	
		}
	}
	
	/**
	 * 融合情感信息
	 */
	private void integration()
	{
		if(!new File(ModelConstants.SENTIMENT_DICTIONARY).exists()){
			System.err.println("You don't set sentiment dictionary.");
			isExitDictionary=false;
			return;
		}
		//读情感词字典
		ArrayList<String> sentiDictionary=FilesUtil.readDocument(
				ModelConstants.SENTIMENT_DICTIONARY);
		//读表情字典
		ArrayList<String> emoticonDictionary=FilesUtil.readDocument(
				ModelConstants.SENTIMENT_EMOTICONS);
		
		sentiDictionary.addAll(emoticonDictionary);
		
		for (int i=0; i<ModelVariables.g_termDictionary.size(); i++) {
			
			String term=ModelVariables.g_termDictionary.get(i);
			for (String senti : sentiDictionary) {
				String sentiItems[]=senti.split("\t");
				if(term.equals(sentiItems[0])){
					setTermSentiPrior(sentiItems, i);
				}	
			}
		}
	}
	
	public double[][][] getBeta() {
		return beta;
	}
	public double[][] getOmega() {
		return omega;
	}
	public double[][] getBetaSum() {
		return betaSum;
	}
	public int[] getTermSenti() {
		return termSenti;
	}
	
	public void print()
	{
		for (int v = 0; v < V; v++) {
			System.out.print(termSenti[v]+"\t");
			System.out.print(ModelVariables.g_termDictionary.get(v)+"\t");
			
			for (int s = 0; s < S; s++) {
				System.out.print(beta[s][0][v]+"\t");
			}
			System.out.println();
		}
	}

}
