package engine.simulation.parameters;

import java.util.*;
import java.util.function.Consumer;

/**
 * Parâmetro numérico ajustável de um módulo de simulação.
 *
 * Suporta:
 *   - Valor atual, mínimo, máximo e padrão
 *   - Unidade de exibição (ex: "m/s²", "°", "kg")
 *   - Callback on-change
 *   - Validação automática de range
 *
 * Exemplo de declaração num módulo:
 * <pre>
 *   parameters.add(Parameter.of("gravity", "Gravidade", 9.8)
 *       .range(0, 25).unit("m/s²").step(0.1));
 * </pre>
 */
public final class Parameter {

    // ── Metadados ──────────────────────────────────
    private final String name;
    private final String label;
    private final String unit;
    private final String tooltip;
    private final List<String> options;

    // ── Valores ────────────────────────────────────
    private final double defaultValue;
    private final double minValue;
    private final double maxValue;
    private final double step;
    private       double currentValue;

    // ── Callback ───────────────────────────────────
    private Consumer<Double> onChange;

    // ── Construtor privado (usar builder estático) ─
    private Parameter(Builder b) {
        this.name         = b.name;
        this.label        = b.label;
        this.unit         = b.unit;
        this.tooltip      = b.tooltip;
        this.options      = List.copyOf(b.options);
        this.defaultValue = b.defaultValue;
        this.minValue     = b.minValue;
        this.maxValue     = b.maxValue;
        this.step         = b.step;
        this.currentValue = b.defaultValue;
    }

    // ── Builder fluente ────────────────────────────
    public static Builder of(String name, String label, double defaultValue) {
        return new Builder(name, label, defaultValue);
    }

    public static final class Builder {
        final String name, label;
        final double defaultValue;
        double minValue = 0, maxValue = 100, step = 0.5;
        String unit = "", tooltip = "";
        List<String> options = List.of();

        Builder(String name, String label, double defaultValue) {
            this.name = name; this.label = label; this.defaultValue = defaultValue;
        }

        public Builder range(double min, double max) { this.minValue = min; this.maxValue = max; return this; }
        public Builder unit(String unit)             { this.unit = unit;    return this; }
        public Builder step(double step)             { this.step = step;    return this; }
        public Builder tooltip(String tip)           { this.tooltip = tip;  return this; }
        public Builder options(String... labels) {
            this.options = List.of(labels);
            this.minValue = 0;
            this.maxValue = Math.max(0, labels.length - 1);
            this.step = 1;
            return this;
        }
        public Parameter build()                     { return new Parameter(this); }
    }

    // ── API pública ────────────────────────────────

    public void setValue(double value) {
        double clamped = Math.max(minValue, Math.min(maxValue, value));
        if (Math.abs(clamped - currentValue) < 1e-10) return;
        this.currentValue = clamped;
        if (onChange != null) onChange.accept(clamped);
    }

    public void resetToDefault() { setValue(defaultValue); }

    public void setOnChange(Consumer<Double> cb) { this.onChange = cb; }

    // ── Getters ────────────────────────────────────
    public String getName()         { return name; }
    public String getLabel()        { return label; }
    public String getUnit()         { return unit; }
    public String getTooltip()      { return tooltip; }
    public double getValue()        { return currentValue; }
    public double getDefaultValue() { return defaultValue; }
    public double getMinValue()     { return minValue; }
    public double getMaxValue()     { return maxValue; }
    public double getStep()         { return step; }
    public List<String> getOptions(){ return options; }
    public boolean hasOptions()     { return !options.isEmpty(); }
    public int getOptionIndex()     { return (int)Math.round(currentValue); }
    public String getSelectedOption() {
        if (options.isEmpty()) return "";
        int idx = Math.max(0, Math.min(options.size() - 1, getOptionIndex()));
        return options.get(idx);
    }

    /** Retorna o valor normalizado [0..1] dentro do range. */
    public double getNormalized() {
        double range = maxValue - minValue;
        return range < 1e-10 ? 0 : (currentValue - minValue) / range;
    }

    @Override
    public String toString() {
        return String.format("Param[%s=%.2f %s (%.1f..%.1f)]",
            name, currentValue, unit, minValue, maxValue);
    }
}
