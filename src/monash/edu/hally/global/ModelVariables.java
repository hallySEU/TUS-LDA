package monash.edu.hally.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ModelVariables {
	
	public static ArrayList<String> g_termDictionary=new ArrayList<String>();
	
	public static Map<String, Integer> g_termToIndexMap=new HashMap<String, Integer>();
	
	public static ArrayList<String> g_usersSet=new ArrayList<String>();
	
	public static Map<String, Integer> g_userToIndexMap=new HashMap<String, Integer>();
	
	public static Map<String, Integer> g_userToCountMap=new HashMap<String, Integer>();
	
	public static ArrayList<String> g_timesSet=new ArrayList<String>();
	
	public static Map<String, Integer> g_timeToIndexMap=new HashMap<String, Integer>();
	
	public static int g_wordCount;

}
