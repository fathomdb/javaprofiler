package com.fathomdb.profiler;

public class LocationInfo {
	public final long pc;
	String method;
	int selfCount;
	int cumulativeCount;

	public LocationInfo(long pc) {
		this.pc = pc;
	}

	@Override
	public String toString() {
		return "pc=" + pc + " method=" + method + " selfCount=" + selfCount + " cumulativeCount=" + cumulativeCount;
	}

}