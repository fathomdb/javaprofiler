package com.fathomdb.profiler.tests;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Test1 {
	public static void main(String[] args) {
		long n = 0;

		List<String> l = new ArrayList<String>();
		for (int i = 0; i < 100; i++) {
			long total = addToList(l);
			n += total;
			System.out.print(".");
			if (0 == ((i + 1) % 10)) {
				System.out.println();
			}
			l.clear();
		}

		System.out.println("Total chars: " + n);
	}

	static long addToList(List<String> target) {
		long n = 0;
		for (int i = 0; i < 20000; i++) {
			String s = buildString(i);
			target.add(s);

			n += s.length();
		}
		return n;
	}

	static String buildString(int i) {
		int k = i % 4;

		if (k == 0) {
			return buildString0(i);
		}

		if (k == 1) {
			return buildString1(i);
		}

		if (k == 2) {
			return buildString2(i);
		}

		return buildString3(i);
	}

	private static String buildString3(int i) {
		return new Integer(i).hashCode() + "::" + i;
	}

	private static String buildString2(int i) {
		BigInteger bi = BigInteger.valueOf(i);
		bi = bi.multiply(bi);
		bi = bi.multiply(bi);
		return bi.toString();
	}

	private static String buildString1(int i) {
		Random r = new Random();
		return r.nextGaussian() + "::" + i;
	}

	private static String buildString0(int i) {
		return System.currentTimeMillis() + "::" + i;
	}
}
