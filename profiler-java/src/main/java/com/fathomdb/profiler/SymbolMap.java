package com.fathomdb.profiler;

import java.util.List;

public interface SymbolMap {

	List<String> translateStack(long[] k, boolean lineNumber);

}
