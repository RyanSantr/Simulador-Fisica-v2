package engine.validation;

import engine.physics.MaterialState;
import engine.physics.MediumState;
import engine.physics.PhysicsWorld;
import engine.renderer.Camera;
import engine.simulation.LabMeasurement;
import engine.simulation.SimulationContext;
import engine.simulation.modules.CollisionModule;
import engine.simulation.modules.FreeFallModule;
import engine.simulation.modules.InclinedPlaneModule;
import engine.simulation.modules.PendulumModule;
import engine.simulation.modules.ProjectileModule;
import engine.simulation.modules.SolarSystemModule;
import engine.simulation.modules.SpringModule;
import engine.simulation.modules.TugOfWarModule;

import java.text.Normalizer;

public final class PhysicsValidation {

    private static final double DT = 1.0 / 240.0;

    private PhysicsValidation() {}

    public static void main(String[] args) {
        validateFreeFall();
        validateProjectileVacuumAndDrag();
        validateProjectileGlassImpact();
        validateCollisionMaterials();
        validateInclinedPlane();
        validatePendulum();
        validateSpring();
        validateSolarSystem();
        validateTugOfWar();
        System.out.println("OK: validacoes fisicas passaram.");
    }

    private static void validateFreeFall() {
        SimulationContext context = context();
        FreeFallModule module = new FreeFallModule();
        module.getParameters().setValue("gravity", 9.81);
        module.getParameters().setValue("initial_height", 8.0);
        module.getParameters().setValue("initial_velocity", 0.0);
        module.onActivate(context);

        double expectedTime = Math.sqrt(2.0 * 8.0 / 9.81);
        for (int i = 0; i < 600 && module.getDropTime() < expectedTime; i++) {
            module.onUpdate(DT);
        }

        assertClose("queda livre: tempo de impacto", expectedTime, module.getDropTime(), 1e-9);
        assertClose("queda livre: tempo teorico", expectedTime, module.getTheoreticalTime(), 1e-9);
        assertTrue("queda livre: velocidade escalar nao pode aparecer negativa",
            !module.telemetry().contains("Velocidade: -"));

        SimulationContext noGravityContext = context();
        noGravityContext.getPhysics().setGravityEnabled(false);
        FreeFallModule noGravity = new FreeFallModule();
        noGravity.getParameters().setValue("initial_height", 8.0);
        noGravity.getParameters().setValue("initial_velocity", 0.0);
        noGravity.onActivate(noGravityContext);
        for (int i = 0; i < 240; i++) noGravity.onUpdate(DT);
        assertClose("queda livre: altura fica constante sem gravidade",
            8.0, noGravity.sampleLabMeasurement().seriesAValue(), 1e-9);

        FreeFallModule moon = new FreeFallModule();
        moon.getParameters().setValue("gravity", 1.62);
        moon.getParameters().setValue("initial_height", 2.0);
        moon.getParameters().setValue("initial_velocity", 0.0);
        moon.onActivate(context());
        assertClose("queda livre: exemplo Lua 2 m",
            Math.sqrt(2.0 * 2.0 / 1.62), moon.getTheoreticalTime(), 1e-9);
    }

    private static void validateProjectileVacuumAndDrag() {
        ProjectileModule vacuum = projectile(MediumState.VACUUM);
        vacuum.launch();
        for (int i = 0; i < 120; i++) vacuum.onUpdate(DT);

        double v0 = 20.0;
        double angle = Math.toRadians(45.0);
        double expectedRange = v0 * v0 * Math.sin(2.0 * angle) / 9.81;
        assertClose("projetil: alcance teorico no vacuo",
            expectedRange, vacuum.getTheoreticalRange(), 1e-9);

        double vx = v0 * Math.cos(angle);
        double vy = v0 * Math.sin(angle) - 9.81 * 0.5;
        double expectedSpeed = Math.sqrt(vx * vx + vy * vy);
        double vacuumSpeed = vacuum.sampleLabMeasurement().seriesBValue();
        assertClose("projetil: velocidade no vacuo apos 0,5 s",
            expectedSpeed, vacuumSpeed, 0.08);

        ProjectileModule water = projectile(MediumState.WATER);
        water.launch();
        for (int i = 0; i < 120; i++) water.onUpdate(DT);
        double waterSpeed = water.sampleLabMeasurement().seriesBValue();
        assertTrue("projetil: agua deve reduzir velocidade mais que vacuo",
            waterSpeed < vacuumSpeed * 0.82);
        assertFinite("projetil: telemetria", water.sampleLabMeasurement());
    }

    private static void validateProjectileGlassImpact() {
        SimulationContext context = context();
        ProjectileModule module = new ProjectileModule();
        module.getParameters().setValue("projectile_material", MaterialState.STEEL.ordinal());
        module.getParameters().setValue("target_material", MaterialState.GLASS.ordinal());
        module.getParameters().setValue("medium", MediumState.VACUUM.ordinal());
        module.getParameters().setValue("speed", 60.0);
        module.getParameters().setValue("angle", 0.0);
        module.getParameters().setValue("gravity", 9.81);
        module.getParameters().setValue("n_targets", 1.0);
        module.onActivate(context);
        module.launch();
        for (int i = 0; i < 1200 && module.getTargetsHit() == 0; i++) {
            module.onUpdate(DT);
        }

        assertTrue("projetil: bola de aco deve atingir alvo de vidro", module.getTargetsHit() > 0);
        assertTrue("projetil: vidro deve reagir como material fragil",
            mentionsGlassBreak(module.telemetry()));
    }


    private static ProjectileModule projectile(MediumState medium) {
        SimulationContext context = context();
        ProjectileModule module = new ProjectileModule();
        module.getParameters().setValue("projectile_material", MaterialState.STONE.ordinal());
        module.getParameters().setValue("target_material", MaterialState.STEEL.ordinal());
        module.getParameters().setValue("medium", medium.ordinal());
        module.getParameters().setValue("speed", 20.0);
        module.getParameters().setValue("angle", 45.0);
        module.getParameters().setValue("gravity", 9.81);
        module.getParameters().setValue("n_targets", 1.0);
        module.onActivate(context);
        return module;
    }

    private static void validateCollisionMaterials() {
        CollisionModule module = new CollisionModule();
        module.getParameters().setValue("material_a", MaterialState.STONE.ordinal());
        module.getParameters().setValue("material_b", MaterialState.GELATIN.ordinal());
        module.getParameters().setValue("velocity_a", 6.0);
        module.onActivate(context());
        module.startCollision();
        for (int i = 0; i < 1000 && !module.isCollisionOccurred(); i++) {
            module.onUpdate(DT);
        }

        assertTrue("colisao: impacto deve acontecer", module.isCollisionOccurred());
        assertTrue("colisao: energia nao deve aumentar",
            module.getKineticEnergyAfter() <= module.getKineticEnergyBefore() * 1.0001);
        assertTrue("colisao: restituicao medida deve ser fisica",
            module.getMeasuredRestitution() >= 0.0 && module.getMeasuredRestitution() <= 1.0);

        CollisionModule glass = new CollisionModule();
        glass.getParameters().setValue("material_a", MaterialState.STEEL.ordinal());
        glass.getParameters().setValue("material_b", MaterialState.GLASS.ordinal());
        glass.getParameters().setValue("velocity_a", 25.0);
        glass.onActivate(context());
        glass.startCollision();
        for (int i = 0; i < 1000 && !glass.isCollisionOccurred(); i++) {
            glass.onUpdate(DT);
        }
        assertTrue("colisao: aco rapido deve quebrar vidro",
            mentionsGlassBreak(glass.telemetry()));
    }

    private static void validateInclinedPlane() {
        InclinedPlaneModule module = new InclinedPlaneModule();
        module.getParameters().setValue("angle", 30.0);
        module.getParameters().setValue("friction", 0.20);
        module.getParameters().setValue("gravity", 9.81);
        module.onActivate(context());
        for (int i = 0; i < 240; i++) module.onUpdate(DT);

        double expectedA = 9.81 * (Math.sin(Math.toRadians(30.0)) - 0.20 * Math.cos(Math.toRadians(30.0)));
        assertClose("plano inclinado: velocidade apos 1 s",
            expectedA, module.sampleLabMeasurement().seriesBValue(), 0.04);
    }

    private static void validatePendulum() {
        PendulumModule module = new PendulumModule();
        module.getParameters().setValue("length", 4.2);
        module.getParameters().setValue("amplitude", 8.0);
        module.getParameters().setValue("gravity", 9.81);
        module.getParameters().setValue("damping", 0.0);
        module.onActivate(context());

        double quarterPeriod = 0.5 * Math.PI * Math.sqrt(4.2 / 9.81);
        int steps = (int)Math.round(quarterPeriod / DT);
        for (int i = 0; i < steps; i++) module.onUpdate(DT);

        double angleDegrees = module.sampleLabMeasurement().seriesAValue();
        assertTrue("pendulo: deve cruzar perto do ponto central no quarto de periodo",
            Math.abs(angleDegrees) < 1.3);
    }

    private static void validateSpring() {
        SpringModule module = new SpringModule();
        module.getParameters().setValue("mass", 1.2);
        module.getParameters().setValue("stiffness", 18.0);
        module.getParameters().setValue("amplitude", 2.0);
        module.getParameters().setValue("damping", 0.0);
        module.onActivate(context());
        module.onUpdate(DT);

        double expectedA = -18.0 * 2.0 / 1.2;
        assertClose("mola: aceleracao inicial por Hooke",
            expectedA, parseAcceleration(module.telemetry()), 0.20);
    }

    private static void validateSolarSystem() {
        SolarSystemModule module = new SolarSystemModule();
        module.onActivate(context());
        for (int i = 0; i < 20; i++) module.onUpdate(1.0 / 60.0);
        String telemetry = module.telemetry();
        assertTrue("sistema solar: telemetria deve carregar Terra", mentions(telemetry, "Terra"));
        assertTrue("sistema solar: telemetria nao deve conter NaN", !telemetry.contains("NaN"));
        assertTrue("sistema solar: telemetria nao deve conter Infinity", !telemetry.contains("Infinity"));
        double earthSpeed = parseTelemetryNumber(telemetry, "Velocidade orbital:", "km/s");
        assertTrue("sistema solar: velocidade orbital da Terra deve ficar perto de 29,8 km/s",
            earthSpeed > 28.0 && earthSpeed < 31.5);
    }

    private static void validateTugOfWar() {
        SimulationContext context = context();
        TugOfWarModule module = new TugOfWarModule();
        module.onActivate(context);
        module.setLeftName("Ana");
        module.setRightName("Leo");

        for (int i = 0; i < 1200 && module.getWinner().isBlank(); i++) {
            if (i % 3 == 0) module.pressLeft();
            module.holdLeft(DT);
            module.onUpdate(DT);
            context.advanceSimTime(DT);
        }

        assertTrue("cabo de guerra: jogador A deve vencer com entrada dominante",
            module.getWinner().contains("Jogador A"));
        assertTrue("cabo de guerra: ranking deve registrar partida",
            !module.getRanking().isEmpty());
    }

    private static SimulationContext context() {
        return new SimulationContext(new PhysicsWorld(), new Camera());
    }

    private static void assertFinite(String label, LabMeasurement measurement) {
        assertTrue(label + " series A finita", Double.isFinite(measurement.seriesAValue()));
        assertTrue(label + " series B finita", Double.isFinite(measurement.seriesBValue()));
    }

    private static double parseAcceleration(String telemetry) {
        return parseTelemetryNumber(telemetry, "Aceleracao:", "m/s2");
    }

    private static double parseTelemetryNumber(String telemetry, String marker, String unit) {
        // Busca tolerante a acentos e maiusculas/minusculas para nao quebrar com pequenas
        // mudancas de texto/idioma na telemetria da UI.
        String t = normalize(telemetry);
        String m = normalize(marker);
        String u = normalize(unit);
        int start = t.indexOf(m);
        if (start < 0) throw new AssertionError("Campo nao encontrado: " + marker + " em " + telemetry);
        start += m.length();
        int end = t.indexOf(u, start);
        if (end < 0) throw new AssertionError("Unidade nao encontrada: " + unit + " em " + telemetry);
        return Double.parseDouble(t.substring(start, end).replace(',', '.').trim());
    }

    /** Reconhece a quebra de um alvo de vidro de forma resiliente a variacoes de texto. */
    private static boolean mentionsGlassBreak(String telemetry) {
        String t = normalize(telemetry);
        return t.contains("vidro")
            && (t.contains("quebrad") || t.contains("estilhac") || t.contains("frag"));
    }

    /** Verifica presenca de um termo ignorando acentos e caixa. */
    private static boolean mentions(String telemetry, String term) {
        return normalize(telemetry).contains(normalize(term));
    }

    /** Minusculas + remocao de acentos (NFD) para comparacoes textuais estaveis. */
    private static String normalize(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(java.util.Locale.ROOT);
    }

    private static void assertClose(String label, double expected, double actual, double tolerance) {
        if (!Double.isFinite(actual) || Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(label + " esperado=" + expected + " obtido=" + actual
                + " tolerancia=" + tolerance);
        }
    }

    private static void assertTrue(String label, boolean condition) {
        if (!condition) throw new AssertionError(label);
    }
}
