package engine.physics;

import engine.math.ColorRGBA;

/**
 * Estados materiais usados pelas simulacoes educacionais.
 * Os valores sao aproximacoes didaticas: o objetivo e gerar respostas fisicas
 * coerentes e comparaveis dentro do motor.
 */
public enum MaterialState {
    GELATIN("Gelatina", 1050, 0.08, 0.92, 0.14, 0.985, 0.78, 2600, new ColorRGBA(0.95, 0.25, 0.55)),
    RUBBER("Borracha", 1100, 0.86, 0.82, 0.48, 0.996, 0.90, 4200, new ColorRGBA(0.12, 0.18, 0.20)),
    WOOD("Madeira", 700, 0.35, 0.62, 0.55, 0.992, 0.86, 1100, new ColorRGBA(0.62, 0.38, 0.16)),
    STONE("Pedra", 2600, 0.28, 0.78, 0.82, 0.994, 0.88, 1800, new ColorRGBA(0.45, 0.48, 0.52)),
    STEEL("Aco", 7850, 0.52, 0.34, 0.96, 0.998, 0.94, 9000, new ColorRGBA(0.72, 0.78, 0.84)),
    GLASS("Vidro", 2500, 0.62, 0.20, 0.74, 0.997, 0.92, 260, new ColorRGBA(0.55, 0.85, 1.00)),
    FOAM("Espuma", 75, 0.18, 0.88, 0.08, 0.980, 0.76, 180, new ColorRGBA(0.92, 0.92, 0.72));

    private static final MaterialState[] VALUES = values();

    public final String label;
    public final double densityKgM3;
    public final double restitution;
    public final double friction;
    public final double hardness;
    public final double linearDamping;
    public final double angularDamping;
    public final double fractureJPerKg;
    public final ColorRGBA color;

    MaterialState(String label, double densityKgM3, double restitution,
                  double friction, double hardness, double linearDamping,
                  double angularDamping, double fractureJPerKg, ColorRGBA color) {
        this.label = label;
        this.densityKgM3 = densityKgM3;
        this.restitution = restitution;
        this.friction = friction;
        this.hardness = hardness;
        this.linearDamping = linearDamping;
        this.angularDamping = angularDamping;
        this.fractureJPerKg = fractureJPerKg;
        this.color = color;
    }

    public double massForSphere(double radiusMeters) {
        double volume = 4.0 / 3.0 * Math.PI * radiusMeters * radiusMeters * radiusMeters;
        return Math.max(0.05, densityKgM3 * volume);
    }

    public double impactAbsorption() {
        return 1.0 - hardness;
    }

    public double fractureEnergyForSphere(double radiusMeters) {
        return massForSphere(radiusMeters) * fractureJPerKg;
    }

    public static MaterialState byIndex(double index) {
        int i = Math.max(0, Math.min(VALUES.length - 1, (int)Math.round(index)));
        return VALUES[i];
    }

    public static String[] labels() {
        String[] labels = new String[VALUES.length];
        for (int i = 0; i < VALUES.length; i++) labels[i] = VALUES[i].label;
        return labels;
    }
}
