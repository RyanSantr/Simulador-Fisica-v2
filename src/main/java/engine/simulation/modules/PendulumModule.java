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
 * Pendulo simples integrado pela equacao angular nao linear.
 */
public class PendulumModule extends SimulationModule {

    private final List<SceneObject> ropeMarkers = new ArrayList<>();
    private SceneObject bob;
    private Vec3 pivot;
    private double theta;
    private double angularVelocity;
    private double angularAcceleration;

    public PendulumModule() {
        super("pendulum", "Pendulo",
            "Veja periodo, amplitude e amortecimento de um pendulo simples.",
            "Mecanica - Oscilacoes");
    }

    @Override
    protected void declareParameters() {
        parameters.add(Parameter.of("length", "Comprimento", 4.2)
            .range(0.8, 8).unit("m").step(0.1));
        parameters.add(Parameter.of("amplitude", "Amplitude inicial", 34)
            .range(2, 80).unit("graus").step(1));
        parameters.add(Parameter.of("gravity", "Gravidade", 9.81)
            .range(0, 30).unit("m/s2").step(0.1));
        parameters.add(Parameter.of("damping", "Amortecimento", 0.035)
            .range(0, 0.5).unit("1/s").step(0.005));
    }

    @Override
    protected void buildScene() {
        ModuleSceneBuilder builder = new ModuleSceneBuilder(context);
        context.setGravityStrength(0);
        pivot = new Vec3(0, 3.9, 0);
        theta = Math.toRadians(parameters.getValue("amplitude", 34));
        angularVelocity = 0;
        angularAcceleration = 0;
        double length = parameters.getValue("length", 4.2);
        double amplitude = Math.toRadians(parameters.getValue("amplitude", 34));
        double frameHalfWidth = Math.max(2.8, length * Math.sin(amplitude) + 0.92);
        double floorY = context.getPhysics().getFloorY();
        double beamY = pivot.y + 0.78;
        double postHeight = beamY - floorY;

        builder.sphere(0.18, ColorRGBA.fromHex("#e2e8f0"))
            .at(pivot).isStatic().named("pivo_pendulo").add();
        builder.box(frameHalfWidth * 2.0 + 0.36, 0.18, 0.38, ColorRGBA.fromHex("#334155"))
            .at(0, beamY, -0.72).isStatic().named("viga_pendulo").add();
        builder.box(0.10, beamY - pivot.y, 0.10, ColorRGBA.fromHex("#cbd5e1"))
            .at(0, pivot.y + (beamY - pivot.y) * 0.5, -0.18)
            .isStatic().named("suporte_pivo").add();
        builder.box(0.18, postHeight, 0.18, ColorRGBA.fromHex("#263446"))
            .at(-frameHalfWidth, floorY + postHeight * 0.5, -0.72)
            .isStatic().named("poste_pendulo_a").add();
        builder.box(0.18, postHeight, 0.18, ColorRGBA.fromHex("#263446"))
            .at(frameHalfWidth, floorY + postHeight * 0.5, -0.72)
            .isStatic().named("poste_pendulo_b").add();
        bob = builder.sphere(0.48, ColorRGBA.fromHex("#38bdf8"))
            .at(bobPosition()).isStatic().named("massa_pendulo").add();
        ropeMarkers.clear();
        for (int i = 1; i <= 10; i++) {
            ropeMarkers.add(builder.sphere(0.045, ColorRGBA.fromHex("#cbd5e1"))
                .at(pivot).isStatic().named("fio_" + i).add());
        }
        for (int i = 0; i <= 10; i++) {
            double markerAngle = -amplitude + amplitude * 2.0 * i / 10.0;
            Vec3 markerPos = pivot.add(new Vec3(length * Math.sin(markerAngle), -length * Math.cos(markerAngle), 0));
            builder.sphere(0.045, new ColorRGBA(0.98, 0.81, 0.31, 0.72))
                .at(markerPos.add(new Vec3(0, -0.02, -0.62)))
                .isStatic()
                .named("arco_pendulo_" + i)
                .add()
                .setShadowCaster(false);
        }
        updateVisuals();
    }

    @Override
    public void onUpdate(double dt) {
        if (bob == null) return;
        double length = parameters.getValue("length", 4.2);
        double gravity = context.getPhysics().isGravityEnabled()
            ? parameters.getValue("gravity", 9.81)
            : 0;
        double damping = parameters.getValue("damping", 0.035);
        angularAcceleration = -(gravity / length) * Math.sin(theta) - damping * angularVelocity;
        angularVelocity += angularAcceleration * dt;
        theta += angularVelocity * dt;
        updateVisuals();
    }

    @Override
    public void onParameterChanged(String paramName, double newValue) {
        onReset();
    }

    public String telemetry() {
        double length = parameters.getValue("length", 4.2);
        double gravity = parameters.getValue("gravity", 9.81);
        double period = gravity <= 0 ? 0 : 2 * Math.PI * Math.sqrt(length / gravity);
        return String.format(
            "Pendulo simples%nEquacao: theta'' = -(g/L).sen(theta)%n%nAngulo: %.2f graus%nVelocidade angular: %.2f rad/s%nAceleracao angular: %.2f rad/s2%nPeriodo pequeno angulo: %.2f s",
            Math.toDegrees(theta), angularVelocity, angularAcceleration, period
        );
    }

    private void updateVisuals() {
        Vec3 pos = bobPosition();
        bob.getBody().setPosition(pos);
        bob.getBody().setVelocity(new Vec3(
            Math.cos(theta) * parameters.getValue("length", 4.2) * angularVelocity,
            Math.sin(theta) * parameters.getValue("length", 4.2) * angularVelocity,
            0
        ));
        for (int i = 0; i < ropeMarkers.size(); i++) {
            double t = (i + 1.0) / (ropeMarkers.size() + 1.0);
            ropeMarkers.get(i).getBody().setPosition(pivot.add(pos.sub(pivot).mul(t)));
        }
    }

    private Vec3 bobPosition() {
        double length = parameters.getValue("length", 4.2);
        return pivot.add(new Vec3(length * Math.sin(theta), -length * Math.cos(theta), 0));
    }

    @Override
    public LabMeasurement sampleLabMeasurement() {
        return new LabMeasurement(
            "pendulo",
            "angulo", "graus", Math.toDegrees(theta),
            "vel.angular", "rad/s", angularVelocity,
            String.format("theta=%.2f graus | omega=%.2f rad/s | alpha=%.2f rad/s2",
                Math.toDegrees(theta), angularVelocity, angularAcceleration)
        );
    }
}
