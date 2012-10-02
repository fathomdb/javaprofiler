package com.fathomdb.profiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaProfileData extends ProfileData {
	static class JavaLocation {
		final long methodid;
		final int line;

		private JavaLocation(long methodid, int line) {
			this.methodid = methodid;
			this.line = line;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + line;
			result = prime * result + (int) (methodid ^ (methodid >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			JavaLocation other = (JavaLocation) obj;
			if (line != other.line) {
				return false;
			}
			if (methodid != other.methodid) {
				return false;
			}
			return true;
		}
	}

	final List<JavaLocation> locations = new ArrayList<JavaLocation>();
	final Map<JavaLocation, Integer> locationToIndex = new HashMap<JavaLocation, Integer>();

	@Override
	protected void readSample(long sampleCount, int depth) throws IOException {
		if (depth % 2 != 0) {
			throw new IllegalArgumentException();
		}

		long[] sample = new long[depth / 2];

		// We have to build virtual PCs, because we can't encode the methodid & line number in 64 bits...
		// System.out.println("Count=" + sampleCount + " depth=" + depth);
		for (int i = 0; i < depth; i += 2) {
			long methodId = readSlot();
			int line = (int) readSlot();

			JavaLocation location = new JavaLocation(methodId, line);

			Integer index = locationToIndex.get(location);
			if (index == null) {
				index = locations.size();
				locations.add(location);
				locationToIndex.put(location, index);
			}

			sample[i / 2] = index;
		}

		recordSample(sample, sampleCount);
	}

	@Override
	public String getMethodName(LocationInfo locationInfo, boolean lineNumber) {
		JavaLocation location = locations.get((int) locationInfo.pc);
		String[] methodInfo = Profiler.getMethodInfo(location.methodid);
		if (methodInfo == null) {
			String id = "Unknown methodid: " + location.methodid;
			if (lineNumber) {
				id += " @" + location.line;
			}
			return id;
		}

		String s = methodInfo[0] + methodInfo[1] + methodInfo[2] + methodInfo[3];
		if (lineNumber) {
			s += " @" + location.line;
		}

		if (lineNumber) {
			long[] lineNumberTable = Profiler.getLineNumberTable(location.methodid);
			if (lineNumberTable != null) {
				long sourceLine = -1;

				for (int i = 0; i < lineNumberTable.length; i += 2) {
					if (lineNumberTable[i] <= location.line) {
						sourceLine = lineNumberTable[i + 1];
					} else if (lineNumberTable[i] > location.line) {
						break;
					}
				}
				if (sourceLine != -1) {
					s += " srcLine=" + sourceLine;
				}
			}
		}

		return s;
	}

}
