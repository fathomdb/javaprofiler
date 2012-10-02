package com.fathomdb.profiler;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProfileData implements SymbolMap {
	final Map<long[], Observation> observations = new HashMap<long[], Observation>();

	final Map<Long, LocationInfo> locations = new HashMap<Long, LocationInfo>();

	static class Observation {
		final long[] stack;
		int count;

		public Observation(long[] stack) {
			this.stack = stack;
		}
	}

	public static void main(String[] args) throws IOException {
		JavaProfileData data = new JavaProfileData();
		data.read(new File("/tmp/profile.dat"));

		data.analyze();

		data.dumpTopNSelf(System.err, 10);
	}

	public static class SortSelfDesc implements Comparator<LocationInfo> {
		@Override
		public int compare(LocationInfo o1, LocationInfo o2) {
			return -(o1.selfCount - o2.selfCount);
		}
	}

	public static class SortCumulativeDesc implements Comparator<LocationInfo> {
		@Override
		public int compare(LocationInfo o1, LocationInfo o2) {
			return -(o1.cumulativeCount - o2.cumulativeCount);
		}
	}

	public void dumpTopNSelf(PrintStream dest, int n) {
		List<LocationInfo> list = new ArrayList<LocationInfo>(locations.values());
		Collections.sort(list, new SortSelfDesc());
		long total = 0;
		for (LocationInfo location : list) {
			total += location.selfCount;
		}

		List<LocationInfo> topN = list;
		if (topN.size() > n) {
			topN = topN.subList(0, n);
		}

		dest.println("!!!TOP 10 SELF!!!");
		for (LocationInfo info : topN) {
			NumberFormat percentFormat = NumberFormat.getPercentInstance();
			percentFormat.setMaximumFractionDigits(1);

			double percent = ((float) info.selfCount) / total;
			dest.println(percentFormat.format(percent) + " " + getMethodName(info, true));
		}
	}

	public void dumpTopNCumulative(PrintStream dest, int n) {
		List<LocationInfo> list = new ArrayList<LocationInfo>(locations.values());
		Collections.sort(list, new SortCumulativeDesc());

		List<LocationInfo> topN = list;
		if (topN.size() > n) {
			topN = topN.subList(0, n);
		}
		dest.println("!!!TOP 10 CUMULATIVE!!!");
		for (LocationInfo info : topN) {
			dest.println(info.cumulativeCount + ": " + getMethodName(info, true));
		}
	}

	public String getMethodName(LocationInfo location, boolean lineNumber) {
		return location.method;
	}

	public void analyze() {
		for (Observation observation : observations.values()) {
			long top = observation.stack[0];
			getLocation(top).selfCount += observation.count;

			for (long pc : observation.stack) {
				getLocation(pc).cumulativeCount += observation.count;
			}
		}
	}

	private LocationInfo getLocation(long pc) {
		LocationInfo location = locations.get(pc);
		if (location == null) {
			location = new LocationInfo(pc);
			locations.put(pc, location);
		}
		return location;
	}

	private DataInputStream dis;

	public void read(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		try {
			this.dis = new DataInputStream(fis);
			read();
			this.dis = null;
		} finally {
			fis.close();
		}
	}

	private void read() throws IOException {
		long headerCount = readSlot();
		long headerSlots = readSlot();
		long formatVersion = readSlot();
		long samplingPeriod = readSlot();
		long padding = readSlot();

		for (int i = 3; i < headerSlots; i++) {
			long unknown = readSlot();
		}

		while (true) {
			long sampleCount = readSlot();
			long depth = readSlot();
			if (sampleCount == 0 && depth == 1) {
				// End of (main data), although a single 0 pc value follows which we ignore
				break;
			}

			readSample(sampleCount, (int) depth);
		}
	}

	protected void readSample(long sampleCount, int depth) throws IOException {
		long[] sample = new long[depth];
		for (int i = 0; i < depth; i++) {
			sample[i] = readSlot();
		}
		recordSample(sample, sampleCount);
	}

	protected void recordSample(long[] sample, long sampleCount) {
		Observation observation = observations.get(sample);
		if (observation == null) {
			observation = new Observation(sample);
			observations.put(sample, observation);
		}
		observation.count += sampleCount;
	}

	protected long readSlot() throws IOException {
		long value = 0;
		for (int i = 0; i < 8; i++) {
			long b = dis.read();
			b <<= i * 8;
			value |= b;
		}
		return value;
	}

	public long getTotalCount() {
		return getTotalCount(observations.values());
	}

	public long getTotalCount(Iterable<Observation> observations) {
		long n = 0;
		for (Observation observation : observations) {
			n += observation.count;
		}
		return n;
	}

	public Collection<Observation> getObservations() {
		return observations.values();
	}

	@Override
	public List<String> translateStack(long[] k, boolean lineNumber) {
		List<String> methods = new ArrayList<String>();
		for (long pc : k) {
			LocationInfo location = getLocation(pc);
			String method = getMethodName(location, lineNumber);
			methods.add(method);
		}
		return methods;
	}

	// # Reduce profile to granularity given by user
	// sub ReduceProfile {
	public Map<String[], Long> getReduced() {
		// my $symbols = shift;
		// my $profile = shift;
		// my $result = {};
		Map<String[], Long> result = new HashMap<String[], Long>();

		// my $fullname_to_shortname_map = {};
		// FillFullnameToShortnameMap($symbols, $fullname_to_shortname_map);
		// foreach my $k (keys(%{$profile})) {
		for (Observation observation : observations.values()) {
			// my $count = $profile->{$k};
			long count = observation.count;
			// my @translated = TranslateStack($symbols, $fullname_to_shortname_map, $k);
			List<String> translated = translateStack(observation.stack, false);

			// my @path = ();
			List<String> path = new ArrayList<String>();

			// my %seen = ();
			Set<String> seen = new HashSet<String>();

			// $seen{''} = 1; # So that empty keys are skipped
			seen.add("");

			// foreach my $e (@translated) {
			for (String e : translated) {
				// # To avoid double-counting due to recursion, skip a stack-trace
				// # entry if it has already been seen
				// if (!$seen{$e}) {
				if (!seen.contains(e)) {
					// $seen{$e} = 1;
					seen.add(e);
					// push(@path, $e);
					path.add(e);
					// }
				}
				// }
			}
			// my $reduced_path = join("\n", @path);
			// String reducedPath = Joiner.on("\n").join(path);
			String[] reducedPath = path.toArray(new String[path.size()]);
			// AddEntry($result, $reduced_path, $count);
			ProfilerUtils.increment(result, reducedPath, count);
			// }
		}
		// return $result;
		return result;
		// }
	}

}
