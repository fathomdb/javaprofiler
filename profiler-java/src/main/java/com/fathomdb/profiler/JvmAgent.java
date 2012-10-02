package com.fathomdb.profiler;

import java.io.File;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.util.Map;

import com.fathomdb.profiler.DotGraph;
import com.fathomdb.profiler.JavaProfileData;
import com.fathomdb.profiler.Profiler;
import com.fathomdb.profiler.ProfilerUtils;

public class JvmAgent {
	public static void premain(String agentArgs, Instrumentation inst) {
		if (!Profiler.isProfilerPresent()) {
			System.err.println("libprofiler.so not found");
			return;
		}

		// System.loadLibrary("profiler");

		final String path = "/tmp/profile.dat";

		Profiler.startProfiling(path);

		Runtime runtime = Runtime.getRuntime();
		runtime.addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				shutdownHook(path);
			}
		}));
	}

	static void shutdownHook(String path) {
		try {
			if (Profiler.isProfilerPresent()) {
				Profiler.stopProfiling();

				JavaProfileData data = new JavaProfileData();

				File file = new File(path);
				if (!file.exists()) {
					System.err.println("Profiling data was not found");
					return;
				}
				data.read(file);

				data.analyze();

				data.dumpTopNSelf(System.err, 15);
				data.dumpTopNCumulative(System.err, 30);

				DotGraph<String> dotGraph = new DotGraph<String>();
				PrintWriter out = new PrintWriter(new File("/tmp/profile.dot"));
				String prog = "Java program";

				Map<String[], Long> reduced = data.getReduced();
				Map<String, Long> flat = ProfilerUtils.getFlatProfile(reduced);
				Map<String, Long> cumulative = ProfilerUtils.getCumulativeProfile(reduced);

				// Map<Long, Long> flat = data.getFlatProfile();
				// Map<Long, Long> cumulative = data.getCumulativeProfile();
				long overallTotal = data.getTotalCount();

				dotGraph.printDot(out, prog, data, reduced, flat, cumulative, overallTotal);
				out.close();

				System.err.flush();
			}
		} catch (Exception e) {
			System.err.println("Unexpected error");
			e.printStackTrace(System.err);
		}
	}

}
