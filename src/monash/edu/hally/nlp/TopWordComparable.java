package monash.edu.hally.nlp;

import java.util.Comparator;

/**
 * 作用：将主题下的词汇，以概率的方式降序排列
 */
public class TopWordComparable implements Comparator<Integer> {
	
	private double phi[];

	public TopWordComparable(double phi[])
	{
		this.phi=phi;
	}

	@Override
	public int compare(Integer o1, Integer o2) {

		if(phi[o1]>phi[o2]) return -1;
		if(phi[o1]<phi[o2]) return 1;
		return 0;
	}
}
