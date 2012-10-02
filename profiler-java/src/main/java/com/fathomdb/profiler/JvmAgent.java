package com.fathomdb.profiler;

import java.lang.instrument.Instrumentation;

import com.fathomdb.profiler.Profiler;

public class JvmAgent {
	public static void premain(String agentArgs, Instrumentation inst) {
		System.loadLibrary("profiler");

		if (!Profiler.isProfilerPresent()) {
			System.err.println("libprofiler.so not found");
		}
	}
}
