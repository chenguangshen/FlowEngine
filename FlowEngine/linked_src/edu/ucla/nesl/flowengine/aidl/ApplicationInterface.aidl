package edu.ucla.nesl.flowengine.aidl;

interface ApplicationInterface {
	void publishString(String name, String data, long timestamp);
	void publishDouble(String name, double data, long timestamp);
	void publishDoubleArray(String name, out double[] data, int length, long timestamp);
	void publishInt(String name, int data, long timestamp);
	void publishIntArray(String name, out int[] data, int length, long timestamp);
}