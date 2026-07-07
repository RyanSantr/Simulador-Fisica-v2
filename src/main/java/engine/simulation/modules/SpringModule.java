package engine.simulation.modules;

import engine.math.ColorRGBA;
import engine.math.Vec3;
import engine.scene.SceneObject;
import engine.simulation.LabMeasurement;
import engine.simulation.ModuleSceneBuilder;
import engine.simulation.SimulationModule;
import engine.simulation.parameters.Parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Oscilador massa-mola horizontal.
 */
public class SpringModule extends SimulationModule {

    private final List<SceneObject> springMarkers = new ArrayList<>();
    private SceneObject mass;
    private Vec3 anchor;
    private double displacement;
    private double velocity;
    private double acceleration;

    public SpringModule() {
        super("spring", "Mola",
            "Explore a lei de Hooke e a energia de um oscilador massa-mola.",
            "Mecanica - Oscilacoes");
    }

    @Override
    protected void declareParameters() {
        parameters.add(Parameter.of("mass", "Massa", 1.2)
            .range(0.1, 8).unit("kg").step(0.1));
        parameters.add(Parameter.of("stiffness", "Constante k", 18)
            .range(0.5, 80).unit("N/m").step(0.5));
        parameters.add(Parameter.of("amplitude", "Alongamento inicial", 2.0)
            .range(0.1, 5).unit("m").step(0.1));
        parameters.add(Parameter.of("damping", "Amortecimento", 0.16)
            .range(0, 2).unit("N.s/m").step(0.02));
    }

    @Override
    protected void buildScene() {
        ModuleSceneBuilder builder = new ModuleSceneBuilder(context);
        context.setGravityStrength(0);
        anchor = new Vec3(-5.2, -1.1, 0);
        displacement = parameters.getValue("amplitude", 2.0);
        velocity = 0;
        acceleration = 0;

        builder.box(0.25, 3.2, 2.2, ColorRGBA.fromHex("#475569"))
            .at(anchor.add(new Vec3(-0.2, 0, 0)))
            .isStatic().named("parede_mola").add();
        builder.box(11.2, 0.10, 1.55, ColorRGBA.fromHex("#17212f"))
            .at(anchor.x + 4.8, anchor.y - 0.64, 0)
            .isStatic().named("base_mola").add();
        builder.box(10.4, 0.06, 0.06, new ColorRGBA(0.34, 0.78, 0.84, 0.50))
            .at(anchor.x + 4.7, anchor.y - 0.56, -0.72)
            .isStatic().named("trilho_mola_a").add();
        builder.box(10.4, 0.06, 0.06, new ColorRGBA(0.34, 0.78, 0.84, 0.50))
            .at(anchor.x + 4.7, anchor.y - 0.56, 0.72)
            .isStatic().named("trilho_mola_b").add();
        builder.box(0.06, 2.0, 0.06, new ColorRGBA(1.0, 0.85, 0.35, 0.68))
            .at(anchor.x + 3.0, anchor.y, -0.95)
            .isStatic().named("equilibrio_mola").add().setShadowCaster(false);
        mass = builder.box(1.0, 1.0, 1.0, ColorRGBA.fromHex("#fb7185"))
            .at(massPosition()).isStatic().named("massa_mola").add();
        mass.setRenderLayer(1);
        springMarkers.clear();
        for (int i = 0; i < 14; i++) {
            springMarkers.add(builder.sphere(0.08, ColorRGBA.fromHex("#fde68a"))
                .at(anchor).isStatic().named("espira_" + i).add());
        }
        updateVisuals();
    }

    @Override
    public void onUpdate(double dt) {
        if (mass == null) return;
        double massKg = parameters.getValue("mass", 1.2);
        double k = parameters.getValue("stiffness", 18);
        double damping = parameters.getValue("damping", 0.16);
        acceleration = (-k * displacement - damping * velocity) / massKg;
        velocity += acceleration * dt;
        displacement += velocity * dt;
        updateVisuals();
    }

    @Override
    public void onParameterChanged(String paramName, double newValue) {
        onReset();
    }

    public String telemetry() {
        double k = parameters.getValue("stiffness", 18);
        double massKg = parameters.getValue("mass", 1.2);
        double energy = 0.5 * k * displacement * displacement + 0.5 * massKg * velocity * velocity;
        return String.format(
            "Massa-mola%nLei de Hooke: F = -k.x%nEquacao: a = (-k.x - c.v)/m%n%nDeslocamento: %.2f m%nVelocidade: %.2f m/s%nAceleracao: %.2f m/s2%nEnergia mecanica: %.2f J",
            displacement, velocity, acceleration, energy
        );
    }

    private void updateVisuals() {
        Vec3 pos = massPosition();
        mass.getBody().setPosition(pos);
        mass.getBody().setVelocity(new Vec3(velocity, 0, 0));
        for (int i = 0; i < springMarkers.size(); i++) {
            double t = (i + 1.0) / (springMarkers.size() + 1.0);
            double zig = (i % 2 == 0 ? 1 : -1) * 0.18;
            springMarkers.get(i).getBody().setPosition(new Vec3(
                anchor.x + (pos.x - anchor.x - 0.55) * t,
                anchor.y + zig,
                zig * 0.35
            ));
        }
    }

    private Vec3 massPosition() {
        return new Vec3(anchor.x + 3.0 + displacement, anchor.y, 0);
    }

    @Override
    public LabMeasurement sampleLabMeasurement() {
        return new LabMeasurement(
            "mola",
            "deslocamento", "m", displacement,
            "velocidade", "m/s", velocity,
            String.format("x=%.2f m | v=%.2f m/s | a=%.2f m/s2",
                displacement, velocity, acceleration)
        );
    }
}
