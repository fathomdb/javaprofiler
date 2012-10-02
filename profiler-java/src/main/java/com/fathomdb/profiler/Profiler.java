package com.fathomdb.profiler;

public class Profiler {
	private static Boolean present;

	private native String[] getMethodInfo0(long methodId);

	private native void start0(String path);

	private native long[] getLineNumberTable0(long methodId);

	private native void stop0();

	private native long getMagic0();

	static final Profiler profiler = new Profiler();

	static String[] getMethodInfo(long methodId) {
		return profiler.getMethodInfo0(methodId);
	}

	static long[] getLineNumberTable(long methodId) {
		return profiler.getLineNumberTable0(methodId);
	}

	public static boolean isProfilerPresent() {
		if (present == null) {
			synchronized (profiler) {
				try {
					profiler.getMagic0();
					present = true;
				} catch (UnsatisfiedLinkError e) {
					present = false;
				}
			}
		}
		return present;
	}

	public static void startProfiling(String path) {
		synchronized (profiler) {
			profiler.start0(path);
		}
	}

	public static void stopProfiling() {
		synchronized (profiler) {
			profiler.stop0();
		}
	}

	static {
		// System.loadLibrary("profiler");
	}
}