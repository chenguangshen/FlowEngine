package edu.ucla.nesl.flowengine.node.feature;

import java.util.Arrays;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class RRInterval extends DataFlowNode {
	private static final String TAG = RRInterval.class.getSimpleName();
	
	private static int mRR_ptr = 0;
	int mLastPeakTime = 0;
	private int[] mRR = new int[256];

	@Override
	protected String processParentNodeName(String parentNodeName) {
		if (parentNodeName.contains("|Buffer")) {
			return parentNodeName.split("\\|Buffer")[0];
		}
		return parentNodeName;
	}

	private int[] calculateRRInterval(int[] data) {
		int[] tbody = lowpass(data);
		int[] tbandbody0 = bandpass(tbody);
		int[] tdiffbody0 = diffsqfilt(tbandbody0);
		int[] tlpbody0 = lpfilt(tdiffbody0);
		mRR = findpks(tlpbody0);

		int[] return_RR = new int[mRR_ptr];
		System.arraycopy(mRR, 0, return_RR, 0, mRR_ptr);

		Arrays.fill(mRR, 0, mRR_ptr, 0);
		mRR_ptr = 0;

		DebugHelper.dump(TAG, return_RR);
		
		return return_RR;
	}
	
	@Override
	protected void processInput(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		if (!type.equals("int[]")) {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}

		int[] rrInterval = calculateRRInterval((int[])inputData);

		output(name + "RRInterval", "int[]", rrInterval, rrInterval.length, timestamp);
	}
	
	private int[] findpks(int[] tlpbody02) {
		final float RR_high_limit = (float) 1.4;
		final float RR_low_limit = (float) 0.6;
		final float RR_missed_limit = (float) 1.6;
		final int min_distance = 30;
		float SPK = 300;
		float NPK = 60;
		float THR1 = 150;
		float THR2 = 75;
		int countq = 0;
		int x;
		int curPeak;
		int temp1 = 0;
		int RR_sel_ptr = 0;
		float RRAVERAGE_1 = 45;
		int[] RR_SEL = new int[8];
		int SIZE = tlpbody02.length;
		int[] pks = new int[SIZE];
		int[] valuepks = new int[SIZE];
		int count = 0;
		for (int j = 2; j < SIZE - 2; j++) {
			if (tlpbody02[j - 2] < tlpbody02[j - 1]
					&& tlpbody02[j - 1] < tlpbody02[j]
					&& tlpbody02[j] >= tlpbody02[j + 1]
					&& tlpbody02[j + 1] > tlpbody02[j + 2]) {
				pks[count] = j - 1;
				valuepks[count] = tlpbody02[j];
				count++;
			}
		}

		for (int j = 0; j < count; j++) {
			x = pks[j];
			curPeak = valuepks[j];
			if (curPeak > THR1 && x - mLastPeakTime > min_distance) {
				SPK = (float) (curPeak * 0.125 + SPK * 0.875);
				mRR[mRR_ptr] = pks[j] - mLastPeakTime;
				// out.println(RR[RR_ptr]);
				if (mRR[mRR_ptr] <= RRAVERAGE_1 * RR_high_limit
						&& mRR[mRR_ptr] >= RRAVERAGE_1 * RR_low_limit) {
					RR_SEL[RR_sel_ptr] = (int) mRR[mRR_ptr];
					RR_sel_ptr = RR_sel_ptr + 1;
					mRR_ptr = mRR_ptr + 1;
					if (RR_sel_ptr == 8) {
						RR_sel_ptr = 0;
						for (int m = 0; m < 8; m++) {
							temp1 = temp1 + RR_SEL[m];
						}
						RRAVERAGE_1 = (float) (temp1 * 0.125);
					}
					temp1 = 0;
				}
				mLastPeakTime = x;
				countq = countq + 1;
			} else if (pks[j] - mLastPeakTime > RRAVERAGE_1 * RR_missed_limit) {
				for (int k = 1; k < j; k++) {
					curPeak = valuepks[k];
					if (curPeak > SPK * 0.25) {
						if (curPeak > THR2
								&& (pks[k] - mLastPeakTime > min_distance)) {
							countq = countq + 1;
							SPK = (float) (0.125 * curPeak + 0.875 * SPK);
							mRR[mRR_ptr] = pks[k] - mLastPeakTime;
							mLastPeakTime = pks[k];
							if (mRR[mRR_ptr] <= RRAVERAGE_1 * RR_high_limit
									&& mRR[mRR_ptr] >= RRAVERAGE_1 * RR_low_limit) {
								RR_SEL[RR_sel_ptr] = (int) mRR[mRR_ptr];
								RR_sel_ptr = RR_sel_ptr + 1;
								mRR_ptr = mRR_ptr + 1;
								if (RR_sel_ptr == 8) {
									RR_sel_ptr = 0;
									for (int m = 0; m < 8; m++) {
										temp1 = temp1 + RR_SEL[m];
									}
									RRAVERAGE_1 = (float) (temp1 * 0.125);
								}
								temp1 = 0;
							}
						}
					}
				}
			} else {
				NPK = (float) (curPeak * 0.125 + NPK * 0.875);
			}
			THR1 = (float) (NPK + (SPK - NPK) * 0.25);
			THR2 = THR1 / 2;
		}
		mLastPeakTime = mLastPeakTime - SIZE;
		return mRR;
	}

	// First low pass filter
	private int[] lowpass(int[] data) {
		int k = 0;
		int SIZE = data.length;
		int[] x0 = new int[4];
		int[] y0 = new int[4];
		for (int i = 0; i < SIZE; i++) {
			x0[k] = data[i];
			y0[k] = B0 * x0[k] + B1 * x0[(k + 3) % FILTER_SIZE] + B2
					* x0[(k + 2) % FILTER_SIZE] + B3
					* x0[(k + 1) % FILTER_SIZE] - A1
					* y0[(k + 3) % FILTER_SIZE] - A2
					* y0[(k + 2) % FILTER_SIZE] - A3
					* y0[(k + 1) % FILTER_SIZE];
			y0[k] = (y0[k]) >> FILTER_GAIN_REMOVE;
			if (y0[k] > 0) {
				data[i] = y0[k];
			} else {
				data[i] = 2000;
			}
			k = (k + 1) % FILTER_SIZE;
		}
		return data;
	}

	// Band Pass filter
	private int[] bandpass(int[] data) {
		int k1 = 0;
		int SIZE = data.length;
		int[] x1 = new int[5];
		int[] y1 = new int[5];
		for (int i = 0; i < SIZE; i++) {
			x1[k1] = data[i];
			y1[k1] = BB0 * x1[k1] + BB1 * x1[(k1 + 4) % FILTER_SIZE_BAND] + BB2
					* x1[(k1 + 3) % FILTER_SIZE_BAND] + BB3
					* x1[(k1 + 2) % FILTER_SIZE_BAND] + BB4
					* x1[(k1 + 1) % FILTER_SIZE_BAND] - AA1
					* y1[(k1 + 4) % FILTER_SIZE_BAND] - AA2
					* y1[(k1 + 3) % FILTER_SIZE_BAND] - AA3
					* y1[(k1 + 2) % FILTER_SIZE_BAND] - AA4
					* y1[(k1 + 1) % FILTER_SIZE_BAND];

			y1[k1] = (y1[k1]) >> FILTER_GAIN_REMOVE_BAND;
			if (y1[k1] == 0) {
				data[i] = 0;
			} else {
				data[i] = y1[k1] + 1500;
			}
			k1 = (k1 + 1) % FILTER_SIZE_BAND;
		}
		return data;
	}

	// Derivative filter and squaring
	private int[] diffsqfilt(int[] data) {
		int[] x2 = new int[5];
		int[] y2 = new int[5];
		int k2 = 0;
		final int SQ = 12;
		int SIZE = data.length;
		for (int i = 0; i < SIZE; i++) {
			x2[k2] = data[i];
			y2[k2] = BBD0 * x2[k2] + BBD1 * x2[(k2 + 4) % FILTER_SIZE_DIFF]
					+ BBD2 * x2[(k2 + 3) % FILTER_SIZE_DIFF] + BBD3
					* x2[(k2 + 2) % FILTER_SIZE_DIFF] + BBD4
					* x2[(k2 + 1) % FILTER_SIZE_DIFF];
			y2[k2] = (y2[k2]) >> FILTER_GAIN_REMOVE_DIFF;
			data[i] = y2[k2];
			if (data[i] > -4096 && data[i] < 4096) {
				data[i] = data[i] * data[i];
				data[i] = data[i] >> SQ;
			}

			k2 = (k2 + 1) % FILTER_SIZE_DIFF;
		}
		return data;
	}

	// Final Low Pass filter2
	private int[] lpfilt(int[] data) {
		int[] x3 = new int[26];
		int[] y3 = new int[26];
		int k3 = 0;
		int SIZE = data.length;
		for (int i = 0; i < SIZE; i++) {
			x3[k3] = data[i];

			y3[k3] = AAL0 * x3[k3] + AAL1 * x3[(k3 + 25) % FILTER_SIZE_LP]
					+ AAL2 * x3[(k3 + 24) % FILTER_SIZE_LP]
					+ AAL3 * x3[(k3 + 23) % FILTER_SIZE_LP]
					+ AAL4 * x3[(k3 + 22) % FILTER_SIZE_LP]
					+ AAL5 * x3[(k3 + 21) % FILTER_SIZE_LP]
					+ AAL6 * x3[(k3 + 20) % FILTER_SIZE_LP]
					+ AAL7 * x3[(k3 + 19) % FILTER_SIZE_LP]
					+ AAL8 * x3[(k3 + 18) % FILTER_SIZE_LP]
					+ AAL9 * x3[(k3 + 17) % FILTER_SIZE_LP]
					+ AAL10 * x3[(k3 + 16) % FILTER_SIZE_LP]
					+ AAL11 * x3[(k3 + 15) % FILTER_SIZE_LP]
					+ AAL12 * x3[(k3 + 14) % FILTER_SIZE_LP]
					+ AAL13 * x3[(k3 + 13) % FILTER_SIZE_LP]
					+ AAL14 * x3[(k3 + 12) % FILTER_SIZE_LP]
					+ AAL15 * x3[(k3 + 11) % FILTER_SIZE_LP]
					+ AAL16 * x3[(k3 + 10) % FILTER_SIZE_LP]
					+ AAL17 * x3[(k3 + 9) % FILTER_SIZE_LP]
					+ AAL18 * x3[(k3 + 8) % FILTER_SIZE_LP]
					+ AAL19 * x3[(k3 + 7) % FILTER_SIZE_LP]
					+ AAL20 * x3[(k3 + 6) % FILTER_SIZE_LP]
					+ AAL21 * x3[(k3 + 5) % FILTER_SIZE_LP] + AAL22
					* x3[(k3 + 4) % FILTER_SIZE_LP] + AAL23
					* x3[(k3 + 3) % FILTER_SIZE_LP] + AAL24
					* x3[(k3 + 2) % FILTER_SIZE_LP] + AAL25
					* x3[(k3 + 1) % FILTER_SIZE_LP];

			y3[k3] = (y3[k3]) >> FILTER_GAIN_REMOVE_LP;
			if (y3[k3] > -500) {
				data[i] = y3[k3];
			} else {
				data[i] = 0;
			}
			k3 = (k3 + 1) % FILTER_SIZE_LP;
		}
		return data;
	}

	private static final int FILTER_GAIN_REMOVE = 16; // Right shift by X
	private static final int A1 = -8208;
	private static final int A2 = 5927;
	private static final int A3 = -1482;
	private static final int B0 = 4096;
	private static final int B1 = 12288;
	private static final int B2 = 12288;
	private static final int B3 = 4096;
	private static final int FILTER_SIZE = 4;

	private static final int FILTER_GAIN_REMOVE_BAND = 21; // Right shift by X
	private static final int BB0 = 1048576;
	private static final int BB1 = -1593350;
	private static final int BB2 = 1361436;
	private static final int BB3 = -826591;
	private static final int BB4 = -826591;
	private static final int AA1 = 0;
	private static final int AA2 = -343174;
	private static final int AA3 = 0;
	private static final int AA4 = 0;
	private static final int FILTER_SIZE_BAND = 5;

	private static final int FILTER_GAIN_REMOVE_DIFF = 2; // Right shift by X
	private static final int BBD0 = -69;
	private static final int BBD1 = 149;
	private static final int BBD2 = 0;
	private static final int BBD3 = -147;
	private static final int BBD4 = 69;
	private static final int FILTER_SIZE_DIFF = 5;

	private static final int FILTER_GAIN_REMOVE_LP = 20;
	private static final int AAL0 = -678;
	private static final int AAL1 = -2916;
	private static final int AAL2 = -7628;
	private static final int AAL3 = -15015;
	private static final int AAL4 = -23327;
	private static final int AAL5 = -28634;
	private static final int AAL6 = -25219;
	private static final int AAL7 = -7737;
	private static final int AAL8 = 26249;
	private static final int AAL9 = 73875;
	private static final int AAL10 = 126634;
	private static final int AAL11 = 172268;
	private static final int AAL12 = 198793;
	private static final int AAL13 = 198793;
	private static final int AAL14 = 172268;
	private static final int AAL15 = 126634;
	private static final int AAL16 = 73875;
	private static final int AAL17 = 26249;
	private static final int AAL18 = -7737;
	private static final int AAL19 = -25219;
	private static final int AAL20 = -28634;
	private static final int AAL21 = -23327;
	private static final int AAL22 = -15015;
	private static final int AAL23 = -7628;
	private static final int AAL24 = -2916;
	private static final int AAL25 = -678;
	private static final int FILTER_SIZE_LP = 26;


}
