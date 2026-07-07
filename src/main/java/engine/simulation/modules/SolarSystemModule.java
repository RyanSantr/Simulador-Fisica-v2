package engine.simulation.modules;

import engine.math.ColorRGBA;
import engine.math.Mat4;
import engine.math.Vec3;
import engine.renderer.TextureMap;
import engine.scene.SceneObject;
import engine.simulation.ModuleSceneBuilder;
import engine.simulation.SimulationModule;
import engine.simulation.challenges.Challenge;
import engine.simulation.challenges.ChallengeResult;
import engine.simulation.parameters.Parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Sistema solar heliocentrico integrado em AU e dias.
 *
 * A dinamica usa gravidade newtoniana do Sol sobre cada planeta. A cena 3D
 * comprime a escala radial para mostrar planetas internos e externos juntos.
 */
public class SolarSystemModule extends SimulationModule {

    private static final String ID = "solar_system";
    private static final double SOLAR_MU_AU3_DAY2 = 0.0002959122082855911;
    private static final double SUN_CAPTURE_RADIUS_AU = 0.06;
    private static final double ORBIT_STEP_DAYS = 0.18;
    private static final int TRAIL_MARKERS = 10;
    private static final int ORBIT_GUIDE_MARKERS = 36;
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double SYSTEM_VIEW_RADIUS = 39.0;
    private static final double PLANET_FOCUS_MIN_RADIUS = 3.35;
    private static final double PLANET_FOCUS_MARGIN = 2.85;

    private final List<PlanetBody> bodies = new ArrayList<>();
    private final List<SceneObject> solarHalos = new ArrayList<>();
    private SceneObject sun;
    private Vec3 lastFocusedPlanetPosition;
    private double simulatedDays;
    private double currentSolarMassFactor = 1.0;

    public SolarSystemModule() {
        super(ID, "Sistema Solar",
            "Compare como cada planeta reage a mudancas na massa do Sol e a impulsos orbitais controlados.",
            "Gravitacao - Orbitas e Vetores");
    }

    @Override
    protected void declareParameters() {
        parameters.add(Parameter.of("planet", "Planeta observado", Planet.EARTH.ordinal())
            .options(Planet.labels())
            .tooltip("O planeta selecionado recebe os impulsos de velocidade e aparece na telemetria."));

        parameters.add(Parameter.of("situation", "Situacao", Situation.STABLE.ordinal())
            .options(Situation.labels())
            .tooltip("Escolha a condicao orbital a comparar."));

        parameters.add(Parameter.of("intensity", "Intensidade", 70.0)
            .range(0, 100).unit("%").step(1)
            .tooltip("Controla quanto a situacao altera a massa solar ou a velocidade do planeta."));

        parameters.add(Parameter.of("days_per_second", "Dias por segundo", 12.0)
            .range(0.2, 180).unit("d/s").step(0.2)
            .tooltip("Acelera o tempo da simulacao sem mudar as equacoes orbitais."));

        parameters.add(Parameter.of("camera_focus", "Foco camera", CameraFocus.SYSTEM.ordinal())
            .options(CameraFocus.labels())
            .tooltip("Veja o sistema inteiro ou acompanhe o planeta observado."));
    }

    @Override
    protected void buildScene() {
        ModuleSceneBuilder builder = new ModuleSceneBuilder(context);
        context.setGravityStrength(0);
        context.getCamera().setOrbitView(0.58, 1.10, SYSTEM_VIEW_RADIUS, Vec3.ZERO);

        currentSolarMassFactor = situation().solarMassFactor(strength());
        bodies.clear();
        solarHalos.clear();
        lastFocusedPlanetPosition = null;
        simulatedDays = 0;

        sun = builder.sphere(1.15, 10, 16, ColorRGBA.fromHex("#ffd35b"))
            .at(0, 0, 0)
            .isStatic()
            .named("sol")
            .add();
        solarHalos.add(builder.sphere(1.42, 5, 8, new ColorRGBA(1.0, 0.65, 0.15, 0.12))
            .at(0, 0, 0).isStatic().named("halo_sol_0").add());
        solarHalos.add(builder.sphere(1.72, 5, 8, new ColorRGBA(1.0, 0.40, 0.08, 0.07))
            .at(0, 0, 0).isStatic().named("halo_sol_1").add());

        for (Planet planet : Planet.values()) {
            addOrbitGuide(builder, planet);
            bodies.add(createPlanet(builder, planet));
        }
        context.getObjects().forEach(object -> object.setShadowCaster(false));
        frameCamera();
    }

    @Override
    public void onUpdate(double dt) {
        if (bodies.isEmpty()) return;
        double days = Math.min(12.0, dt * parameters.getValue("days_per_second", 12.0));
        simulatedDays += days;
        while (days > 1e-9) {
            double step = Math.min(ORBIT_STEP_DAYS, days);
            for (PlanetBody body : bodies) {
                integrate(body, step);
            }
            days -= step;
        }
        for (PlanetBody body : bodies) {
            updatePlanetVisuals(body, dt);
        }
        updateSolarVisuals();
        updateFocusVisuals();
    }

    @Override
    public void onParameterChanged(String paramName, double newValue) {
        if (!paramName.equals("days_per_second")) {
            onReset();
        }
    }

    public static List<Challenge> buildChallenges() {
        return List.of(
            new Challenge("solar_01",
                "Planetas internos e externos",
                "Observe Mercurio, Terra e Netuno em Orbita base com o mesmo tempo acelerado.",
                "Periodos orbitais maiores fazem os planetas externos mudarem de posicao mais devagar.",
                350, 120.0) {
                @Override
                protected ChallengeResult evaluateCondition(double dt) {
                    return ChallengeResult.running(getId(), "Troque o planeta observado e compare a telemetria.");
                }
            },
            new Challenge("solar_02",
                "Perturbacao orbital",
                "Acelere ou freie o planeta observado e compare sua nova orbita com a orbita base.",
                "Energia orbital maior aumenta a orbita; energia demais gera trajetoria de escape.",
                500, 160.0) {
                @Override
                protected ChallengeResult evaluateCondition(double dt) {
                    return ChallengeResult.running(getId(), "Use Situacao e Intensidade para criar a perturbacao.");
                }
            }
        );
    }

    public String telemetry() {
        PlanetBody selected = selectedBody();
        if (selected == null) return "Sistema Solar carregando...";

        OrbitState orbit = orbitState(selected);
        Planet planet = selected.planet;
        double sunMassPercent = currentSolarMassFactor * 100.0;
        return String.format(
            "Sistema solar 3D%nDinamica: a = -GM.r / |r|3%nUnidades: AU, dia e massa solar%nEscala visual: radial comprimida para caber na cena%nVisual: elementos J2000, texturas, rotacao sideral, rastros, aneis e luas maiores%n%nPlaneta: %s%nSemieixo de referencia: %.3f AU | periodo orbital: %.1f dias%nExcentricidade de referencia: %.3f | inclinacao orbital: %.2f graus%nRotacao sideral: %.5f dias | inclinacao axial: %.2f graus%nMassa: %.3f Terras | raio: %.3f Terras%nGravidade superficial: %.2f m/s2%nLuas maiores visiveis: %d%nFoco da camera: %s%n%nSituacao: %s | intensidade %.0f%%%nMassa solar efetiva: %.0f%%%nTempo simulado: %.1f dias%nDistancia atual: %.3f AU%nVelocidade orbital: %.2f km/s%nExcentricidade estimada: %.3f%nReacao: %s",
            planet.label, planet.orbitAu, planet.periodDays,
            planet.orbitalEccentricity, Math.toDegrees(planet.orbitalInclinationRadians),
            planet.rotationPeriodDays, Math.toDegrees(planet.axialTiltRadians),
            planet.massEarth, planet.radiusEarth, planet.surfaceGravityMs2, selected.moons.size(), cameraFocus().label,
            situation().label, strength() * 100.0, sunMassPercent, simulatedDays,
            selected.positionAu.length(), auPerDayToKmPerSecond(selected.velocityAuDay.length()),
            orbit.eccentricity, orbit.description
        );
    }

    private PlanetBody createPlanet(ModuleSceneBuilder builder, Planet planet) {
        OrbitalVectors orbitalVectors = referenceOrbit(planet);
        Vec3 position = orbitalVectors.positionAu;
        Vec3 velocity = orbitalVectors.velocityAuDay;

        if (planet == selectedPlanet()) {
            velocity = velocity.mul(situation().tangentialFactor(strength()));
            velocity = velocity.add(position.normalize().mul(situation().radialKickAuDay(strength())));
        }

        boolean selected = planet == selectedPlanet();
        SceneObject object = builder.sphere(planet.renderRadius, selected ? 12 : 8, selected ? 18 : 12, planet.color)
            .at(toRenderPosition(position))
            .isStatic()
            .named("planeta_" + planet.label)
            .add();
        object.setTexture(TextureMap.load(planet.texturePath));

        PlanetBody body = new PlanetBody(planet, object, position, velocity);
        createTrail(builder, body);
        createRings(builder, body);
        createMoons(builder, body);
        return body;
    }

    private void addOrbitGuide(ModuleSceneBuilder builder, Planet planet) {
        ColorRGBA guideColor = planet.color.mix(ColorRGBA.BLACK, 0.58);
        for (int i = 0; i < ORBIT_GUIDE_MARKERS; i++) {
            double eccentricAnomaly = TWO_PI * i / ORBIT_GUIDE_MARKERS;
            Vec3 physical = orbitalPosition(planet, eccentricAnomaly);
            builder.icosahedron(planet == selectedPlanet() ? 0.065 : 0.04, guideColor)
                .at(toRenderPosition(physical))
                .isStatic()
                .named("orbita_" + planet.label + "_" + i)
                .add();
        }
    }

    private OrbitalVectors referenceOrbit(Planet planet) {
        double eccentricAnomaly = solveEccentricAnomaly(
            planet.meanLongitudeRadians - planet.longitudePerihelionRadians,
            planet.orbitalEccentricity
        );
        Vec3 position = orbitalPosition(planet, eccentricAnomaly);

        double cosE = Math.cos(eccentricAnomaly);
        double sinE = Math.sin(eccentricAnomaly);
        double e = planet.orbitalEccentricity;
        double eccentricRate = Math.sqrt(SOLAR_MU_AU3_DAY2 / (planet.orbitAu * planet.orbitAu * planet.orbitAu))
            / Math.max(1e-9, 1.0 - e * cosE);
        Vec3 velocity = orbitalPlaneToWorld(
            planet,
            -planet.orbitAu * sinE * eccentricRate,
            planet.orbitAu * Math.sqrt(1.0 - e * e) * cosE * eccentricRate
        );
        return new OrbitalVectors(position, velocity);
    }

    private Vec3 orbitalPosition(Planet planet, double eccentricAnomaly) {
        double e = planet.orbitalEccentricity;
        return orbitalPlaneToWorld(
            planet,
            planet.orbitAu * (Math.cos(eccentricAnomaly) - e),
            planet.orbitAu * Math.sqrt(1.0 - e * e) * Math.sin(eccentricAnomaly)
        );
    }

    private Vec3 orbitalPlaneToWorld(Planet planet, double xOrbital, double yOrbital) {
        double ascendingNode = planet.longitudeAscendingNodeRadians;
        double argumentOfPerihelion = planet.longitudePerihelionRadians - ascendingNode;
        double cosNode = Math.cos(ascendingNode);
        double sinNode = Math.sin(ascendingNode);
        double cosPeri = Math.cos(argumentOfPerihelion);
        double sinPeri = Math.sin(argumentOfPerihelion);
        double cosInclination = Math.cos(planet.orbitalInclinationRadians);
        double sinInclination = Math.sin(planet.orbitalInclinationRadians);

        double eclipticX = (cosPeri * cosNode - sinPeri * sinNode * cosInclination) * xOrbital
            + (-sinPeri * cosNode - cosPeri * sinNode * cosInclination) * yOrbital;
        double eclipticY = (cosPeri * sinNode + sinPeri * cosNode * cosInclination) * xOrbital
            + (-sinPeri * sinNode + cosPeri * cosNode * cosInclination) * yOrbital;
        double eclipticZ = sinPeri * sinInclination * xOrbital
            + cosPeri * sinInclination * yOrbital;
        return new Vec3(eclipticX, eclipticZ, eclipticY);
    }

    private double solveEccentricAnomaly(double meanAnomaly, double eccentricity) {
        double normalizedMean = normalizeAngle(meanAnomaly);
        double eccentricAnomaly = normalizedMean + eccentricity * Math.sin(normalizedMean);
        for (int i = 0; i < 8; i++) {
            double residual = eccentricAnomaly - eccentricity * Math.sin(eccentricAnomaly) - normalizedMean;
            double derivative = 1.0 - eccentricity * Math.cos(eccentricAnomaly);
            eccentricAnomaly -= residual / derivative;
        }
        return eccentricAnomaly;
    }

    private void integrate(PlanetBody body, double dtDays) {
        if (body.capturedBySun) return;

        Vec3 a0 = solarAcceleration(body.positionAu);
        Vec3 halfVelocity = body.velocityAuDay.add(a0.mul(dtDays * 0.5));
        Vec3 nextPosition = body.positionAu.add(halfVelocity.mul(dtDays));
        if (nextPosition.length() <= SUN_CAPTURE_RADIUS_AU) {
            body.positionAu = nextPosition.normalize().mul(SUN_CAPTURE_RADIUS_AU);
            body.velocityAuDay = Vec3.ZERO;
            body.capturedBySun = true;
            body.object.setColor(body.planet.color.mix(ColorRGBA.BLACK, 0.55));
            return;
        }

        Vec3 a1 = solarAcceleration(nextPosition);
        body.positionAu = nextPosition;
        body.velocityAuDay = halfVelocity.add(a1.mul(dtDays * 0.5));
    }

    private Vec3 solarAcceleration(Vec3 positionAu) {
        double r = Math.max(SUN_CAPTURE_RADIUS_AU, positionAu.length());
        double mu = SOLAR_MU_AU3_DAY2 * currentSolarMassFactor;
        return positionAu.mul(-mu / (r * r * r));
    }

    private OrbitState orbitState(PlanetBody body) {
        if (body.capturedBySun) {
            return new OrbitState(1.0, "capturado pelo Sol apos perder energia orbital");
        }

        double r = body.positionAu.length();
        double speedSq = body.velocityAuDay.lengthSq();
        double mu = SOLAR_MU_AU3_DAY2 * currentSolarMassFactor;
        double energy = speedSq * 0.5 - mu / r;
        Vec3 h = body.positionAu.cross(body.velocityAuDay);
        Vec3 eccentricityVector = body.velocityAuDay.cross(h).div(mu).sub(body.positionAu.normalize());
        double eccentricity = eccentricityVector.length();

        if (energy >= 0 || eccentricity >= 1.0) {
            return new OrbitState(eccentricity, "energia positiva: tendencia de escape do sistema");
        }
        if (eccentricity < 0.08) {
            return new OrbitState(eccentricity, "orbita ligada quase circular");
        }
        if (situation() == Situation.STRONGER_SUN) {
            return new OrbitState(eccentricity, "gravidade solar maior curva a trajetoria para dentro");
        }
        if (situation() == Situation.WEAKER_SUN) {
            return new OrbitState(eccentricity, "gravidade solar menor alarga a orbita");
        }
        if (situation() == Situation.BRAKE_PLANET) {
            return new OrbitState(eccentricity, "velocidade menor reduz o perihelio");
        }
        if (situation() == Situation.ACCELERATE_PLANET) {
            return new OrbitState(eccentricity, "velocidade maior eleva o afelio");
        }
        if (situation() == Situation.RADIAL_IMPACT) {
            return new OrbitState(eccentricity, "impulso radial tornou a orbita mais alongada");
        }
        return new OrbitState(eccentricity, "orbita ligada eliptica");
    }

    private PlanetBody selectedBody() {
        Planet selected = selectedPlanet();
        for (PlanetBody body : bodies) {
            if (body.planet == selected) return body;
        }
        return null;
    }

    private void createTrail(ModuleSceneBuilder builder, PlanetBody body) {
        Vec3 renderPosition = toRenderPosition(body.positionAu);
        for (int i = 0; i < TRAIL_MARKERS; i++) {
            double alpha = 0.06 + 0.30 * (double)(i + 1) / TRAIL_MARKERS;
            SceneObject marker = builder.icosahedron(body.planet == selectedPlanet() ? 0.060 : 0.038,
                    new ColorRGBA(body.planet.color.r, body.planet.color.g, body.planet.color.b, alpha))
                .at(renderPosition)
                .isStatic()
                .named("rastro_" + body.planet.label + "_" + i)
                .add();
            body.trailObjects.add(marker);
            body.trailAu.add(body.positionAu);
        }
    }

    private void createRings(ModuleSceneBuilder builder, PlanetBody body) {
        if (!body.planet.hasRings) return;
        ColorRGBA ringColor = body.planet == Planet.SATURN
            ? new ColorRGBA(0.86, 0.79, 0.58, 0.88)
            : new ColorRGBA(0.66, 0.88, 0.90, 0.48);
        body.ring = builder.torus(body.planet.renderRadius * 1.38, Math.max(0.018, body.planet.renderRadius * 0.10), ringColor)
            .at(body.object.getBody().getPosition())
            .isStatic()
            .named("aneis_" + body.planet.label)
            .add();
    }

    private void createMoons(ModuleSceneBuilder builder, PlanetBody body) {
        for (NaturalSatellite satellite : NaturalSatellite.values()) {
            if (satellite.parent != body.planet) continue;
            Vec3 position = moonRenderPosition(body.object.getBody().getPosition(), satellite, 0);
            SceneObject moon = builder.sphere(satellite.renderRadius, 5, 8, satellite.color)
                .at(position)
                .isStatic()
                .named("lua_" + satellite.label)
                .add();
            body.moons.add(new MoonBody(satellite, moon));
        }
    }

    private void updatePlanetVisuals(PlanetBody body, double dt) {
        Vec3 renderPosition = toRenderPosition(body.positionAu);
        body.object.getBody().setPosition(renderPosition);
        body.spinRadians = planetSpin(body.planet);
        body.object.setRotationTransform(planetRotation(body.planet, body.spinRadians));

        body.trailClock += dt * parameters.getValue("days_per_second", 12.0);
        if (body.trailClock >= body.planet.trailCadenceDays()) {
            body.trailClock = 0;
            body.trailAu.remove(0);
            body.trailAu.add(body.positionAu);
        }
        for (int i = 0; i < body.trailObjects.size(); i++) {
            body.trailObjects.get(i).getBody().setPosition(toRenderPosition(body.trailAu.get(i)));
        }
        if (body.ring != null) {
            body.ring.getBody().setPosition(renderPosition);
            body.ring.setRotationTransform(planetRotation(body.planet, body.spinRadians * 0.10));
        }
        for (MoonBody moon : body.moons) {
            moon.object.getBody().setPosition(moonRenderPosition(renderPosition, moon.satellite, simulatedDays));
            moon.object.getBody().setRotation(new Vec3(0, simulatedDays / Math.max(0.2, moon.satellite.periodDays), 0));
        }
    }

    private void updateSolarVisuals() {
        if (sun == null) return;
        sun.getBody().setRotation(new Vec3(0.12, simulatedDays * 0.02, 0));
        for (int i = 0; i < solarHalos.size(); i++) {
            double pulse = 0.5 + 0.5 * Math.sin(simulatedDays * 0.10 + i * 1.7);
            solarHalos.get(i).setColor(i == 0
                ? new ColorRGBA(1.0, 0.65 + pulse * 0.12, 0.12, 0.08 + pulse * 0.12)
                : new ColorRGBA(1.0, 0.34 + pulse * 0.10, 0.05, 0.04 + pulse * 0.08));
            solarHalos.get(i).getBody().setRotation(new Vec3(0, simulatedDays * (0.006 + i * 0.003), 0));
        }
    }

    private void updateFocusVisuals() {
        PlanetBody selected = selectedBody();
        if (selected == null) return;
        if (cameraFocus() == CameraFocus.PLANET) {
            Vec3 planetPosition = selected.object.getBody().getPosition();
            if (lastFocusedPlanetPosition == null) {
                context.getCamera().setTarget(planetPosition);
            } else {
                Vec3 orbitDelta = planetPosition.sub(lastFocusedPlanetPosition);
                context.getCamera().setTarget(context.getCamera().getTarget().add(orbitDelta));
            }
            lastFocusedPlanetPosition = planetPosition;
        }
    }

    private void frameCamera() {
        if (cameraFocus() != CameraFocus.PLANET) return;

        PlanetBody selected = selectedBody();
        if (selected == null) return;

        context.getCamera().setOrbitView(
            0.58,
            1.10,
            planetFocusRadius(selected),
            selected.object.getBody().getPosition()
        );
        lastFocusedPlanetPosition = selected.object.getBody().getPosition();
    }

    private double planetFocusRadius(PlanetBody body) {
        double localRadius = body.planet.renderRadius;
        if (body.ring != null) {
            localRadius = Math.max(localRadius, body.planet.renderRadius * 1.58);
        }
        for (MoonBody moon : body.moons) {
            double moonEnvelope = moon.satellite.renderOrbitRadius() + moon.satellite.renderRadius;
            localRadius = Math.max(localRadius, moonEnvelope);
        }
        return Math.max(PLANET_FOCUS_MIN_RADIUS, localRadius * PLANET_FOCUS_MARGIN);
    }

    private Vec3 moonRenderPosition(Vec3 planetPosition, NaturalSatellite satellite, double days) {
        double direction = satellite.periodDays < 0 ? -1.0 : 1.0;
        double period = Math.max(0.05, Math.abs(satellite.periodDays));
        double angle = satellite.initialAngleRadians + direction * days * Math.PI * 2.0 / period;
        double radius = satellite.renderOrbitRadius();
        double y = Math.sin(angle) * radius * Math.sin(satellite.inclinationRadians);
        Vec3 local = new Vec3(
            Math.cos(angle) * radius,
            y,
            Math.sin(angle) * radius * Math.cos(satellite.inclinationRadians)
        );
        return planetPosition.add(local);
    }

    private Mat4 planetRotation(Planet planet, double spinRadians) {
        return Mat4.rotationX(planet.axialTiltRadians).mul(Mat4.rotationY(spinRadians));
    }

    private double planetSpin(Planet planet) {
        return TWO_PI * simulatedDays / planet.rotationPeriodDays;
    }

    private Vec3 toRenderPosition(Vec3 positionAu) {
        double radius = positionAu.length();
        if (radius < 1e-9) return Vec3.ZERO;
        double compressed = 1.35 + Math.log1p(radius) * 4.25;
        return positionAu.normalize().mul(compressed);
    }

    private double auPerDayToKmPerSecond(double velocity) {
        return velocity * 149_597_870.7 / 86_400.0;
    }

    private double normalizeAngle(double angle) {
        double normalized = angle % TWO_PI;
        if (normalized > Math.PI) normalized -= TWO_PI;
        if (normalized < -Math.PI) normalized += TWO_PI;
        return normalized;
    }

    private static double deg(double degrees) {
        return Math.toRadians(degrees);
    }

    private double strength() {
        return parameters.getValue("intensity", 70.0) / 100.0;
    }

    private Planet selectedPlanet() {
        return Planet.byIndex(parameters.getValue("planet", Planet.EARTH.ordinal()));
    }

    private Situation situation() {
        return Situation.byIndex(parameters.getValue("situation", Situation.STABLE.ordinal()));
    }

    private CameraFocus cameraFocus() {
        return CameraFocus.byIndex(parameters.getValue("camera_focus", CameraFocus.SYSTEM.ordinal()));
    }

    private static final class PlanetBody {
        private final Planet planet;
        private final SceneObject object;
        private final List<Vec3> trailAu = new ArrayList<>();
        private final List<SceneObject> trailObjects = new ArrayList<>();
        private final List<MoonBody> moons = new ArrayList<>();
        private Vec3 positionAu;
        private Vec3 velocityAuDay;
        private SceneObject ring;
        private double spinRadians;
        private double trailClock;
        private boolean capturedBySun;

        private PlanetBody(Planet planet, SceneObject object, Vec3 positionAu, Vec3 velocityAuDay) {
            this.planet = planet;
            this.object = object;
            this.positionAu = positionAu;
            this.velocityAuDay = velocityAuDay;
        }
    }

    private record MoonBody(NaturalSatellite satellite, SceneObject object) {}

    private record OrbitalVectors(Vec3 positionAu, Vec3 velocityAuDay) {}

    private record OrbitState(double eccentricity, String description) {}

    private enum CameraFocus {
        SYSTEM("Sistema inteiro"),
        PLANET("Planeta observado");

        private final String label;

        CameraFocus(String label) {
            this.label = label;
        }

        private static String[] labels() {
            String[] labels = new String[values().length];
            for (int i = 0; i < values().length; i++) labels[i] = values()[i].label;
            return labels;
        }

        private static CameraFocus byIndex(double value) {
            int index = Math.max(0, Math.min(values().length - 1, (int)Math.round(value)));
            return values()[index];
        }
    }

    private enum Situation {
        STABLE("Orbita base"),
        STRONGER_SUN("Sol mais massivo"),
        WEAKER_SUN("Sol menos massivo"),
        ACCELERATE_PLANET("Acelerar planeta"),
        BRAKE_PLANET("Frear planeta"),
        RADIAL_IMPACT("Impacto radial");

        private final String label;

        Situation(String label) {
            this.label = label;
        }

        private double solarMassFactor(double strength) {
            return switch (this) {
                case STRONGER_SUN -> 1.0 + 0.85 * strength;
                case WEAKER_SUN -> 1.0 - 0.65 * strength;
                default -> 1.0;
            };
        }

        private double tangentialFactor(double strength) {
            return switch (this) {
                case ACCELERATE_PLANET -> 1.0 + 0.58 * strength;
                case BRAKE_PLANET -> 1.0 - 0.52 * strength;
                default -> 1.0;
            };
        }

        private double radialKickAuDay(double strength) {
            return this == RADIAL_IMPACT ? 0.018 * strength : 0.0;
        }

        private static String[] labels() {
            String[] labels = new String[values().length];
            for (int i = 0; i < values().length; i++) labels[i] = values()[i].label;
            return labels;
        }

        private static Situation byIndex(double value) {
            int index = Math.max(0, Math.min(values().length - 1, (int)Math.round(value)));
            return values()[index];
        }
    }

    private enum NaturalSatellite {
        MOON(Planet.EARTH, "Lua", 384_400, 27.322, 0.095, 0.089, 0.20, "#cfd2d6"),
        PHOBOS(Planet.MARS, "Phobos", 9_376, 0.319, 0.035, 0.019, 1.18, "#b6a491"),
        DEIMOS(Planet.MARS, "Deimos", 23_463, 1.263, 0.030, 0.031, 3.55, "#998b7a"),

        IO(Planet.JUPITER, "Io", 421_800, 1.769, 0.072, 0.001, 0.35, "#dbc567"),
        EUROPA(Planet.JUPITER, "Europa", 671_100, 3.551, 0.068, 0.008, 1.42, "#c4b399"),
        GANYMEDE(Planet.JUPITER, "Ganymede", 1_070_400, 7.155, 0.088, 0.003, 2.36, "#a99b83"),
        CALLISTO(Planet.JUPITER, "Callisto", 1_882_700, 16.689, 0.082, 0.003, 4.02, "#7e7164"),

        MIMAS(Planet.SATURN, "Mimas", 186_000, 0.942, 0.040, 0.027, 0.45, "#d8d4cb"),
        ENCELADUS(Planet.SATURN, "Enceladus", 238_400, 1.370, 0.046, 0.000, 1.10, "#e6e5dc"),
        TETHYS(Planet.SATURN, "Tethys", 295_000, 1.888, 0.052, 0.019, 1.85, "#c8c2b6"),
        DIONE(Planet.SATURN, "Dione", 377_700, 2.737, 0.054, 0.000, 2.60, "#b8aea3"),
        RHEA(Planet.SATURN, "Rhea", 527_200, 4.518, 0.060, 0.005, 3.20, "#cbc4b8"),
        TITAN(Planet.SATURN, "Titan", 1_221_900, 15.945, 0.098, 0.005, 4.75, "#d49b52"),

        MIRANDA(Planet.URANUS, "Miranda", 129_900, 1.413, 0.040, 0.075, 0.70, "#bbb8b7"),
        ARIEL(Planet.URANUS, "Ariel", 191_000, 2.520, 0.052, 0.001, 1.62, "#d2d0cf"),
        UMBRIEL(Planet.URANUS, "Umbriel", 266_000, 4.144, 0.052, 0.002, 2.58, "#8c8889"),
        TITANIA(Planet.URANUS, "Titania", 436_300, 8.706, 0.064, 0.001, 3.45, "#b9b4ad"),
        OBERON(Planet.URANUS, "Oberon", 583_500, 13.463, 0.062, 0.001, 4.55, "#9f978f"),

        PROTEUS(Planet.NEPTUNE, "Proteus", 117_600, 1.122, 0.042, 0.001, 1.00, "#928e8a"),
        TRITON(Planet.NEPTUNE, "Triton", 354_800, -5.877, 0.074, 2.74, 3.15, "#c5b7a5");

        private final Planet parent;
        private final String label;
        private final double semiMajorAxisKm;
        private final double periodDays;
        private final double renderRadius;
        private final double inclinationRadians;
        private final double initialAngleRadians;
        private final ColorRGBA color;

        NaturalSatellite(Planet parent, String label, double semiMajorAxisKm, double periodDays,
                         double renderRadius, double inclinationRadians, double initialAngleRadians, String colorHex) {
            this.parent = parent;
            this.label = label;
            this.semiMajorAxisKm = semiMajorAxisKm;
            this.periodDays = periodDays;
            this.renderRadius = renderRadius;
            this.inclinationRadians = inclinationRadians;
            this.initialAngleRadians = initialAngleRadians;
            this.color = ColorRGBA.fromHex(colorHex);
        }

        private double renderOrbitRadius() {
            double compressedDistance = Math.log1p(semiMajorAxisKm / 100_000.0) * 0.29;
            return parent.renderRadius * 1.48 + Math.max(0.12, compressedDistance);
        }
    }

    private enum Planet {
        MERCURY("Mercurio", 0.38709927, 87.9691, 0.20563593, deg(7.00497902),
            deg(252.25032350), deg(77.45779628), deg(48.33076593),
            0.0553, 0.383, 3.70, 0.18, "#a9a39a",
            58.6462, deg(0.034), false, "/textures/planets/mercury.jpg"),
        VENUS("Venus", 0.72333566, 224.701, 0.00677672, deg(3.39467605),
            deg(181.97909950), deg(131.60246718), deg(76.67984255),
            0.815, 0.949, 8.87, 0.26, "#d7a567",
            -243.018, deg(177.36), false, "/textures/planets/venus.jpg"),
        EARTH("Terra", 1.00000261, 365.256, 0.01671123, deg(-0.00001531),
            deg(100.46457166), deg(102.93768193), deg(0.0),
            1.000, 1.000, 9.81, 0.27, "#4da6ff",
            0.99726968, deg(23.439), false, "/textures/planets/earth.jpg"),
        MARS("Marte", 1.52371034, 686.980, 0.09339410, deg(1.84969142),
            deg(-4.55343205), deg(-23.94362959), deg(49.55953891),
            0.107, 0.532, 3.71, 0.21, "#d7644c",
            1.02595676, deg(25.19), false, "/textures/planets/mars.jpg"),
        JUPITER("Jupiter", 5.20288700, 4332.589, 0.04838624, deg(1.30439695),
            deg(34.39644051), deg(14.72847983), deg(100.47390909),
            317.83, 11.21, 24.79, 0.62, "#d7b18c",
            0.41354, deg(3.13), false, "/textures/planets/jupiter.jpg"),
        SATURN("Saturno", 9.53667594, 10759.22, 0.05386179, deg(2.48599187),
            deg(49.95424423), deg(92.59887831), deg(113.66242448),
            95.16, 9.45, 10.44, 0.54, "#d2bf83",
            0.44401, deg(26.73), true, "/textures/planets/saturn.jpg"),
        URANUS("Urano", 19.18916464, 30688.5, 0.04725744, deg(0.77263783),
            deg(313.23810451), deg(170.95427630), deg(74.01692503),
            14.54, 4.01, 8.69, 0.39, "#7ad7df",
            -0.71833, deg(97.77), true, "/textures/planets/uranus.jpg"),
        NEPTUNE("Netuno", 30.06992276, 60182.0, 0.00859048, deg(1.77004347),
            deg(-55.12002969), deg(44.96476227), deg(131.78422574),
            17.15, 3.88, 11.15, 0.39, "#4c72e6",
            0.67125, deg(28.32), false, "/textures/planets/neptune.jpg");

        private final String label;
        private final double orbitAu;
        private final double periodDays;
        private final double orbitalEccentricity;
        private final double orbitalInclinationRadians;
        private final double meanLongitudeRadians;
        private final double longitudePerihelionRadians;
        private final double longitudeAscendingNodeRadians;
        private final double massEarth;
        private final double radiusEarth;
        private final double surfaceGravityMs2;
        private final double renderRadius;
        private final ColorRGBA color;
        private final double rotationPeriodDays;
        private final double axialTiltRadians;
        private final boolean hasRings;
        private final String texturePath;

        Planet(String label, double orbitAu, double periodDays, double orbitalEccentricity,
               double orbitalInclinationRadians, double meanLongitudeRadians,
               double longitudePerihelionRadians, double longitudeAscendingNodeRadians,
               double massEarth, double radiusEarth, double surfaceGravityMs2, double renderRadius,
               String colorHex,
               double rotationPeriodDays, double axialTiltRadians, boolean hasRings, String texturePath) {
            this.label = label;
            this.orbitAu = orbitAu;
            this.periodDays = periodDays;
            this.orbitalEccentricity = orbitalEccentricity;
            this.orbitalInclinationRadians = orbitalInclinationRadians;
            this.meanLongitudeRadians = meanLongitudeRadians;
            this.longitudePerihelionRadians = longitudePerihelionRadians;
            this.longitudeAscendingNodeRadians = longitudeAscendingNodeRadians;
            this.massEarth = massEarth;
            this.radiusEarth = radiusEarth;
            this.surfaceGravityMs2 = surfaceGravityMs2;
            this.renderRadius = renderRadius;
            this.color = ColorRGBA.fromHex(colorHex);
            this.rotationPeriodDays = rotationPeriodDays;
            this.axialTiltRadians = axialTiltRadians;
            this.hasRings = hasRings;
            this.texturePath = texturePath;
        }

        private double trailCadenceDays() {
            return Math.max(0.5, Math.min(120.0, periodDays / 72.0));
        }

        private static String[] labels() {
            String[] labels = new String[values().length];
            for (int i = 0; i < values().length; i++) labels[i] = values()[i].label;
            return labels;
        }

        private static Planet byIndex(double value) {
            int index = Math.max(0, Math.min(values().length - 1, (int)Math.round(value)));
            return values()[index];
        }
    }
}
