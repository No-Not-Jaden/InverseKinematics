import com.sun.javafx.geom.Vec3d;

public class Attempt2 {
    public static Segment[] segments;
    public static final Segment[] home = new Segment[4];
    public static Vec3d targetPoint = new Vec3d(1,20,0);
    public static double finalA1Rotation = 0;
    public static double finalA2Rotation = 0;
    public static double finalA3Rotation = 0;
    public static double finalA4Rotation = 0;
    public static boolean A4CloseCC = true;
    public static final double minRotation = 0.01;
    public static boolean randomTests = false;
    public static long startMs;
    public static void main(String[] args) {
        // this is the layout of the arm, it includes a start position, direction, and rotation axis
        segments = new Segment[]{
                new Segment(new Vec3d(0,0,0), new Vec3d(1,0,-1), new Vec3d(1,0,0)),
                new Segment(new Vec3d(1,0,-1), new Vec3d(0,0,1), new Vec3d(0,0,1)),
                new Segment(new Vec3d(1,0,0), new Vec3d(0,-10,0), new Vec3d(0,-1,0)),
                new Segment(new Vec3d(1,-10,0), new Vec3d(0,-10,0), new Vec3d(-1,0,0))};
        // how DEEP do I need to clone this before It doesn't change
        // should have just made it final from the start T.T
        for (int i = 0; i < segments.length; i++) {
            //home[i] = new Segment(new Vec3d(segments[i].getStartPos().x, segments[i].getStartPos().y, segments[i].getStartPos().z), new Vec3d(segments[i].getDirection().x, segments[i].getDirection().y, segments[i].getDirection().z), new Vec3d(segments[i].getRotationAxis().x, segments[i].getRotationAxis().y, segments[i].getRotationAxis().z));
            home[i] = segments[i].deepClone();
        }
        if (randomTests) {
            // get a random target point
            targetPoint = new Vec3d(Math.random() * 38 - 19, Math.random() * 38 - 19, Math.random() * 38 - 19);
            while (targetPoint.length() > 19) {
                targetPoint = new Vec3d(Math.random() * 38 - 19, Math.random() * 38 - 19, Math.random() * 38 - 19);
            }



            // rotate it a random amount of times to get it in a random position
            for (int i = 0; i < segments.length; i++) {
                double rot = Math.random() * Math.PI * 2;
                rotateSegment(i, rot);
                System.out.println("R" + i + ": " + rot);
            }
        }
            startMs = System.currentTimeMillis();
        // prelude to finding position, we will try and rotate back to home
        // can't use a for loop because some axis use different references for rotation
            double A1Rotation = angleOnPlane(segments[1].getRotationAxis(), home[1].getRotationAxis(), segments[0].getRotationAxis());
            rotateSegment(0, A1Rotation);
            finalA1Rotation+= A1Rotation;
            double A2Rotation = angleOnPlane(segments[2].getDirection(), home[2].getDirection(), segments[1].getRotationAxis());
            rotateSegment(1, A2Rotation);
            finalA2Rotation+= A2Rotation;
            double A3Rotation = angleOnPlane(segments[3].getRotationAxis(), home[3].getRotationAxis(), segments[2].getRotationAxis());
            rotateSegment(2, A3Rotation);
            finalA3Rotation+= A3Rotation;
            double A4Rotation = angleOnPlane(segments[3].getDirection(), home[3].getDirection(), segments[3].getRotationAxis());
            rotateSegment(3, A4Rotation);
            finalA4Rotation+= A4Rotation;
            // comparing to see if they got to home

            double score = 0;
            for (int i = 0; i < segments.length; i++){
                Vec3d diff = new Vec3d();
                diff.sub(segments[i].getDirection(), home[i].getDirection());
                score+= Math.abs(diff.x) + Math.abs(diff.y) + Math.abs(diff.z);
                System.out.println(i + ": " + score);
            }
            // goal is for this to be 0, usually it is or close
        System.out.println("Score from home: " + score);

            // First step, find where A4 can be
            // Using the law of cosines with a triangle of A3-A4-Target, we can get the angles
            // with the angles, there becomes a circle of possible locations
            Vec3d A3toTarget = new Vec3d();
            A3toTarget.sub(targetPoint, segments[2].getStartPos());
            double hypotenuse = A3toTarget.length();
            System.out.println("Length to target: " + hypotenuse);
            // this is the lower arm length
            double l1 = segments[2].getDirection().length();
            // this is the upper arm length
            double l2 = segments[3].getDirection().length();
            // quick check to see if it even is in range
            if (hypotenuse > l1 + l2) {
                System.out.println("Cannot reach that position! (>" + (l1 + l2) + ")");
            }
            // now we can use the law of cosines to get the angle in radians for A4
            double A4Angle = Math.acos((l1 * l1 + l2 * l2 - hypotenuse * hypotenuse) / (2 * l1 * l2));
            // using the value we previously calculated, we can do the law of sines to get rotation;
            double A3Angle = Math.asin((l2 / hypotenuse) * Math.sin(A4Angle));
            System.out.println("Angle between target and new A4 Target: " + displayRotation(A3Angle));
            //now we find the total angle from the arm to the target
            // this is the normal we will be checking rotation of off
            Vec3d rotationNormal = new Vec3d();
            rotationNormal.cross(A3toTarget, segments[2].getDirection());
        rotationNormal.normalize();
            if (Double.isNaN(rotationNormal.x)){
                rotationNormal = new Vec3d(segments[3].getRotationAxis());
            }

            System.out.println("Rotation normal: " + rotationNormal);
            // this is the total rotation from the arm's current position to pointing directly at the target
            double armToTargetAngle = angleOnPlane(segments[2].getDirection(), A3toTarget, rotationNormal);
            // this is the updated rotation to get the arm pointed at the new A4 position
            double armToNewA4Angle = (Math.abs(armToTargetAngle) - A3Angle) * Math.signum(armToTargetAngle);
            System.out.println("Angle from arm to new A4 Target: " + displayRotation(armToNewA4Angle));
            // now this is the vector from A3 to the new A4 position
            Vec3d newA4 = rotateVectorCC(segments[2].getDirection(), rotationNormal, armToNewA4Angle);
            // add A3 start position to get absolute position
            newA4.add(segments[2].getStartPos());
            System.out.println("A4 target: " + newA4);

            // next step is rotating A1 and A2 to get it pointed at the target
// getting the vector between the start and the target point - might need to be the point in front of the axis start
            Vec3d start2target = new Vec3d();
            start2target.sub(newA4, segments[0].getStartPos());
            // getting the normal vector of the plane with start2target and A1 axis of rotation
            Vec3d newA2 = new Vec3d();
            newA2.cross(segments[0].getRotationAxis(), start2target);
            newA2.normalize();

            // rotate A1 enough to change a2 to match the calculated value
            double radians = angleOnPlane(segments[1].getRotationAxis(), newA2, segments[0].getRotationAxis());

            //System.out.println("To Rotate: " + displayRotation(radians));
            // this angle will be between π and -π. If the value is above π/2, then it would be faster to rotate its complimentary angle
            // for every target position, there are 2 positions that A1 could rotate to, but one will almost always be quicker than the other
            if (radians > Math.PI / 2) {
                radians -= Math.PI;
            } else if (radians < Math.PI / -2) {
                radians += Math.PI;
            }
            //System.out.println("A1 Rotation: " + displayRotation(radians));
            finalA1Rotation += radians;
            rotateSegment(0, radians);
            // now find how much A2 needs to rotate by getting the angle between A3 axis and the vector between the target and start position of A3
            // we can use the axis of rotation as the normal for finding the angle (like every other time we will need to find rotation) in this case, A2
            Vec3d a32target = new Vec3d(newA4);
            a32target.sub(segments[2].getStartPos());
            radians = angleOnPlane(segments[2].getRotationAxis(), a32target, segments[1].getRotationAxis());
            //System.out.println("A2 Rotation: " + displayRotation(radians));
            finalA2Rotation += radians;
            rotateSegment(1, radians);
            // rotate A3 to get A4 to close toward the target
            // A4 axis needs to be perpendicular to the vector of A42target and the vector of A42A3
            Vec3d A42target = new Vec3d();
            A42target.sub(targetPoint, segments[3].getStartPos());
            Vec3d A42A3 = new Vec3d();
            A42A3.sub(segments[2].getStartPos(), segments[3].getStartPos());
            newA4 = new Vec3d();
            newA4.cross(A42target, A42A3);
            newA4.normalize();
            // if A4CloseCC is true and the angle of rotation of A42A3 and A42target with newly calculated A4 vector as the normal is positive,
            // then the new A4 vector has to be flipped to go in the opposite direction - if A4CloseCC is false, then the angle has to be negative
            if (A4CloseCC) {
                if (angleOnPlane(A42A3, A42target, newA4) > 0) {
                    newA4 = new Vec3d(-newA4.x, -newA4.y, -newA4.z);
                }
            } else {
                if (angleOnPlane(A42A3, A42target, newA4) < 0) {
                    newA4 = new Vec3d(-newA4.x, -newA4.y, -newA4.z);
                }
            }
            // now we just have to rotate A3 to get A4 to the new position
            radians = angleOnPlane(segments[3].getRotationAxis(), newA4, segments[2].getRotationAxis());
            //System.out.println("A3 Rotation: " + displayRotation(radians));
            rotateSegment(2, radians);
            finalA3Rotation += radians;

            A42A3.sub(new Vec3d(0, 0, 0), segments[3].getDirection());
            double currentA4Rotation = angleOnPlane(segments[3].getDirection(), A42A3, segments[3].getRotationAxis());
            // subtract currentA4Rotation by A4Rotation to get the required rotation
            radians = currentA4Rotation - A4Angle;
            if (radians < 0) {
                radians += Math.PI * 2;
            }
            //System.out.println("A4 Rotation: " + displayRotation(radians));
            rotateSegment(3, radians);
            finalA4Rotation += radians;

        System.out.println("Final Rotations: ");
        System.out.println("A1: " + displayRotation(finalA1Rotation));
        System.out.println("A2: " + displayRotation(finalA2Rotation));
        System.out.println("A3: " + displayRotation(finalA3Rotation));
        System.out.println("A4: " + displayRotation(finalA4Rotation));
        System.out.println("Total: " + displayRotation(Math.abs(finalA4Rotation) + Math.abs(finalA3Rotation) + Math.abs(finalA2Rotation) + Math.abs(finalA1Rotation)));
        System.out.println("Completed in " + (System.currentTimeMillis() - startMs) + "ms");
        Vec3d finalPosition = new Vec3d(segments[0].getStartPos());
        for (Segment segment : segments) {
            finalPosition.add(segment.getDirection());
        }
        System.out.println("Target Position: " + targetPoint);
        System.out.println("Final Position: " + finalPosition);
        System.out.println("Score: " + Math.sqrt(Math.pow(finalPosition.x - targetPoint.x, 2) + Math.pow(finalPosition.y - targetPoint.y, 2) + Math.pow(finalPosition.z - targetPoint.z, 2)));


    }
    public static String displayRotation(double radians){
        String ans = convertDecimalToFraction((radians / Math.PI));
        if (Math.abs(Long.parseLong(ans.substring(0,ans.indexOf("/")))) < 10 && Math.abs(Long.parseLong(ans.substring(ans.indexOf("/") + 1))) < 10) {
            ans = ans.substring(0, ans.indexOf("/")) + "π" + ans.substring(ans.indexOf("/"));
        } else {
            ans = ((double) Math.round((radians * 1000)) / 1000) + "π";
        }
        return ans + " | " + Math.round(radians * 57.2958) + "°";
    }

    // rotates a segment and any other segment effected in radians
    public static void rotateSegment(int segment, double angle){
        if (Math.abs(angle) > minRotation) {
            Vec3d rotationAxis = new Vec3d(segments[segment].getRotationAxis());
            for (int i = segment; i < segments.length; i++) {
                segments[i].setStartPos(rotateVectorCC(segments[i].getStartPos(), rotationAxis, angle));
                segments[i].setDirection(rotateVectorCC(segments[i].getDirection(), rotationAxis, angle));
                segments[i].setRotationAxis(rotateVectorCC(segments[i].getRotationAxis(), rotationAxis, angle));
            }
        }
    }
    // gets angle of vector if you are looking at a plane from the "n" normal vector side
    // Is a unit circle, so angle is in terms of pi and will return [pi, -pi]
    public static double angleOnPlane(Vec3d v1, Vec3d v2, Vec3d n){
        double dot = v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
        double det = v1.x*v2.y*n.z + v2.x*n.y*v1.z + n.x*v1.y*v2.z - v1.z *v2.y*n.x - v2.z*n.y*v1.x - n.z*v1.y*v2.x;
        return Math.atan2(det, dot);
    }

    public static void displaySegments(){
        for (int i = 0; i < segments.length; i++){
            System.out.println("<" + i + "> P: " + segments[i].getStartPos().toString() + " D: " + segments[i].getDirection().toString() + " R: " + segments[i].getRotationAxis().toString());
        }
    }

    // Credit to https://stackoverflow.com/questions/31585931/how-to-convert-decimal-to-fractions
    static private String convertDecimalToFraction(double x){
        if (x < 0){
            return "-" + convertDecimalToFraction(-x);
        }
        double tolerance = 1.0E-6;
        double h1=1; double h2=0;
        double k1=0; double k2=1;
        double b = x;
        do {
            double a = Math.floor(b);
            double aux = h1; h1 = a*h1+h2; h2 = aux;
            aux = k1; k1 = a*k1+k2; k2 = aux;
            b = 1/(b-a);
        } while (Math.abs(x-h1/k1) > x*tolerance);

        return Math.round(h1)+"/"+Math.round(k1);
    }

    // Credit to https://stackoverflow.com/questions/31225062/rotating-a-vector-by-angle-and-axis-in-java
    public static Vec3d rotateVectorCC(Vec3d vec, Vec3d axis, double theta){
        double x, y, z;
        double u, v, w;
        x=vec.x;y=vec.y;z=vec.z;
        u=axis.x;v=axis.y;w=axis.z;
        double v1 = u * x + v * y + w * z;
        double xPrime = u* v1 *(1d - Math.cos(theta))
                + x*Math.cos(theta)
                + (-w*y + v*z)*Math.sin(theta);
        double yPrime = v* v1 *(1d - Math.cos(theta))
                + y*Math.cos(theta)
                + (w*x - u*z)*Math.sin(theta);
        double zPrime = w* v1 *(1d - Math.cos(theta))
                + z*Math.cos(theta)
                + (-v*x + u*y)*Math.sin(theta);
        return new Vec3d(xPrime, yPrime, zPrime);
    }
}
