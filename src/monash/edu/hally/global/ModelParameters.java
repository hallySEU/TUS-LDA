package monash.edu.hally.global;

public class ModelParameters {
	
	
	private int iterations;	//迭代次数
	private int burn_in;	//burn-in 时期
	private int saveStep;	//burn-in 过后，每saveStep次迭代保存一次结果

	private int K; //主题数目
	private int topNum;	//	显示主题下概率最高的前topNum词项
	
	
	public int getIterations() {
		return iterations;
	}
	public void setIterations(int iterations) {
		this.iterations = iterations;
	}
	public int getBurn_in() {
		return burn_in;
	}
	public void setBurn_in(int burn_in) {
		this.burn_in = burn_in;
	}
	public int getSaveStep() {
		return saveStep;
	}
	public void setSaveStep(int saveStep) {
		this.saveStep = saveStep;
	}
	public int getK() {
		return K;
	}
	public void setK(int k) {
		K = k;
	}
	public int getTopNum() {
		return topNum;
	}
	public void setTopNum(int topNum) {
		this.topNum = topNum;
	}
	
	
}
