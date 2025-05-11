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
			int hStar = (int)Math.min(y, 1l<<(1l<<t));
			int n= pts.length;
			int nGroups = (n + hStar - 1) / hStar; // round up
			// divide points into smaller groups for Graham's Scan
			Point2D[][] Sj = groups = new Point2D[nGroups][];
			for(int i=0, j=0; i<n;) {
				// at most hStar points in Sj
				Sj[j++] = Arrays.copyOfRange(pts, i, Math.min(i+hStar, n));
				i += hStar;
			}
			
			// compute convex hull for all subgroups
			Point2D[][] convSj = convGroups = new Point2D[groups.length][];
			for(int i=0; i<groups.length; i++) {
				convSj[i] = GrahamsScan.computeHull(groups[i]);
			}
			
			conv = JarvisMarchh.computeHull(convSj, hStar);
			if(conv != null || hStar >= n) {
				break;
			}
			
			// Superexponential search
			hStar = Math.min(hStar*hStar, n);
			
			// Filter out points no longer needed
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
		return (index+length) % length;
	}
	
	private static int jarvisBinarySearch(ArrayList<Point2D> pts, Point2D pi, Point2D displayBeingSpecial) {
		int length = pts.size();
		int max = length-1, min = -max;
		
		int besti = 0, altMax, altMin;
		Point2D best = pts.get(0), maybe;
		while(max-min > 0) {
			maybe = pts.get(circleAccess(length, besti-1));
			if(besti > min && Maths.isLeftOf(pi, maybe, best)) {
				altMax = max;
				max = besti-1;
				besti = (min+max)>>1;
				best = pts.get(circleAccess(length, besti));
				
				if(Maths.isLeftOf(pi, maybe, best)) {
					besti = max;
					best = maybe;
					max = altMax;
				}
			} else {
				maybe = pts.get(circleAccess(length, besti+1));
				if(besti < max && Maths.isLeftOf(pi, maybe, best)) {
					altMin = min;
					min = besti+1;
					besti = (min+max)>>1;
					best = pts.get(circleAccess(length, besti));
					
					if(Maths.isLeftOf(pi, maybe, best)) {
						besti = min;
						best = maybe;
						min = altMin;
					}
				} else {
					return circleAccess(length, besti);
				}
			}
		}
		return circleAccess(length, besti);
	}

	public static Point2D[] computeHull(Point2D[][] hulls, int hStar) {
		// only one subhull
		if(hulls.length == 1) return hulls[0];
		
		ArrayList<ArrayList<Point2D>> S2 = new ArrayList<>();
		
		// find lowest, leftmost point
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
						g2.add(pt);
						pt.setAngle(Math.atan2(pt.getX()-minPt.getX(), pt.getY()-minPt.getY()));
						pt.setGroup(k);
					}
				}
				
				Point2D first = g2.get(0);
				if(group.length > 1 && first.getX() < minPt.getX()) {
					g2.remove(0);
					g2.add(first);
				}
				
				k++;
			}
		}
		
		ArrayList<Point2D> conv = new ArrayList<>(hStar);
		conv.add(minPt);
		
		do {
			Point2D pt1 = conv.get(conv.size()-1);
			Point2D nextCandidate = conv.size() == 1 ? null : minPt;
			
			ArrayList<Point2D> bpts = null;
			int bptsi = 0;
			
			// find best candidate in subhulls
			for(ArrayList<Point2D> pts : S2) {
				if(pts.size() > 0) {
					int pti = jarvisBinarySearch(pts, pt1, nextCandidate == null || nextCandidate == minPt ? pt1 : nextCandidate);
					Point2D pt = pts.get(pti);
					
					if(nextCandidate == null || Maths.isLeftOf(pt1, pt, nextCandidate)) {
						nextCandidate = pt;
						bpts = pts;
						bptsi = pti;
					}
				}
			}
			
			// check for completion
			if(nextCandidate == null || nextCandidate == minPt) {
				return conv.toArray(new Point2D[0]);
			}
			
			// add point to hull
			bpts.remove(bptsi);
			conv.add(nextCandidate);
			
		} while(conv.size() <= hStar);
		
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
		
		// remove minPt
		ArrayList<Point2D> andere = new ArrayList<>(pts.length-1);
		for(int i=0; i<pts.length; i++) {
			if(i != minIndex) {
				andere.add(pts[i]);
			}
		}
		
		// calculate angle to minPt and x-axis
		for(Point2D pt : andere) {
			pt.setAngle(Math.atan2(pt.getY()-minPt.getY(), pt.getX()-minPt.getX()));
			pt.setStillNeeded(false);
		}
		
		// sort points by angle
		Collections.sort(andere);
		
		// remove collinear points
		for(int i=1, l=andere.size(); i<l; i++) {
			Point2D a = andere.get(i-1);
			Point2D b = andere.get(i);
			if(a.getAngle() == b.getAngle()) {
				// remove point closer to p0
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
		conv.add(minPt);
		conv.add(andere.get(0));
		
		for(int i=1, l=andere.size(); i<l;) {
			int stackLength = conv.size();
			Point2D pt1 = conv.get(stackLength - 1);
			Point2D pt2 = conv.get(stackLength - 2);
			Point2D compare = andere.get(i);

			if (stackLength == 2 || Maths.isLeftOf(pt2, pt1, compare)) {
				conv.add(compare);
				i++;
			} else {
				conv.remove(stackLength - 1);
			}
		}
		
		
		Point2D[] ret = conv.toArray(new Point2D[0]);
		for(Point2D pt : ret) {
			pt.setStillNeeded(true);
		}
		
		return ret;
	}
}