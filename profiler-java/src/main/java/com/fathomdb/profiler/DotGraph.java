package com.fathomdb.profiler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class DotGraph<K> {
	long totalCount;

	float optNodeFraction = 0.005f;
	float optEdgeFraction = 0.001f;
	int optNodeCount = 80;
	int optMaxDegree = 8;

	String title = "title??";

	// # Print DOT graph
	// sub PrintDot {
	public boolean printDot(PrintWriter out, String prog, SymbolMap symbols, Map<K[], Long> raw, Map<K, Long> flat,
			Map<K, Long> cumulative, long overallTotal) {
		// my $prog = shift;
		// my $symbols = shift;
		// my $raw = shift;
		// my $flat = shift;
		// my $cumulative = shift;
		// my $overall_total = shift;
		//
		// # Get total
		// my $local_total = TotalProfile($flat);
		long localTotal = ProfilerUtils.totalProfile(flat);
		// my $nodelimit = int($main::opt_nodefraction * $local_total);
		int nodeLimit = (int) (optNodeFraction * localTotal);
		// my $edgelimit = int($main::opt_edgefraction * $local_total);
		int edgeLimit = (int) (optEdgeFraction * localTotal);
		// my $nodecount = $main::opt_nodecount;
		int nodeCount = this.optNodeCount;
		//
		// # Find nodes to include
		// my @list = (sort { abs(GetEntry($cumulative, $b)) <=>
		// abs(GetEntry($cumulative, $a))
		// || $a cmp $b }
		// keys(%{$cumulative}));
		List<K> list = keysByCountDescending(cumulative);
		// my $last = $nodecount - 1;
		int last = nodeCount - 1;
		// if ($last > $#list) {
		if (last >= list.size()) {
			// $last = $#list;
			last = list.size() - 1;
			// }
		}

		// while (($last >= 0) &&
		// (abs(GetEntry($cumulative, $list[$last])) <= $nodelimit)) {
		while ((last >= 0) && (Math.abs(cumulative.get(list.get(last))) <= nodeLimit)) {
			// $last--;
			last--;
			// }
		}

		// if ($last < 0) {
		// print STDERR "No nodes to print\n";
		// return 0;
		// }
		if (last < 0) {
			return false;
		}
		//
		// if ($nodelimit > 0 || $edgelimit > 0) {
		// printf STDERR ("Dropping nodes with <= %s %s; edges with <= %s abs(%s)\n",
		// Unparse($nodelimit), Units(),
		// Unparse($edgelimit), Units());
		// }
		//
		// # Open DOT output file
		// my $output;
		// my $escaped_dot = ShellEscape(@DOT);
		// my $escaped_ps2pdf = ShellEscape(@PS2PDF);
		// if ($main::opt_gv) {
		// my $escaped_outfile = ShellEscape(TempName($main::next_tmpfile, "ps"));
		// $output = "| $escaped_dot -Tps2 >$escaped_outfile";
		// } elsif ($main::opt_evince) {
		// my $escaped_outfile = ShellEscape(TempName($main::next_tmpfile, "pdf"));
		// $output = "| $escaped_dot -Tps2 | $escaped_ps2pdf - $escaped_outfile";
		// } elsif ($main::opt_ps) {
		// $output = "| $escaped_dot -Tps2";
		// } elsif ($main::opt_pdf) {
		// $output = "| $escaped_dot -Tps2 | $escaped_ps2pdf - -";
		// } elsif ($main::opt_web || $main::opt_svg) {
		// # We need to post-process the SVG, so write to a temporary file always.
		// my $escaped_outfile = ShellEscape(TempName($main::next_tmpfile, "svg"));
		// $output = "| $escaped_dot -Tsvg >$escaped_outfile";
		// } elsif ($main::opt_gif) {
		// $output = "| $escaped_dot -Tgif";
		// } else {
		// $output = ">&STDOUT";
		// }
		// open(DOT, $output) || error("$output: $!\n");
		//
		// # Title
		// printf DOT ("digraph \"%s; %s %s\" {\n",
		// $prog,
		// Unparse($overall_total),
		// Units());
		out.printf("digraph \"%s; %s %s\" {\n", title, prettyNumber(overallTotal), units());

		// if ($main::opt_pdf) {
		// # The output is more printable if we set the page size for dot.
		// printf DOT ("size=\"8,11\"\n");
		// }

		// printf DOT ("node [width=0.375,height=0.25];\n");
		out.println("node [width=0.375,height=0.25];\n");

		//
		// # Print legend
		// printf DOT ("Legend [shape=box,fontsize=24,shape=plaintext," .
		// "label=\"%s\\l%s\\l%s\\l%s\\l%s\\l\"];\n",
		// $prog,
		// sprintf("Total %s: %s", Units(), Unparse($overall_total)),
		// sprintf("Focusing on: %s", Unparse($local_total)),
		// sprintf("Dropped nodes with <= %s abs(%s)",
		// Unparse($nodelimit), Units()),
		// sprintf("Dropped edges with <= %s %s",
		// Unparse($edgelimit), Units())
		// );
		out.printf("Legend [shape=box,fontsize=24,shape=plaintext," + "label=\"%s\\l%s\\l%s\\l%s\\l%s\\l\"];\n", prog,
				String.format("Total %s: %s", units(), prettyNumber(overallTotal)),
				String.format("Focusing on: %s", prettyNumber(localTotal)),
				String.format("Dropped nodes with <= %s abs(%s)", prettyNumber(nodeLimit), units()),
				String.format("Dropped edges with <= %s %s", prettyNumber(nodeLimit), units()));

		//
		// # Print nodes
		// my %node = ();
		Map<K, Integer> node = new HashMap<K, Integer>();
		// my $nextnode = 1;
		int nextNode = 1;
		// foreach my $a (@list[0..$last]) {
		for (K a : list.subList(0, last + 1)) {
			// # Pick font size
			// my $f = GetEntry($flat, $a);
			long f = getEntry(flat, a);
			// my $c = GetEntry($cumulative, $a);
			long c = cumulative.get(a);
			//
			// my $fs = 8;
			float fs = 8.0f;
			// if ($local_total > 0) {
			// $fs = 8 + (50.0 * sqrt(abs($f * 1.0 / $local_total)));
			// }
			if (localTotal > 0) {
				fs = (float) (8.0 + (50.0 * Math.sqrt(Math.abs(f * 1.0 / localTotal))));
			}
			//
			// $node{$a} = $nextnode++;
			node.put(a, nextNode++);
			// my $sym = $a;
			String sym = a.toString();
			// $sym =~ s/\s+/\\n/g;
			// $sym =~ s/::/\\n/g;

			// # Extra cumulative info to print for non-leaves
			// my $extra = "";
			String extra = "";
			// if ($f != $c) {
			if (f != c) {
				// $extra = sprintf("\\rof %s (%s)",
				// Unparse($c),
				// Percent($c, $local_total));
				extra = String.format("\\rof %s (%s)", prettyNumber(c), prettyPercent(c, localTotal));
				// }
			}

			// my $style = "";
			String style = "";
			// if ($main::opt_heapcheck) {
			// if ($f > 0) {
			// # make leak-causing nodes more visible (add a background)
			// $style = ",style=filled,fillcolor=gray"
			// } elsif ($f < 0) {
			// # make anti-leak-causing nodes (which almost never occur)
			// # stand out as well (triple border)
			// $style = ",peripheries=3"
			// }
			// }
			//
			// printf DOT ("N%d [label=\"%s\\n%s (%s)%s\\r" .
			// "\",shape=box,fontsize=%.1f%s];\n",
			// $node{$a},
			// $sym,
			// Unparse($f),
			// Percent($f, $local_total),
			// $extra,
			// $fs,
			// $style,
			// );
			out.printf("N%d [label=\"%s\\n%s (%s)%s\\r" + "\",shape=box,fontsize=%.1f%s];\n", node.get(a), sym,
					prettyNumber(f), prettyPercent(f, localTotal), extra, fs, style);
			// }
		}

		// # Get edges and counts per edge
		// my %edge = ();
		Map<String, Long> edge = new HashMap<String, Long>();

		// my $n;
		// my $fullname_to_shortname_map = {};
		// FillFullnameToShortnameMap($symbols, $fullname_to_shortname_map);
		// foreach my $k (keys(%{$raw})) {
		for (Entry<K[], Long> entry : raw.entrySet()) {
			// # TODO: omit low %age edges
			// $n = $raw->{$k};
			K[] k = entry.getKey();
			long n = entry.getValue();

			// my @translated = TranslateStack($symbols, $fullname_to_shortname_map, $k);
			List<String> translated = translateStack(k);
			// for (my $i = 1; $i <= $#translated; $i++) {
			for (int i = 1; i < translated.size(); i++) {
				// my $src = $translated[$i];
				// my $dst = $translated[$i-1];
				String src = translated.get(i);
				String dst = translated.get(i - 1);

				// #next if ($src eq $dst); # Avoid self-edges?

				// if (exists($node{$src}) && exists($node{$dst})) {
				if (node.containsKey(src) && node.containsKey(dst)) {
					// my $edge_label = "$src\001$dst";
					String edgeLabel = src + "\001" + dst;
					// if (!exists($edge{$edge_label})) {
					// $edge{$edge_label} = 0;
					// }
					// $edge{$edge_label} += $n;
					ProfilerUtils.increment(edge, edgeLabel, n);
					// }
				}
				// }
			}
			// }
		}
		//
		// # Print edges (process in order of decreasing counts)
		// my %indegree = (); # Number of incoming edges added per node so far
		// my %outdegree = (); # Number of outgoing edges added per node so far
		Map<String, Long> indegree = new HashMap<String, Long>();
		Map<String, Long> outdegree = new HashMap<String, Long>();

		// foreach my $e (sort { $edge{$b} <=> $edge{$a} } keys(%edge)) {
		List<String> edgeKeys = keysByCountDescending(edge);
		for (String e : edgeKeys) {
			// my @x = split(/\001/, $e);
			String[] x = e.split(Pattern.quote("\001"));
			// $n = $edge{$e};
			long n = edge.get(e);
			//
			// # Initialize degree of kept incoming and outgoing edges if necessary
			// my $src = $x[0];
			String src = x[0];

			// my $dst = $x[1];
			String dst = x[1];

			// if (!exists($outdegree{$src})) { $outdegree{$src} = 0; }
			if (!outdegree.containsKey(src)) {
				outdegree.put(src, 0L);
			}
			// if (!exists($indegree{$dst})) { $indegree{$dst} = 0; }
			if (!indegree.containsKey(dst)) {
				indegree.put(dst, 0L);
			}
			//
			// my $keep;
			boolean keep;

			// if ($indegree{$dst} == 0) {
			if (0 == indegree.get(dst)) {
				// # Keep edge if needed for reachability
				// $keep = 1;
				keep = true;
				// } elsif (abs($n) <= $edgelimit) {
			} else if (Math.abs(n) <= edgeLimit) {
				// # Drop if we are below --edgefraction
				// $keep = 0;
				keep = false;
				// } elsif ($outdegree{$src} >= $main::opt_maxdegree ||
				// $indegree{$dst} >= $main::opt_maxdegree) {
			} else if (outdegree.get(src) >= optMaxDegree || indegree.get(dst) >= optMaxDegree) {
				// # Keep limited number of in/out edges per node
				// $keep = 0;
				keep = false;
				// } else {
			} else {
				// $keep = 1;
				keep = true;
				// }
			}
			//
			// if ($keep) {
			if (keep) {
				// $outdegree{$src}++;
				ProfilerUtils.increment(outdegree, src, 1);
				// $indegree{$dst}++;
				ProfilerUtils.increment(indegree, dst, 1);
				//
				// # Compute line width based on edge count
				// my $fraction = abs($local_total ? (3 * ($n / $local_total)) : 0);
				float fraction = (float) Math.abs((localTotal != 0) ? (3.0 * ((float) n / (float) localTotal)) : 0.0);

				// if ($fraction > 1) { $fraction = 1; }
				if (fraction > 1) {
					fraction = 1;
				}

				// my $w = $fraction * 2;
				float w = fraction * 2;
				// if ($w < 1 && ($main::opt_web || $main::opt_svg)) {
				// # SVG output treats line widths < 1 poorly.
				// $w = 1;
				// }
				//
				// # Dot sometimes segfaults if given edge weights that are too large, so
				// # we cap the weights at a large value
				// my $edgeweight = abs($n) ** 0.7;
				float edgeWeight = (float) Math.pow(Math.abs(n), 0.7);
				// if ($edgeweight > 100000) { $edgeweight = 100000; }
				if (edgeWeight > 100000) {
					edgeWeight = 100000;
				}
				// $edgeweight = int($edgeweight);
				edgeWeight = (int) edgeWeight;

				// my $style = sprintf("setlinewidth(%f)", $w);
				String style = String.format("setlinewidth(%f)", w);

				// if ($x[1] =~ m/\(inline\)/) {
				if (x[1].contains("(inline)")) {
					// $style .= ",dashed";
					style += ",dashed";
					// }
				}
				//
				// # Use a slightly squashed function of the edge count as the weight
				// printf DOT ("N%s -> N%s [label=%s, weight=%d, style=\"%s\"];\n",
				// $node{$x[0]},
				// $node{$x[1]},
				// Unparse($n),
				// $edgeweight,
				// $style);

				// # Use a slightly squashed function of the edge count as the weight
				out.printf("N%s -> N%s [label=%s, weight=%d, style=\"%s\"];\n", node.get(x[0]), node.get(x[1]),
						prettyNumber(n), (int) edgeWeight, style);
				// }
			}
			// }
		}

		// print DOT ("}\n");
		out.print("}\n");
		// close(DOT);
		out.flush(); // Caller will close

		//
		// if ($main::opt_web || $main::opt_svg) {
		// # Rewrite SVG to be more usable inside web browser.
		// RewriteSvg(TempName($main::next_tmpfile, "svg"));
		// }
		//
		// return 1;
		return true;
		// }
	}

	private List<String> translateStack(K[] kArray) {
		List<String> translated = new ArrayList<String>();
		for (K k : kArray) {
			translated.add(k.toString());
		}
		return translated;
		// Lists.newArrayList(Splitter.on('\n').split(k));
	}

	private static <K> long getEntry(Map<K, Long> counts, K key) {
		Long v = counts.get(key);
		if (v == null) {
			return 0;
		}
		return v;
	}

	private static <K> List<K> keysByCountDescending(final Map<K, Long> profile) {
		List<K> keys = new ArrayList<K>();

		Collections.sort(keys, new Comparator<K>() {
			@Override
			public int compare(K k1, K k2) {
				Long v1 = profile.get(k1);
				Long v2 = profile.get(k2);

				return v2.compareTo(v1);
			}
		});

		return keys;
	}

	// # Generate pretty-printed form of number
	// sub Unparse {
	String prettyNumber(Number number) {
		return number.toString();
		// my $num = shift;
		// if ($main::profile_type eq 'heap' || $main::profile_type eq 'growth') {
		// if ($main::opt_inuse_objects || $main::opt_alloc_objects) {
		// return sprintf("%d", $num);
		// } else {
		// if ($main::opt_show_bytes) {
		// return sprintf("%d", $num);
		// } else {
		// return sprintf("%.1f", $num / 1048576.0);
		// }
		// }
		// } elsif ($main::profile_type eq 'contention' && !$main::opt_contentions) {
		// return sprintf("%.3f", $num / 1e9); # Convert nanoseconds to seconds
		// } else {
		// return sprintf("%d", $num);
		// }
		// }
	}

	// # Generate percent string for a number and a total
	// sub Percent {
	String prettyPercent(float num, float tot) {
		// my $num = shift;
		// my $tot = shift;
		// if ($tot != 0) {
		if (tot != 0) {
			// return sprintf("%.1f%%", $num * 100.0 / $tot);
			return String.format("%.1f%%", num * 100.0 / tot);
		} else {
			// } else {
			// return ($num == 0) ? "nan" : (($num > 0) ? "+inf" : "-inf");
			return (num == 0) ? "nan" : ((num > 0) ? "+inf" : "-inf");
			// }
		}
		// }
	}

	// # Return output units
	// sub Units {
	String units() {
		// if ($main::profile_type eq 'heap' || $main::profile_type eq 'growth') {
		// if ($main::opt_inuse_objects || $main::opt_alloc_objects) {
		// return "objects";
		// } else {
		// if ($main::opt_show_bytes) {
		// return "B";
		// } else {
		// return "MB";
		// }
		// }
		// } elsif ($main::profile_type eq 'contention' && !$main::opt_contentions) {
		// return "seconds";
		// } else {
		// return "samples";
		// }
		// }
		return "samples";
	}
}
