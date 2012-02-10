package edu.ucla.nesl.flowengine.node.feature;

import java.util.ArrayList;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Exhalation extends DataFlowNode {
	private static final String TAG = Exhalation.class.getSimpleName();
	
	@Override
	public void inputData(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		if (!type.equals("int[]")) {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}

		int[] data = (int[])inputData;
		int valleyIndex=0,peakIndex=0;
		
		ArrayList<Integer> list=new ArrayList<Integer>();
		
		int i=0;
		int temp=length;
		for(;i<temp;i+=4)
		{
			//check the starting whether it starts from valley or not. It should be valley
			if((i==0) && (data[i+1]>data[i+3]))
				continue;						//it escaping if first member is a peak. in that case we can not find the inspiration. inspiration always starts from a valley
			
			
			//check last element whether it is valley or peak. it should be valley
			if((i==0)&&(data[length-1]>data[length-3]))		//at the beginning the stopping condition is changed
				temp=length-2;
			
			if(i+4<length)
			{
				valleyIndex=data[i+4];
				peakIndex=data[i+2];
				list.add(new Integer(valleyIndex-peakIndex));
			}
		}
		//converting the ArrayList to array
		int exhalation[]=new int[list.size()];
		for(int j=0;j<list.size();j++)
		{
			exhalation[j]=list.get(j).intValue();
		}
		
		DebugHelper.dump(TAG, exhalation);
		
		outputData(name + "Exhalation", "int[]", exhalation, exhalation.length, timestamp);
	}

}
