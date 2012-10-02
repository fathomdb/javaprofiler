package com.fathomdb.profiler;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;

public class Main {
	public static void main(String[] args) {
		System.out.println("FathomDB Java Profiler");

		try {
			if (args.length == 1) {
				JavaProfileData data = new JavaProfileData();
				data.read(new File(args[0]));

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
