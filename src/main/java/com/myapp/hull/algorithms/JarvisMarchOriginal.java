
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class JarvisMarchOriginal {

    static class Point{
        int x;
        int y;
        Point(int x, int y){
            this.x = x;
            this.y = y;
        }
        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }

    public List<Point> findConvexHull(Point[] points) {
        //first find leftmost point to start the march.
        Point start = points[0];
        for (int i = 1; i < points.length; i++) {
            if (points[i].x < start.x) {
                start = points[i];
            }
        }
        Point current = start;
        //use set because this algorithm might try to insert duplicate point.
        Set<Point> result = new HashSet<>();
        result.add(start);
        List<Point> collinearPoints = new ArrayList<>();
        while (true) {
            Point nextTarget = points[0];
            for (int i = 1; i < points.length; i++) {
                if (points[i] == current) {
                    continue;
                }
                int val = crossProduct(current, nextTarget, points[i]);
                //if val > 0 it means points[i] is on left of current -> nextTarget. Make him the nextTarget.
                if (val > 0) {
                    nextTarget = points[i];
                    //reset collinear points because we now have a new nextTarget.
                    collinearPoints = new ArrayList<>();
                } else if (val == 0) { //if val is 0 then collinear current, nextTarget and points[i] are collinear.
                    //if its collinear point then pick the further one but add closer one to list of collinear points.
                    if (distance(current, nextTarget, points[i]) < 0) {
                        collinearPoints.add(nextTarget);
                        nextTarget = points[i];
                    } else { //just add points[i] to collinearPoints list. If nextTarget indeed is the next point on
                        //convex then all points in collinear points will be also on boundary.
                        collinearPoints.add(points[i]);
                    }
                }
                //else if val < 0 then nothing to do since points[i] is on right side of current -> nextTarget.
            }

            //add all points in collinearPoints to result.
            for (Point p : collinearPoints) {
                result.add(p);
            }
            //if nextTarget is same as start it means we have formed an envelope and its done.
            if (nextTarget == start) {
                break;
            }
            //add nextTarget to result and set current to nextTarget.
            result.add(nextTarget);
            current = nextTarget;
        }
        return new ArrayList<>(result);
    }

    /**
     * Returns < 0 if 'b' is closer to 'a' compared to 'c', == 0 if 'b' and 'c' are same distance from 'a'
     * or > 0 if 'c' is closer to 'a' compared to 'b'.
     */
    private int distance(Point a, Point b, Point c) {
        int y1 = a.y - b.y;
        int y2 = a.y - c.y;
        int x1 = a.x - b.x;
        int x2 = a.x - c.x;
        return Integer.compare(y1 * y1 + x1 * x1, y2 * y2 + x2 * x2);
    }

    /**
     * Cross product to find where c belongs in reference to vector ab.
     * If result > 0 it means 'c' is on left of ab
     *    result == 0 it means 'a','b' and 'c' are collinear
     *    result < 0  it means 'c' is on right of ab
     */
    private int crossProduct(Point a, Point b, Point c) {
        int y1 = a.y - b.y;
        int y2 = a.y - c.y;
        int x1 = a.x - b.x;
        int x2 = a.x - c.x;
        return y2 * x1 - y1 * x2;
    }

    
    // Utility method to create points from coordinate pairs
    public static Point[] createPoints(int[][] coords) {
        Point[] points = new Point[coords.length];
        for (int i = 0; i < coords.length; i++) {
            points[i] = new Point(coords[i][0], coords[i][1]);
        }
        return points;
    }

    public static void main(String[] args) {
       //for testing purposes:
       JarvisMarchOriginal jarvis = new JarvisMarchOriginal();
    
       // Test Case 1: All collinear points
       System.out.println("Test Case 1 - Collinear:");
       int[][] input1 = {{0,0}, {1,1}, {2,2}, {3,3}};
       Point[] points1 = createPoints(input1);
       System.out.println("Expected: [(0,0), (3,3)]");
       System.out.println("Actual: " + jarvis.findConvexHull(points1));
       
       // Test Case 2: Square with interior points
       System.out.println("\nTest Case 2 - Square with interior:");
       int[][] input2 = {{0,0}, {0,3}, {3,3}, {3,0}, {1,1}, {2,2}};
       Point[] points2 = createPoints(input2);
       System.out.println("Expected: [(0,0), (0,3), (3,3), (3,0)]");
       System.out.println("Actual: " + jarvis.findConvexHull(points2));
       
       // Test Case 3: Triangle with collinear edge
       System.out.println("\nTest Case 3 - Triangle with collinear:");
       int[][] input3 = {{0,0}, {2,0}, {1,1}, {1,0}};
       Point[] points3 = createPoints(input3);
       System.out.println("Expected: [(0,0), (2,0), (1,1)]");
       System.out.println("Actual: " + jarvis.findConvexHull(points3));
       
       // Test Case 4: Single point
       System.out.println("\nTest Case 4 - Single point:");
       int[][] input4 = {{5,5}};
       Point[] points4 = createPoints(input4);
       System.out.println("Expected: [(5,5)]");
       System.out.println("Actual: " + jarvis.findConvexHull(points4));
       
       // Test Case 5: Two points
       System.out.println("\nTest Case 5 - Two points:");
       int[][] input5 = {{1,2}, {3,4}};
       Point[] points5 = createPoints(input5);
       System.out.println("Expected: [(1,2), (3,4)]");
       System.out.println("Actual: " + jarvis.findConvexHull(points5));
       
       // Test Case 6: Random non-convex shape
       System.out.println("\nTest Case 6 - Random shape:");
       int[][] input6 = {{0,0}, {1,3}, {2,2}, {4,4}, {0,5}, {3,1}, {5,0}};
       Point[] points6 = createPoints(input6);
       System.out.println("Expected: [(0,0), (0,5), (4,4), (5,0)]");
       System.out.println("Actual: " + jarvis.findConvexHull(points6));
       
       // Test Case 7: Vertical line
       System.out.println("\nTest Case 7 - Vertical line:");
       int[][] input7 = {{2,0}, {2,1}, {2,3}, {2,2}};
       Point[] points7 = createPoints(input7);
       System.out.println("Expected: [(2,0), (2,3)]");
       System.out.println("Actual: " + jarvis.findConvexHull(points7));
       
       // Test Case 8: Horizontal line
       System.out.println("\nTest Case 8 - Horizontal line:");
       int[][] input8 = {{1,5}, {3,5}, {2,5}, {4,5}};
       Point[] points8 = createPoints(input8);
       System.out.println("Expected: [(1,5), (4,5)]");
       System.out.println("Actual: " + jarvis.findConvexHull(points8));
       
       // Test Case 9: Pentagon
       System.out.println("\nTest Case 9 - Pentagon:");
       int[][] input9 = {{0,0}, {2,0}, {3,2}, {1,4}, {-1,2}};
       Point[] points9 = createPoints(input9);
       System.out.println("Expected: [(0,0), (2,0), (3,2), (1,4), (-1,2)]");
       System.out.println("Actual: " + jarvis.findConvexHull(points9));
       
       // Test Case 10: Duplicate points
       System.out.println("\nTest Case 10 - Duplicates:");
       int[][] input10 = {{1,1}, {1,1}, {2,2}, {1,1}};
       Point[] points10 = createPoints(input10);
       System.out.println("Expected: [(1,1), (2,2)]");
       System.out.println("Actual: " + jarvis.findConvexHull(points10));
   
       // Test Case 11: Large test case with mixed scenarios
       System.out.println("Large Test Case - Mixed Points:");
       int[][] input = {
           {0, 0},    // Should be in hull (bottom-left corner)
           {2, 0},    // Should be in hull (bottom edge)
           {4, 0},    // Should be in hull (bottom-right corner)
           {6, 2},    // Should be in hull (right edge)
           {6, 4},    // Should be in hull (top-right corner)
           {4, 6},    // Should be in hull (top edge)
           {2, 6},    // Should be in hull (top-left corner)
           {0, 4},    // Should be in hull (left edge)
           {1, 1},    // Interior point (should be excluded)
           {3, 1},    // Collinear with {2,0} and {4,0} (should be excluded)
           {5, 3},    // Interior point (should be excluded)
           {3, 5},    // Interior point (should be excluded)
           {1, 3},    // Interior point (should be excluded)
           {2, 2},    // Interior point (should be excluded)
           {4, 2},    // Interior point (should be excluded)
           {2, 4},    // Interior point (should be excluded)
           {4, 4},    // Interior point (should be excluded)
           {3, 3},    // Dead center (should be excluded)
           {0, 2},    // Collinear with {0,0} and {0,4} (should be excluded)
           {6, 0}     // Collinear with {4,0} and {6,2} (should be excluded)
       };
       
       Point[] points = createPoints(input);
       System.out.println("Expected: [(0,0), (6,0), (6,4), (4,6), (2,6), (0,4)]");
       System.out.println("Actual: " + jarvis.findConvexHull(points));
   
   
       }
    }