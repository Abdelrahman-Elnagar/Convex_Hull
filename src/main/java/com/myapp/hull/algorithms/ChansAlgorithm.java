package com.myapp.hull.algorithms;

import com.myapp.hull.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.myapp.hull.Maths;

public class ChansAlgorithm implements ConvexHullAlgorithm {
	
	private static Point2D[] allPts, pts;
	private static Point2D[][] groups, convGroups;
	private static ArrayList<Point2D> currentConv = new ArrayList<>();

	@Override
	public List<Point2D> computeHull(List<Point2D> points) {
		return Arrays.asList(computeHull(points.toArray(new Point2D[0]), 0));
	}

    

    

	

	public static Point2D[] computeHull(Point2D[] points) {
		return computeHull(points, 0);
	}

	public static Point2D[] computeHull(Point2D[] points, int t) {
		currentConv.clear();
		
		Point2D[] pts = ChansAlgorithm.allPts = ChansAlgorithm.pts = points;
		Point2D[] conv;
		
		convGroups = null;
		int y= points.length;
		while(true) {
			t++;
			// 2^2^i
			// Superexponential search
			// if m is bigger than our number of points, then go for y
			int m = (int)Math.min(y, 1l<<(1l<<t));
			int n= pts.length;
			int nGroups = (n + m - 1) / m; // round up
			// divide points into smaller groups for Graham's Scan
			Point2D[][] Sj = groups = new Point2D[nGroups][];
			for(int i=0, j=0; i<n;) {
				// at most m points in Sj
				Sj[j++] = Arrays.copyOfRange(pts, i, Math.min(i+m, n));
				i += m;
			}
			
			// compute convex hull for all subgroups using graham
			Point2D[][] convSj = convGroups = new Point2D[groups.length][];
			for(int i=0; i<groups.length; i++) {
				convSj[i] = GrahamsScan.computeHull(groups[i]);
			}
			
			// compute the jarvis convex using the subgroup hulls
			conv = JarvisMarchh.computeHull(convSj, m);
			if(conv != null || m >= n) {
				break;
			}
			
			
			// Filter out points no longer needed
			// if a point is not on the hull on one of the subgroups
			// then it's not definitely not a part of the bigger convex hull
			// similar to the idea of Akl–Toussaint heuristic
			int neededSpace = 0;
			for(Point2D pt: pts) {
				if(pt.isStillNeeded()) neededSpace++;
			}
			
			Point2D[] newPts = new Point2D[neededSpace];
			for(Point2D pt: pts) {
				if(pt.isStillNeeded()) {
					newPts[--neededSpace] = pt;
				}
			}
			
			pts = ChansAlgorithm.pts = newPts;
		}
		
		return conv;
	}

	public static List<List<Point2D>> getPartialHulls(Point2D[] points) {
		// Run computeHull to get the last computed group of hulls
		computeHull(points);
		// Convert the 2D array to a list of lists
		List<List<Point2D>> result = new ArrayList<>();
		if (convGroups != null) {
			for (Point2D[] hull : convGroups) {
				if (hull != null) {
					result.add(Arrays.asList(hull));
				}
			}
		}
		return result;
	}

	public static List<List<Point2D>> getPartialHulls(List<Point2D> points) {
		return getPartialHulls(points.toArray(new Point2D[0]));
	}
}

class JarvisMarchh {
	
	private static int circleAccess(int length, int index) {
		// used for binary search since array is circular but sorted
		return (index+length) % length;
	}
	
	private static int jarvisBinarySearch(ArrayList<Point2D> pts, Point2D pi, Point2D displayBeingSpecial) {
		int length = pts.size();
		int max = length-1, min = -max; // Indices can wrap around negatively
		
		int besti = 0, altMax, altMin; // Current best candidate index
		Point2D best = pts.get(0), maybe; // Current best candidate point

		//APPROACH
		// Look at the current best index (besti)
		// Compare it to its left and right neighbors using isLeftOf
		// If the neighbor is better, we narrow the search toward that direction
		// If no neighbor is better, we've found our best candidate and return it.
		while(max-min > 0) {
			// Check the left neighbor of `besti`
			maybe = pts.get(circleAccess(length, besti-1));
			if(besti > min && Maths.isLeftOf(pi, maybe, best)) {
				// `maybe` is better than `best` → search left half
				// because best is on the left of maybe, so from pi to maybe is better than pi to best
				altMax = max;
				max = besti-1; // left space is narrowed
				besti = (min+max)>>1; // divide by 2
				best = pts.get(circleAccess(length, besti)); // update best to our midpoint
				
				if(Maths.isLeftOf(pi, maybe, best)) { // if maybe is better than midpoint
					// `maybe` is still better → keep it
					besti = max;
					best = maybe;
					max = altMax;
				}
			} else {
				// Check the right neighbor of `besti`
				maybe = pts.get(circleAccess(length, besti+1));
				if(besti < max && Maths.isLeftOf(pi, maybe, best)) {
					// `maybe` is better than `best` → search right half
					altMin = min;
					min = besti+1;
					besti = (min+max)>>1;
					best = pts.get(circleAccess(length, besti));
					
					if(Maths.isLeftOf(pi, maybe, best)) {
						// `maybe` is still better → keep it
						besti = min;
						best = maybe;
						min = altMin;
					}
				} else {
					// Found the best candidate
					return circleAccess(length, besti);
				}
			}
		}
		return circleAccess(length, besti);
	}

	public static Point2D[] computeHull(Point2D[][] hulls, int m) {
		// only one subhull
		// happens when nr of points is small so graham only outputs one hull
		if(hulls.length == 1) return hulls[0];
		
		ArrayList<ArrayList<Point2D>> S2 = new ArrayList<>();
		
		// find lowest, leftmost point
		// minPt will be the starting point for building the hull
		Point2D minPt = hulls[0][0];
		for(int i=0; i<hulls.length; i++) {
			Point2D pt = hulls[i][0];
			if(pt.getY() < minPt.getY() || (pt.getY() == minPt.getY() && pt.getX() < minPt.getX())) {
				minPt = pt;
			}
		}
		
		int k = 0;
		for(Point2D[] group: hulls) {
			if(group.length > 0 && (group.length > 1 || group[0] != minPt)) {
				ArrayList<Point2D> g2 = new ArrayList<>(group.length);
				S2.add(g2);
				
				for(Point2D pt: group) {
					if(pt != minPt) {
						// Copy it to a new list g2, excluding minPt
						// Compute angle of each point relative to minPt
						g2.add(pt);
						pt.setAngle(Math.atan2(pt.getX()-minPt.getX(), pt.getY()-minPt.getY()));
						pt.setGroup(k);
					}
				}
				// rotate array to start from right
				// Is the first point’s X-coordinate less than minPt.getX()?
				// If yes, it assumes the first point is “early” in angle order 
				// and rotates by 1: removes the first point and pushes it to the end.
				Point2D first = g2.get(0);
				if(group.length > 1 && first.getX() < minPt.getX()) {
					g2.remove(0);
					g2.add(first);
				}
				
				k++;
			}
		}
		// Preallocates capacity m — an upper bound on the number of points in the convex hull.
		ArrayList<Point2D> conv = new ArrayList<>(m);
		conv.add(minPt);
		
		do {
			// This is the current point on the hull, from which we want to find the next point.
			Point2D pt1 = conv.get(conv.size()-1);
			// If this is the first step, nextCandidate is null. 
			// Otherwise, it's set to minPt, so we can compare other points against it.
			Point2D nextCandidate = conv.size() == 1 ? null : minPt;
			
			ArrayList<Point2D> bpts = null;
			int bptsi = 0;
			
			// find best candidate in subhulls
			// APPROACH
			// It goes through every subhull (each group).
			// It finds the best candidate point in that subhull to go to next from pt1, using a binary search.
			// It compares the result from each subhull and keeps the most counterclockwise one — using Maths.isLeftOf(pt1, pt, nextCandidate).
			// instead of checking every point (like Jarvis), we do binary search on each subhull.
			for(ArrayList<Point2D> pts : S2) {
				if(pts.size() > 0) {
					int pti = jarvisBinarySearch(pts, pt1, nextCandidate == null || nextCandidate == minPt ? pt1 : nextCandidate);
					Point2D pt = pts.get(pti);
					
					// find best point out of all subgroups
					if(nextCandidate == null || Maths.isLeftOf(pt1, pt, nextCandidate)) {
						nextCandidate = pt;
						bpts = pts;
						bptsi = pti;
					}
				}
			}
			
			// check for completion
			// If we’ve wrapped around to the start (minPt), we are done. 
			// Convert conv to an array and return the full hull.
			if(nextCandidate == null || nextCandidate == minPt) {
				return conv.toArray(new Point2D[0]);
			}
			
			// add point to hull
			// We remove the chosen point from its subhull (so we don’t revisit it) and add it to the convex hull.
			bpts.remove(bptsi);
			conv.add(nextCandidate);
			
		} while(conv.size() <= m);
		// If m (the max steps) is exceeded before completing the hull 
		// (i.e., we didn't get back to minPt), then we return null. -> try larger m
		
		return null;
	}
}

class GrahamsScan {
	
	public static Point2D[] computeHull(Point2D[] pts) {
		if(pts.length < 4) {
			for(Point2D pt : pts) {
				pt.setStillNeeded(true);
			}
			return pts;
		}
		// this is the algo's assumption
		
		ArrayList<Point2D> conv = new ArrayList<>(pts.length);
		
		// find point with lowest y-coordinate
		// if multiple, take leftmost
		double minY = pts[0].getY();
		Point2D minPt = pts[0];
		int minIndex = 0;
		for(int i=1; i<pts.length; i++) {
			Point2D pt = pts[i];
			if(pt.getY() < minY) {
				minPt = pt;
				minY = pt.getY();
				minIndex = i;
			} else if(pt.getY() == minY && pt.getX() < minPt.getX()) {
				minPt = pt;
				minIndex = i;
			}
		}
		
		// remove minPt (already guaranteed on convex hull)
		ArrayList<Point2D> andere = new ArrayList<>(pts.length-1);
		for(int i=0; i<pts.length; i++) {
			if(i != minIndex) {
				andere.add(pts[i]);
			}
		}
		
		// calculate angle to minPt and x-axis, does it using tan
		for(Point2D pt : andere) {
			pt.setAngle(Math.atan2(pt.getY()-minPt.getY(), pt.getX()-minPt.getX()));
			pt.setStillNeeded(false);
		}
		
		// sort points by angle m log m since size is m
		Collections.sort(andere);
		
		// remove collinear points
		for(int i=1, l=andere.size(); i<l; i++) {
			Point2D a = andere.get(i-1);
			Point2D b = andere.get(i);
			if(a.getAngle() == b.getAngle()) {
				// remove point closer to p0 (keep the one further apart)
				if(Maths.squaredDistance(a.getX()-minPt.getX(), a.getY()-minPt.getY()) > 
				   Maths.squaredDistance(b.getX()-minPt.getX(), b.getY()-minPt.getY())) {
					// keep a
					andere.remove(i);
					l=andere.size();
				} else {
					// keep b
					andere.remove(i-1);
					l=andere.size();
				}
				i--;
			}
		}
		
		// stack of hull points
		conv.add(minPt); //1st point
		conv.add(andere.get(0)); //2nd point
		
		for(int i=1, l=andere.size(); i<l;) {
			int stackLength = conv.size();
			Point2D pt1 = conv.get(stackLength - 1);
			Point2D pt2 = conv.get(stackLength - 2);
			Point2D compare = andere.get(i); //compare third

			// if it makes a CCW turn, add it, else remove the old 2nd point
			if (stackLength == 2 || Maths.isLeftOf(pt2, pt1, compare)) {
				conv.add(compare);
				i++;
			} else {
				conv.remove(stackLength - 1);
			}
		}
		
		
		Point2D[] ret = conv.toArray(new Point2D[0]);
		for(Point2D pt : ret) {
			pt.setStillNeeded(true); //mark as needed to remove rest
		}
		
		return ret;
	}
}