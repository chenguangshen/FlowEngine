package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Mean extends DataFlowNode {
	private static final String TAG = Mean.class.getSimpleName();
	
	@Override
	public void inputData(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}

		double mean = 0;
		if (type.equals("int[]")) {
			int[] data = (int[])inputData;
			for (int value: data) {
				mean += value;
			}
		} else if (type.equals("double[]")) {
			double[] data = (double[])inputData;
			for (double value: data) {
				mean += value;
			}
		} else if (type.equals("float[]")) {
			float[] data = (float[])inputData;
			for (float value: data) {
				mean += value;
			}
		} else if (type.equals("long[]")) {
			long[] data = (long[])inputData;
			for (long value: data) {
				mean += value;
			}
		} else {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}
		mean /= length;
		
		DebugHelper.log(TAG, "Mean: " + mean);
		
		outputData(name + "Mean", "double", mean, 0, timestamp);
	}
}
