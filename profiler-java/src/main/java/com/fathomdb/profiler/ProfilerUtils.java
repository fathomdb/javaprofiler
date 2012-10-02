package com.fathomdb.profiler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ProfilerUtils {
	public static <K> void increment(Map<K, Long> map, K key, long add) {
		Long count = map.get(key);
		if (count == null) {
			count = 0L;
		}
		count += add;
		map.put(key, count);
	}

	// # Get total count in profile
	// sub TotalProfile {
	public static <K> long totalProfile(Map<K, Long> profile) {
		// my $profile = shift;
		// my $result = 0;
		long result = 0;
		// foreach my $k (keys(%{$profile})) {
		for (long value : profile.values()) {
			// $result += $profile->{$k};
			result += value;
			// }
		}
		// return $result;
		return result;
		// }
	}

	// # Generate flattened profile:
	// # If count is charged to stack [a,b,c,d], in generated profile,
	// # it will be charged to [a]
	public static <K> Map<K, Long> getFlatProfile(Map<K[], Long> observations) {
		Map<K, Long> counts = new HashMap<K, Long>();

		for (Entry<K[], Long> entry : observations.entrySet()) {
			long count = entry.getValue();
			K[] stack = entry.getKey();
			if (stack.length == 0) {
				continue;
			}

			K ip = stack[0];
			ProfilerUtils.increment(counts, ip, count);
		}

		return counts;
	}

	// # Generate cumulative profile:
	// # If count is charged to stack [a,b,c,d], in generated profile,
	// # it will be charged to [a], [b], [c], [d]
	public static <K> Map<K, Long> getCumulativeProfile(Map<K[], Long> observations) {
		Map<K, Long> counts = new HashMap<K, Long>();

		for (Entry<K[], Long> entry : observations.entrySet()) {
			long count = entry.getValue();
			for (K ip : entry.getKey()) {
				ProfilerUtils.increment(counts, ip, count);
			}
		}

		return counts;
	}
}
