/**
 * Copyright (c) 2013 Puppet Labs, Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Puppet Labs
 */
package com.puppetlabs.geppetto.common.stats;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;

/**
 * <p>
 * IntegerCluster utility that implements a simplistic hierarchical clustering algorithm.
 * </p>
 * <p>
 * This class solves the problem; given a series of integers find the smallest number of clusters (of a given min-max
 * inclusive range of values) such that no range is wider than a given max.
 * </p>
 * <p>
 * If all values are within the given max, one cluster is produces, and if no two values are closer than max, then there
 * will be as many clusters as there are unique values in the observed set of values.
 * </p>
 * <p>
 * The algorithm orders the set of observed values into an set of clusters of 0 size (min = max = value), and searches
 * for the two adjacent clusters that produce the smallest resulting cluster if merged. If the smallest available merge
 * is bigger than the max, the work is done. If the range is smaller than the max, the merged cluster replaces the two
 * inputs. The algorithm now loops back to check for the next two adjacent clusters with the smallest distance.
 * </p>
 * <p>
 * The implementation is primarily intended for a fairly small number of observations/clusters as the final step of
 * mapping observations to clusters search (binary search) for a cluster per value. To use this class with larger data
 * sets, it would be better to keep a map from observations to clusters.
 * </p>
 */
public class IntegerCluster {
	private static class ClusterNode {
		int min;

		int max;

		private ClusterNode(ClusterNode a, ClusterNode b) {
			this.min = Math.min(a.min(), b.min());
			this.max = Math.max(a.max(), b.max());
		}

		private ClusterNode(int value) {
			this.min = this.max = value;
		}

		private ClusterNode(int min, int max) {
			this.min = Math.min(min, max);
			this.max = Math.max(min, max);
		}

		public int max() {
			return this.max;
		}

		public int min() {
			return this.min;
		}

	}

	/**
	 * Compares nodes; the node that starts first is smaller. If starting on the same value, the node that ends first is
	 * smaller.
	 */
	private static class ClusterNodeComparator implements Comparator<ClusterNode> {

		@Override
		public int compare(ClusterNode o1, ClusterNode o2) {
			if(o1.min() == o2.min() && o1.max() == o2.max())
				return 0;
			if(o1.min() < o2.min())
				return -1;
			if(o1.max() < o2.max())
				return -1;
			return 1;
		}

	}

	/**
	 * Calls Ranges.closed() or Range.closed depending on which Guava version
	 * we're using.
	 *
	 * @param min
	 * @param max
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <C extends Comparable<?>> Range<C> closed(C min, C max) {
		try {
			return (Range<C>) RNG_CLOSED.invoke(null, min, max);
		}
		catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		catch(IllegalArgumentException e) {
			throw e;
		}
		catch(InvocationTargetException e) {
			if(e.getTargetException() instanceof RuntimeException)
				throw ((RuntimeException) e.getTargetException());
			throw new RuntimeException(e.getTargetException());
		}
	}

	private final static ClusterNodeComparator comparator = new ClusterNodeComparator();

	private List<ClusterNode> clusterList = Lists.newArrayList();

	private boolean dirty;

	private final int maxDistance;

	private static final Method RNG_CLOSED;

	static {
		// Guava moved static methods on Ranges into the Range class for some reason. We resolve
		// this here by using introspection
		Method rngClosed;
		try {
			rngClosed = Range.class.getDeclaredMethod("closed", Comparable.class, Comparable.class);
		}
		catch(Exception e) {
			try {
				rngClosed = Class.forName("com.google.common.collect.Ranges").getDeclaredMethod(
					"closed", Comparable.class, Comparable.class);
			}
			catch(Exception e2) {
				throw new ExceptionInInitializerError(e);
			}
		}
		RNG_CLOSED = rngClosed;
	}

	public IntegerCluster(int maxDistance) {
		this.maxDistance = maxDistance;
	}

	public void add(int observation) {
		clusterList.add(new ClusterNode(observation));
		dirty = true;
	}

	public void addAll(Iterable<Integer> iterable) {
		for(Integer i : iterable)
			add(i);
	}

	private void cluster() {
		Collections.sort(clusterList, comparator);

		while(clusterList.size() > 1) {
			int limit = clusterList.size() - 1;
			int mind = Integer.MAX_VALUE;
			int minix = -1;
			for(int i = 0; i < limit; i++) {
				int d = distance(clusterList.get(i), clusterList.get(i + 1));
				if(d < mind) {
					mind = d;
					minix = i;
				}
			}
			// the two smallest causes a range that is bigger than max allowed
			if(mind > maxDistance)
				return;
			// join the two clusters closest to each other.
			clusterList.set(minix, new ClusterNode(clusterList.get(minix), clusterList.get(minix + 1)));
			clusterList.remove(minix + 1);
		}
		dirty = false;
	}

	private ClusterNode clusterForValue(int x) {
		lazyCluster();
		int pos = Collections.binarySearch(clusterList, new ClusterNode(x), comparator);
		if(pos >= 0) {
			return clusterList.get(pos);
		}

		// abs(pos) is the index of the first element > x (or the size if last).
		pos = -pos - 1;
		if(pos == clusterList.size()) {
			ClusterNode result = clusterList.get(pos - 1);
			// outch, the value is > the largest cluster - should not happen if used correctly
			if(result.max() < x)
				throw new IllegalStateException("The given value was not included in the set of observed values: " + x);
			return result;
		}
		// pos is the insertion point, but needs adjustment if x > min of its cluster since clusters are pimarily ordered on min value
		ClusterNode result = clusterList.get(pos);
		if(result.min() > x)
			return clusterList.get(pos - 1);
		return result;

	}

	/**
	 * Returns the max value for the cluster x is a member of.
	 *
	 * @param x
	 * @return
	 */
	public int clusterMax(int x) {
		return clusterForValue(x).max();
	}

	/**
	 * Returns the min value for the cluster x is a member of.
	 *
	 * @param x
	 * @return
	 */
	public int clusterMin(int x) {
		return clusterForValue(x).min();
	}

	private int distance(ClusterNode a, ClusterNode b) {
		int low = Math.min(a.min(), b.min());
		int high = Math.max(a.max(), b.max());
		return high - low;
	}

	/**
	 * Return the number of clusters.
	 *
	 * @return
	 */
	public int getClusterCount() {
		lazyCluster();
		return clusterList.size();
	}

	private void lazyCluster() {
		if(dirty)
			cluster();
	}

	public List<Range<Integer>> toListOfRanges() {
		lazyCluster();
		List<Range<Integer>> result = Lists.newArrayListWithExpectedSize(clusterList.size());
		for(ClusterNode n : clusterList) {
			result.add(closed(n.min(), n.max()));
		}
		return result;
	}
}
