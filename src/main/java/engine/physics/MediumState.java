package engine.physics;

/**
 * Meio fisico onde o projetil se move.
 * Densidade e viscosidade controlam arrasto; gravityFactor simula empuxo
 * aproximado para meios densos como agua.
 */
public enum MediumState {
    VACUUM("Vacuo", 0.0, 0.0, 1.00),
    AIR("Ar", 1.225, 0.018, 1.00),
    WATER("Agua", 997.0, 0.160, 0.72),
    OIL("Oleo", 850.0, 0.260, 0.78),
    GEL("Gel", 1040.0, 0.420, 0.62);

    private static final MediumState[] VALUES = values();

    public final String label;
    public final double densityKgM3;
    public final double dragScale;
    public final double gravityFactor;

    MediumState(String label, double densityKgM3, double dragScale, double gravityFactor) {
        this.label = label;
        this.densityKgM3 = densityKgM3;
        this.dragScale = dragScale;
        this.gravityFactor = gravityFactor;
    }

    public static MediumState byIndex(double index) {
        int i = Math.max(0, Math.min(VALUES.length - 1, (int)Math.round(index)));
        return VALUES[i];
    }

    public static String[] labels() {
        String[] labels = new String[VALUES.length];
        for (int i = 0; i < VALUES.length; i++) labels[i] = VALUES[i].label;
        return labels;
    }
}
