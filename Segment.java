import com.sun.javafx.geom.Vec3d;

public class Segment {
    private Vec3d startPos;
    private Vec3d direction;
    private Vec3d rotationAxis;
    public Segment(Vec3d startPos, Vec3d direction, Vec3d rotationAxis){
        this.startPos = startPos;
        this.direction = direction;
        this.rotationAxis = rotationAxis;
    }

    public Vec3d getDirection() {
        return direction;
    }

    public Vec3d getStartPos() {
        return startPos;
    }

    public void setDirection(Vec3d direction) {
        this.direction = direction;
    }

    public void setStartPos(Vec3d startPos) {
        this.startPos = startPos;
    }

    public Vec3d getRotationAxis() {
        return rotationAxis;
    }

    public void setRotationAxis(Vec3d rotationAxis) {
        this.rotationAxis = rotationAxis;
    }

    public Segment deepClone(){
        return new Segment(new Vec3d(startPos.x, startPos.y, startPos.z), new Vec3d(direction.x, direction.y, direction.z), new Vec3d(rotationAxis.x, rotationAxis.y, rotationAxis.z));

    }
}

