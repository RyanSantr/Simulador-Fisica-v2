package engine.math;

/**
 * Vetor 3D imutável de alto desempenho.
 * Operações retornam novas instâncias (padrão funcional).
 * Para mutabilidade use Vec3Mutable nos loops de física.
 */
public final class Vec3 {

    public static final Vec3 ZERO   = new Vec3(0, 0, 0);
    public static final Vec3 ONE    = new Vec3(1, 1, 1);
    public static final Vec3 UP     = new Vec3(0, 1, 0);
    public static final Vec3 RIGHT  = new Vec3(1, 0, 0);
    public static final Vec3 FORWARD = new Vec3(0, 0, -1);

    public final double x, y, z;

    public Vec3(double x, double y, double z) {
        this.x = x; this.y = y; this.z = z;
    }

    // ── Operações básicas ──────────────────────────
    public Vec3 add(Vec3 o)              { return new Vec3(x+o.x, y+o.y, z+o.z); }
    public Vec3 add(double dx, double dy, double dz) { return new Vec3(x+dx, y+dy, z+dz); }
    public Vec3 sub(Vec3 o)              { return new Vec3(x-o.x, y-o.y, z-o.z); }
    public Vec3 mul(double s)            { return new Vec3(x*s, y*s, z*s); }
    public Vec3 div(double s)            { return new Vec3(x/s, y/s, z/s); }
    public Vec3 negate()                 { return new Vec3(-x, -y, -z); }

    // ── Produto escalar e vetorial ─────────────────
    public double dot(Vec3 o)            { return x*o.x + y*o.y + z*o.z; }
    public Vec3 cross(Vec3 o) {
        return new Vec3(
            y*o.z - z*o.y,
            z*o.x - x*o.z,
            x*o.y - y*o.x
        );
    }

    // ── Magnitude ─────────────────────────────────
    public double lengthSq()             { return x*x + y*y + z*z; }
    public double length()               { return Math.sqrt(lengthSq()); }

    public Vec3 normalize() {
        double len = length();
        return len < 1e-10 ? ZERO : div(len);
    }

    // ── Utilitários ────────────────────────────────
    public double distanceTo(Vec3 o)     { return sub(o).length(); }
    public double distanceSqTo(Vec3 o)   { return sub(o).lengthSq(); }

    public Vec3 lerp(Vec3 to, double t) {
        return new Vec3(
            x + (to.x - x) * t,
            y + (to.y - y) * t,
            z + (to.z - z) * t
        );
    }

    public Vec3 reflect(Vec3 normal) {
        return sub(normal.mul(2.0 * dot(normal)));
    }

    public Vec3 clamp(double minLen, double maxLen) {
        double len = length();
        if (len < 1e-10) return ZERO;
        if (len < minLen) return normalize().mul(minLen);
        if (len > maxLen) return normalize().mul(maxLen);
        return this;
    }

    public static Vec3 random() {
        return new Vec3(
            Math.random() * 2 - 1,
            Math.random() * 2 - 1,
            Math.random() * 2 - 1
        ).normalize();
    }

    @Override
    public String toString() {
        return String.format("Vec3(%.3f, %.3f, %.3f)", x, y, z);
    }
}
