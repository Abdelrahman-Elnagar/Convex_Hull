package com.myapp.hull.algorithms;

import java.util.*;
import com.myapp.hull.Point2D;



public class Chans implements ConvexHullAlgorithm {

    static final int RIGHT_TURN = -1;
    static final int LEFT_TURN = 1;
    static final int COLLINEAR = 0;

    static Point2D p0 = new Point2D(0.0, 0.0);

    static double dist(Point2D p1, Point2D p2) {
        return (p1.getX() - p2.getX()) * (p1.getX() - p2.getX()) + 
               (p1.getY() - p2.getY()) * (p1.getY() - p2.getY());
    }

    static int orientation(Point2D p, Point2D q, Point2D r) {
        double val = (q.getY() - p.getY()) * (r.getX() - q.getX()) - 
                    (q.getX() - p.getX()) * (r.getY() - q.getY());
        if (val == 0) return COLLINEAR;
        return (val > 0) ? RIGHT_TURN : LEFT_TURN;
    }

    static Comparator<Point2D> polarOrder = (p1, p2) -> {
        int orient = orientation(p0, p1, p2);
        if (orient == 0)
            return (dist(p0, p2) >= dist(p0, p1)) ? -1 : 1;
        return (orient == LEFT_TURN) ? -1 : 1;
    };

    static int tangent(List<Point2D> v, Point2D p) {
        int n = v.size();
        if (n == 1) return 0;
        if (n == 2)
            return (orientation(p, v.get(0), v.get(1)) != RIGHT_TURN) ? 0 : 1;

        int l = 0, r = n;
        while (l < r) {
            int c = (l + r) / 2;
            int c_prev = (c - 1 + n) % n;
            int c_next = (c + 1) % n;

            Point2D pc = v.get(c);
            Point2D pprev = v.get(c_prev);
            Point2D pnext = v.get(c_next);

            int o1 = orientation(p, pc, pprev);
            int o2 = orientation(p, pc, pnext);

            if (o1 != RIGHT_TURN && o2 != RIGHT_TURN)
                return c;

            int o = orientation(p, v.get(l), pc);
            if (o == RIGHT_TURN && (o1 == RIGHT_TURN || orientation(p, v.get(l), v.get((l + 1) % n)) == o))
                r = c;
            else
                l = c + 1;
        }
        return l % n;
    }

    static int[] extremeHullPoint(List<List<Point2D>> hulls) {
        int h = 0, p = 0;
        for (int i = 0; i < hulls.size(); i++) {
            for (int j = 0; j < hulls.get(i).size(); j++) {
                Point2D curr = hulls.get(i).get(j);
                Point2D best = hulls.get(h).get(p);
                if (curr.getY() < best.getY() || (curr.getY() == best.getY() && curr.getX() < best.getX())) {
                    h = i;
                    p = j;
                }
            }
        }
        return new int[]{h, p};
    }

    static int[] nextHullPoint(List<List<Point2D>> hulls, int[] lpoint) {
        Point2D p = hulls.get(lpoint[0]).get(lpoint[1]);
        int[] next = new int[]{lpoint[0], (lpoint[1] + 1) % hulls.get(lpoint[0]).size()};

        for (int h = 0; h < hulls.size(); h++) {
            if (h != lpoint[0]) {
                int s = tangent(hulls.get(h), p);
                Point2D q = hulls.get(next[0]).get(next[1]);
                Point2D r = hulls.get(h).get(s);
                int t = orientation(p, q, r);
                if (t == RIGHT_TURN || (t == COLLINEAR && dist(p, r) > dist(p, q))) {
                    next = new int[]{h, s};
                }
            }
        }
        return next;
    }

    static List<Point2D> keepLeft(List<Point2D> v, Point2D p) {
        while (v.size() > 1 && orientation(v.get(v.size() - 2), v.get(v.size() - 1), p) != LEFT_TURN)
            v.remove(v.size() - 1);
        if (v.isEmpty() || !v.get(v.size() - 1).equals(p))
            v.add(p);
        return v;
    }

    static List<Point2D> grahamScan(List<Point2D> points) {
        if (points.size() <= 1) return new ArrayList<>(points);

        points.sort((p1, p2) -> {
            if (p1.getY() != p2.getY()) return Double.compare(p1.getY(), p2.getY());
            return Double.compare(p1.getX(), p2.getX());
        });
        p0 = points.get(0);
        points.sort(polarOrder);

        List<Point2D> lower = new ArrayList<>();
        for (Point2D p : points) lower = keepLeft(lower, p);

        Collections.reverse(points);
        List<Point2D> upper = new ArrayList<>();
        for (Point2D p : points) upper = keepLeft(upper, p);

        upper.remove(0);  // avoid duplication
        lower.addAll(upper);
        if (!lower.isEmpty() && lower.get(lower.size() - 1).equals(lower.get(0)) && lower.size() > 1)
            lower.remove(lower.size() - 1); // remove duplicate last point
        return lower;
    }

    @Override
    public List<Point2D> computeHull(List<Point2D> points) {        
        int n = points.size();
        if (n <= 3) return grahamScan(points);

        for (int t = 1; ; t++) {
            int m = (int)Math.min(n+0l, 1l << (1 << t));
            List<List<Point2D>> hulls = new ArrayList<>();

            for (int i = 0; i < n; i += m) {
                List<Point2D> chunk = points.subList(i, Math.min(i + m, n));
                hulls.add(grahamScan(new ArrayList<>(chunk)));
            }

            List<int[]> hull = new ArrayList<>();
            hull.add(extremeHullPoint(hulls));

            for (int i = 0; i < m; ++i) {
                int[] p = nextHullPoint(hulls, hull.get(hull.size() - 1));
                if (Arrays.equals(p, hull.get(0))) {
                    List<Point2D> result = new ArrayList<>();
                    for (int[] pt : hull)
                        result.add(hulls.get(pt[0]).get(pt[1]));
                    return result;
                }
                hull.add(p);
            }
        }
    }

    public static void main(String[] args) {
        List<Point2D> points = Arrays.asList(
            new Point2D(10.0, 0.0), new Point2D(7.0, 7.0), new Point2D(0.0, 10.0), new Point2D(-7.0, 7.0),
            new Point2D(-10.0, 0.0), new Point2D(-7.0, -7.0), new Point2D(0.0, -10.0), new Point2D(7.0, -7.0),
            new Point2D(9.0, 4.0), new Point2D(4.0, 9.0), new Point2D(-9.0, 4.0), new Point2D(-4.0, -9.0),
            new Point2D(0.0, 0.0), new Point2D(1.0, 1.0), new Point2D(-2.0, 3.0), new Point2D(4.0, -2.0),
            new Point2D(-3.0, -1.0), new Point2D(5.0, 0.0), new Point2D(-5.0, 0.0), new Point2D(0.0, 5.0),
            new Point2D(0.0, -5.0), new Point2D(-2.0, 2.0), new Point2D(-4.0, 7.0), new Point2D(2.0, -6.0),
            new Point2D(-2.0, 7.0), new Point2D(3.0, 7.0), new Point2D(7.0, 1.0), new Point2D(-10.0, -10.0),
            new Point2D(-2.0, 7.0), new Point2D(-10.0, 2.0), new Point2D(-3.0, -8.0)
        );
    }
}


