package edu.ucla.nesl.flowengine.node.feature;

import java.util.ArrayList;
import java.util.Arrays;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class IERatio extends DataFlowNode {
	private static final String TAG = IERatio.class.getSimpleName();

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
		int inhalation = 0,exhalation = 0;
		
		ArrayList<Integer> list=new ArrayList<Integer>();
		int temp=length;
		for(int i=0;i<temp;i+=4)
		{
			//check the starting whether it starts from valley or not. It should be valley
			if((i==0) && (data[i+1]>data[i+3]))
				continue;						//it escaping if first member is a peak. in that case we can not find the inspiration. inspiration always starts from a valley
			
			//check last element whether it is valley or peak. it should be valley
			if((i==0)&&(data[length-1]>data[length-3]))		//at the beginning the stopping condition is changed
				temp=length-2;						//skipping the last one if it is peak
			
			if(i+4<length)
			{
				inhalation=data[i+2]-data[i];
				exhalation=data[i+4]-data[i+2];
				float ieRatio=(float)inhalation/exhalation;
				int raoundedIeRatio=(int)(ieRatio*10000);
				list.add(new Integer(raoundedIeRatio));
			}
		}
		
		//converting the ArrayList to array
		int ieRatio[]=new int[list.size()];
		for(int j=0;j<list.size();j++)
		{
			ieRatio[j]=list.get(j).intValue();
		}
		
		DebugHelper.dump(TAG, ieRatio);
		
		outputData(name + "IERatio", "int[]", ieRatio, ieRatio.length, timestamp);
	}
	
}
