package engine.physics;

import engine.math.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Physics world with a fixed 120 Hz step and a bounding-sphere broad phase.
 */
public class PhysicsWorld {

    private static final int MAX_SUBSTEPS = 8;
    private static final double FIXED_DT = 1.0 / 120.0;
    private static final double ARENA_HALF = 18.0;
    private static final double FLOOR_Y = -5.0;

    private final List<RigidBody> bodies = new ArrayList<>();
    private Vec3 gravity = new Vec3(0, -12.0, 0);
    private boolean gravityEnabled = true;
    private double accumulator;

    private int collisionChecks;
    private int activeObjects;

    public void addBody(RigidBody body) { bodies.add(body); }

    public void removeBody(RigidBody body) { bodies.remove(body); }

    public void clear() {
        bodies.clear();
        accumulator = 0;
        collisionChecks = 0;
        activeObjects = 0;
    }

    public void update(double deltaTime) {
        accumulator += Math.min(deltaTime, 0.25);

        int substeps = 0;
        while (accumulator >= FIXED_DT && substeps < MAX_SUBSTEPS) {
            step(FIXED_DT);
            accumulator -= FIXED_DT;
            substeps++;
        }
    }

    private void step(double dt) {
        Vec3 effectiveGravity = gravityEnabled ? gravity : Vec3.ZERO;

        activeObjects = 0;
        for (RigidBody body : bodies) {
            if (body.getType() != RigidBody.PhysicsType.DYNAMIC) continue;
            body.integrate(dt, effectiveGravity);
            if (!body.isSleeping()) activeObjects++;
        }

        for (RigidBody body : bodies) {
            if (body.getType() != RigidBody.PhysicsType.DYNAMIC) continue;
            body.resolveFloorCollision(FLOOR_Y, dt);
            body.resolveWallCollision(-ARENA_HALF, ARENA_HALF, -ARENA_HALF, ARENA_HALF);
        }

        collisionChecks = 0;
        int n = bodies.size();
        for (int i = 0; i < n; i++) {
            RigidBody a = bodies.get(i);
            if (a.getType() == RigidBody.PhysicsType.STATIC) continue;

            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                RigidBody b = bodies.get(j);

                // Dynamic pairs are checked once; static bodies are checked only
                // from the movable side so large decorative scenes stay cheap.
                if (b.getType() != RigidBody.PhysicsType.STATIC && j <= i) continue;
                if (a.isSleeping() && b.isSleeping()) continue;

                double minDist = a.getBoundingRadius() + b.getBoundingRadius();
                if (a.getPosition().distanceSqTo(b.getPosition()) <= minDist * minDist) {
                    collisionChecks++;
                    RigidBody.resolveSphereSphere(a, b);
                }
            }
        }
    }

    public void explodeAt(Vec3 center, double strength) {
        bodies.forEach(body -> body.applyExplosion(center, strength));
    }

    public void setGravityEnabled(boolean enabled) { this.gravityEnabled = enabled; }
    public boolean isGravityEnabled() { return gravityEnabled; }
    public Vec3 getGravityVector() { return gravityEnabled ? gravity : Vec3.ZERO; }

    public void setGravityStrength(double g) {
        gravity = new Vec3(0, -Math.abs(g), 0);
    }

    public List<RigidBody> getBodies() { return bodies; }
    public int getBodyCount() { return bodies.size(); }
    public int getActiveObjects() { return activeObjects; }
    public int getCollisionChecks() { return collisionChecks; }
    public double getFloorY() { return FLOOR_Y; }
    public double getArenaHalf() { return ARENA_HALF; }
}
