package engine.ui.simulation;

import engine.simulation.LabPreset;
import engine.simulation.LabPresetCatalog;
import engine.simulation.ModuleManager;
import engine.simulation.SimulationModule;
import engine.simulation.parameters.Parameter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleConsumer;

/**
 * Painel lateral de controle de módulos educacionais.
 *
 * Exibe:
 *   - Seletor de módulo (abas ou lista)
 *   - Controles de parâmetros (sliders + campos de valor)
 *   - Botões de ação (Lançar, Reset, Próximo desafio)
 *   - Painel de telemetria/informação do módulo ativo
 */
public class ModulePanel extends VBox {

    /** Largura minima do painel; fonte unica reutilizada pelo layout responsivo do Engine. */
    public static final double MIN_WIDTH = 215;

    private static final String STYLE_DARK  = "-fx-background-color: #111827;";
    private static final String STYLE_TITLE = "-fx-text-fill: #e0f2fe; -fx-font-family: monospace; -fx-font-weight: bold;";
    private static final String STYLE_BTN   =
        "-fx-background-color: #1f2937; -fx-text-fill: #e5e7eb; -fx-border-color: #475569; " +
        "-fx-border-radius: 6; -fx-background-radius: 6; -fx-font-family: monospace; " +
        "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 9 12;";
    private static final String STYLE_BTN_ACTIVE =
        "-fx-background-color: #2563eb; -fx-text-fill: #ffffff; -fx-border-color: #60a5fa; " +
        "-fx-border-radius: 6; -fx-background-radius: 6; -fx-font-family: monospace; " +
        "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 9 12;";

    // ── Referências ────────────────────────────────
    private final ModuleManager manager;

    // ── Subpainéis ─────────────────────────────────
    private final VBox moduleButtonsPane = new VBox(7);
    private final VBox parametersPane    = new VBox(6);
    private final VBox infoPane          = new VBox(4);
    private final Label lblModuleName    = new Label("Nenhum módulo");
    private final Label lblModuleDesc    = new Label("");
    private final Label lblCurriculum    = new Label("");
    private final Label lblTelemetry     = new Label("");

    // Botões de ação do módulo
    private Button btnReset;
    private Button btnLaunch;
    private Button btnNextChallenge;
    private Button btnPause;
    private Button btnStep;
    private Button btnExportCsv;
    private ComboBox<String> cbTimeScale;
    private ComboBox<LabPreset> cbPreset;
    private Label lblLabClock;
    private Label lblPresetNote;

    // Mapa de controles por nome de parâmetro
    private final Map<String, Slider> paramSliders = new HashMap<>();
    private final Map<String, Label>  paramValues  = new HashMap<>();
    private final Map<String, TextField> paramInputs = new HashMap<>();
    private boolean applyingTypedValue = false;

    // Callbacks para ações
    private Runnable onResetRequested;
    private Runnable onLaunchRequested;
    private Runnable onPauseRequested;
    private Runnable onStepRequested;
    private Runnable onExportRequested;
    private DoubleConsumer onTimeScaleChanged;

    public ModulePanel(ModuleManager manager) {
        super(0);
        this.manager = manager;
        setPrefWidth(320);
        setMinWidth(MIN_WIDTH);
        setStyle(STYLE_DARK + "-fx-border-color: #334155; -fx-border-width: 0 1 0 0;");
        setPadding(new Insets(0));

        buildLayout();
        refresh();
    }

    public void setPanelWidth(double width) {
        double usableWidth = Math.max(MIN_WIDTH, width);
        setPrefWidth(usableWidth);
        setMinWidth(Math.min(MIN_WIDTH, usableWidth));
        setMaxWidth(usableWidth);
    }

    // ══════════════════════════════════════════════
    //  LAYOUT
    // ══════════════════════════════════════════════

    private void buildLayout() {
        // ── Header ──────────────────────────────
        VBox header = new VBox(5);
        header.setPadding(new Insets(18, 18, 14, 18));
        header.setStyle("-fx-background-color: #0b1223; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");

        Label appLabel = new Label("SimulaFisica 3D");
        appLabel.setFont(Font.font("monospace", FontWeight.BOLD, 22));
        appLabel.setStyle("-fx-text-fill: #e0f2fe;");

        Label subLabel = new Label("Fisica Computacional | JavaFX | POO");
        subLabel.setFont(Font.font("monospace", 13));
        subLabel.setStyle("-fx-text-fill: #94a3b8;");

        header.getChildren().addAll(appLabel, subLabel);

        // ── Seletor de módulos ───────────────────
        VBox modulesSection = new VBox(8);
        modulesSection.setPadding(new Insets(14, 18, 12, 18));
        modulesSection.getChildren().add(sectionLabel("SIMULACOES"));
        ScrollPane moduleScroll = new ScrollPane(moduleButtonsPane);
        moduleScroll.setFitToWidth(true);
        moduleScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        moduleScroll.setPrefHeight(230);
        moduleScroll.setMinHeight(150);
        moduleScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        modulesSection.getChildren().add(moduleScroll);

        // ── Info do módulo ───────────────────────
        VBox moduleInfo = new VBox(6);
        moduleInfo.setPadding(new Insets(13, 18, 13, 18));
        moduleInfo.setStyle("-fx-background-color: #0d1a25; -fx-border-color: #334155; -fx-border-width: 1 0;");

        lblModuleName.setFont(Font.font("monospace", FontWeight.BOLD, 17));
        lblModuleName.setStyle("-fx-text-fill: #bfdbfe;");
        lblModuleName.setWrapText(true);

        lblModuleDesc.setFont(Font.font("monospace", 13));
        lblModuleDesc.setStyle("-fx-text-fill: #cbd5e1; -fx-wrap-text: true;");
        lblModuleDesc.setWrapText(true);

        lblCurriculum.setFont(Font.font("monospace", 12));
        lblCurriculum.setStyle("-fx-text-fill: #86efac;");

        moduleInfo.getChildren().addAll(lblModuleName, lblModuleDesc, lblCurriculum);

        // ── Parâmetros ───────────────────────────
        parametersPane.setPadding(new Insets(10, 18, 8, 18));

        // ── Ações ────────────────────────────────
        VBox actionsSection = new VBox(6);
        actionsSection.setPadding(new Insets(12, 18, 12, 18));

        btnReset = styledBtn("Resetar simulacao");
        btnLaunch = styledBtn("Executar lancamento");
        btnNextChallenge = styledBtn("Proximo desafio");

        btnReset.setMaxWidth(Double.MAX_VALUE);
        btnLaunch.setMaxWidth(Double.MAX_VALUE);
        btnNextChallenge.setMaxWidth(Double.MAX_VALUE);
        btnLaunch.setStyle(STYLE_BTN_ACTIVE);

        btnReset.setOnAction(e -> { if (onResetRequested != null) onResetRequested.run(); });
        btnLaunch.setOnAction(e -> { if (onLaunchRequested != null) onLaunchRequested.run(); });
        btnNextChallenge.setOnAction(e -> {
            if (manager.hasActiveModule()) manager.getActiveModule().advanceChallenge();
            refresh();
        });

        actionsSection.getChildren().addAll(
            sectionLabel("ACOES"),
            btnReset, btnLaunch, btnNextChallenge
        );

        VBox labSection = new VBox(7);
        labSection.setPadding(new Insets(12, 18, 12, 18));
        lblLabClock = new Label("Tempo simulado: 0.00 s");
        lblLabClock.setFont(Font.font("monospace", 12));
        lblLabClock.setStyle("-fx-text-fill: #bfdbfe;");

        btnPause = styledBtn("Pausar");
        btnStep = styledBtn("Passo unico");
        btnExportCsv = styledBtn("Exportar CSV");
        btnStep.setTooltip(new Tooltip("Avanca 1/120 s quando a simulacao esta pausada."));
        btnExportCsv.setTooltip(new Tooltip("Salva as amostras do grafico em CSV."));
        btnPause.setOnAction(e -> { if (onPauseRequested != null) onPauseRequested.run(); });
        btnStep.setOnAction(e -> { if (onStepRequested != null) onStepRequested.run(); });
        btnExportCsv.setOnAction(e -> { if (onExportRequested != null) onExportRequested.run(); });

        HBox playbackButtons = new HBox(7, btnPause, btnStep);
        HBox.setHgrow(btnPause, Priority.ALWAYS);
        HBox.setHgrow(btnStep, Priority.ALWAYS);
        btnPause.setMaxWidth(Double.MAX_VALUE);
        btnStep.setMaxWidth(Double.MAX_VALUE);

        cbTimeScale = new ComboBox<>();
        cbTimeScale.getItems().addAll("0.25x", "0.50x", "1.00x", "2.00x", "4.00x");
        cbTimeScale.setValue("1.00x");
        cbTimeScale.setMaxWidth(Double.MAX_VALUE);
        cbTimeScale.setMinHeight(38);
        cbTimeScale.setStyle("-fx-background-color: #1f2937; -fx-text-fill: #e5e7eb; " +
            "-fx-border-color: #475569; -fx-font-family: monospace; -fx-font-size: 13px;");
        cbTimeScale.setOnAction(e -> {
            if (onTimeScaleChanged != null && cbTimeScale.getValue() != null) {
                onTimeScaleChanged.accept(Double.parseDouble(cbTimeScale.getValue().replace("x", "")));
            }
        });

        cbPreset = new ComboBox<>();
        cbPreset.setPromptText("Preset experimental");
        cbPreset.setMaxWidth(Double.MAX_VALUE);
        cbPreset.setMinHeight(38);
        cbPreset.setStyle("-fx-background-color: #1f2937; -fx-text-fill: #e5e7eb; " +
            "-fx-border-color: #475569; -fx-font-family: monospace; -fx-font-size: 13px;");
        cbPreset.setOnAction(e -> applySelectedPreset());
        lblPresetNote = new Label("Presets repetem configuracoes para comparacao.");
        lblPresetNote.setWrapText(true);
        lblPresetNote.setFont(Font.font("monospace", 11));
        lblPresetNote.setStyle("-fx-text-fill: #94a3b8;");

        labSection.getChildren().addAll(
            sectionLabel("MODO LABORATORIO"),
            lblLabClock,
            playbackButtons,
            new VBox(4, compactLabel("Escala do tempo"), cbTimeScale),
            new VBox(4, compactLabel("Preset"), cbPreset, lblPresetNote),
            btnExportCsv
        );

        // ── Telemetria ───────────────────────────
        infoPane.setPadding(new Insets(12, 18, 14, 18));
        infoPane.getChildren().add(sectionLabel("RESULTADOS"));
        lblTelemetry.setFont(Font.font("monospace", 14));
        lblTelemetry.setStyle("-fx-text-fill: #e0f2fe; -fx-line-spacing: 4px; -fx-padding: 14;");
        lblTelemetry.setWrapText(true);
        VBox telemetryPlate = new VBox(lblTelemetry);
        telemetryPlate.setStyle("-fx-background-color: #020617; -fx-border-color: #334155; " +
            "-fx-border-radius: 7; -fx-background-radius: 7;");
        ScrollPane telemetryScroll = new ScrollPane(telemetryPlate);
        telemetryScroll.setFitToWidth(true);
        telemetryScroll.setPrefHeight(240);
        telemetryScroll.setMinHeight(150);
        telemetryScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        infoPane.getChildren().addAll(telemetryScroll, referencesLabel());

        // ── Montar ──────────────────────────────
        getChildren().addAll(
            header, modulesSection,
            sep(), moduleInfo,
            sep(),
            paramsSection(), sep(),
            actionsSection, sep(),
            labSection, sep(),
            infoPane
        );
    }

    private VBox paramsSection() {
        VBox v = new VBox(0);
        Label lbl = sectionLabel("PARAMETROS EXPERIMENTAIS");
        lbl.setPadding(new Insets(6, 10, 4, 10));
        v.getChildren().addAll(lbl, parametersPane);
        VBox.setVgrow(parametersPane, Priority.ALWAYS);
        return v;
    }

    // ══════════════════════════════════════════════
    //  REFRESH — sincroniza com módulo ativo
    // ══════════════════════════════════════════════

    /**
     * Atualiza o painel inteiro ao mudar de módulo ou estado.
     * Chamado pelo ModuleManager via callback.
     */
    public void refresh() {
        rebuildModuleButtons();
        rebuildParameters();
        rebuildPresets();
        updateModuleInfo();
    }

    private void rebuildModuleButtons() {
        moduleButtonsPane.getChildren().clear();
        for (SimulationModule mod : manager.getAllModules()) {
            Button btn = new Button(mod.getDisplayName());
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setMinHeight(56);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setWrapText(true);
            btn.setFont(Font.font("monospace", FontWeight.BOLD, 14));
            boolean active = manager.isActive(mod.getId());
            btn.setText(mod.getDisplayName() + "\n" + mod.getCurriculumTopic());
            btn.setStyle((active ? STYLE_BTN_ACTIVE : STYLE_BTN) +
                (active ? "-fx-border-width: 1 1 1 4;" : "-fx-border-width: 1;") +
                "-fx-text-alignment: left; -fx-line-spacing: 2px;");
            btn.setOnAction(e -> {
                manager.activateById(mod.getId());
                refresh();
            });
            btn.setTooltip(new Tooltip(mod.getDescription()));
            moduleButtonsPane.getChildren().add(btn);

        }
    }

    private void rebuildParameters() {
        parametersPane.getChildren().clear();
        paramSliders.clear();
        paramValues.clear();
        paramInputs.clear();

        SimulationModule mod = manager.getActiveModule();
        if (mod == null) return;

        for (Parameter param : mod.getParameters()) {
            VBox paramRow = buildParamRow(param, mod);
            parametersPane.getChildren().add(paramRow);
        }
    }

    private VBox buildParamRow(Parameter param, SimulationModule mod) {
        VBox row = new VBox(5);
        row.setPadding(new Insets(9, 10, 9, 10));
        row.setStyle("-fx-background-color: #0b1223; -fx-border-color: #253449; " +
            "-fx-border-radius: 7; -fx-background-radius: 7;");

        // Label com valor atual
        HBox labelRow = new HBox();
        labelRow.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(param.getLabel());
        lbl.setFont(Font.font("monospace", 13));
        lbl.setStyle("-fx-text-fill: #e5e7eb;");

        Label valLbl = new Label(formatParamValue(param));
        valLbl.setFont(Font.font("monospace", 13));
        valLbl.setStyle("-fx-text-fill: #7dd3fc;");
        HBox.setHgrow(lbl, Priority.ALWAYS);
        labelRow.getChildren().addAll(lbl, valLbl);

        if (param.hasOptions()) {
            ComboBox<String> combo = new ComboBox<>();
            combo.getItems().addAll(param.getOptions());
            combo.getSelectionModel().select(param.getOptionIndex());
            combo.setMaxWidth(Double.MAX_VALUE);
            combo.setMinHeight(34);
            combo.setStyle("-fx-background-color: #1f2937; -fx-text-fill: #e5e7eb; " +
                "-fx-border-color: #475569; -fx-font-family: monospace; -fx-font-size: 13px;");
            combo.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
                int selected = Math.max(0, newVal.intValue());
                param.setValue(selected);
                valLbl.setText(param.getSelectedOption());
                manager.notifyParameterChanged(param.getName(), selected);
            });

            if (!param.getTooltip().isEmpty()) {
                Tooltip.install(row, new Tooltip(param.getTooltip()));
            }

            row.getChildren().addAll(labelRow, combo);
            return row;
        }

        // Slider
        Slider slider = new Slider(param.getMinValue(), param.getMaxValue(), param.getValue());
        slider.setBlockIncrement(param.getStep());
        slider.setMajorTickUnit((param.getMaxValue() - param.getMinValue()) / 4.0);
        slider.setMinHeight(30);
        slider.setStyle("-fx-control-inner-background: #1f2937;");
        slider.setMaxWidth(Double.MAX_VALUE);

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (applyingTypedValue) return;
            double snapped = snapToStep(newVal.doubleValue(), param.getStep());
            param.setValue(snapped);
            valLbl.setText(formatParamValue(param));
            TextField input = paramInputs.get(param.getName());
            if (input != null && !input.isFocused()) {
                input.setText(formatPlainValue(param));
            }
            manager.notifyParameterChanged(param.getName(), param.getValue());
        });

        TextField input = numericInput(formatPlainValue(param));
        input.setTooltip(new Tooltip("Digite o valor real e pressione Enter. Use virgula ou ponto decimal."));
        input.setOnAction(e -> applyTypedValue(input, slider, param, valLbl));
        input.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) applyTypedValue(input, slider, param, valLbl);
        });

        HBox controlRow = new HBox(8, slider, input);
        controlRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(slider, Priority.ALWAYS);

        // Tooltip
        if (!param.getTooltip().isEmpty()) {
            Tooltip.install(row, new Tooltip(param.getTooltip()));
        }

        paramSliders.put(param.getName(), slider);
        paramValues.put(param.getName(), valLbl);
        paramInputs.put(param.getName(), input);

        row.getChildren().addAll(labelRow, controlRow);
        return row;
    }

    private void updateModuleInfo() {
        SimulationModule mod = manager.getActiveModule();
        if (mod == null) {
            lblModuleName.setText("Nenhum módulo ativo");
            lblModuleDesc.setText("");
            lblCurriculum.setText("");
            return;
        }
        lblModuleName.setText(mod.getDisplayName());
        lblModuleDesc.setText(mod.getDescription());
        lblCurriculum.setText("Topico: " + mod.getCurriculumTopic());

        // Mostrar info do desafio atual
        var challenge = mod.getCurrentChallenge();
        if (challenge != null) {
            lblTelemetry.setText(
                "Desafio " + (mod.getCurrentChallengeIndex() + 1) + "/" + mod.getChallengeCount()
                + "\n" + challenge.getTitle()
                + "\n\n" + challenge.getInstruction()
                + "\n\nDica: " + challenge.getHint()
            );
        } else {
            lblTelemetry.setText("Modo livre - explore os parametros.");
        }

        // Visibilidade do botão Lançar (apenas projéteis)
        boolean runnableModule = mod.getId().equals("projectile") || mod.getId().equals("collisions");
        btnLaunch.setVisible(runnableModule);
        btnLaunch.setManaged(runnableModule);
        btnLaunch.setText(mod.getId().equals("collisions") ? "Iniciar colisao" : "Executar lancamento");
        btnNextChallenge.setVisible(mod.hasMoreChallenges());
        btnNextChallenge.setManaged(mod.hasMoreChallenges());
    }

    private void rebuildPresets() {
        cbPreset.getItems().clear();
        SimulationModule mod = manager.getActiveModule();
        if (mod == null) return;

        cbPreset.getItems().addAll(LabPresetCatalog.forModule(mod.getId()));
        boolean hasPresets = !cbPreset.getItems().isEmpty();
        cbPreset.setDisable(!hasPresets);
        cbPreset.setPromptText(hasPresets ? "Escolha um preset" : "Sem preset para este modulo");
        lblPresetNote.setText(hasPresets
            ? "Presets repetem configuracoes para comparacao."
            : "Este modulo usa o estado atual da cena.");
    }

    private void applySelectedPreset() {
        LabPreset preset = cbPreset.getValue();
        SimulationModule mod = manager.getActiveModule();
        if (preset == null || mod == null) return;

        preset.values().forEach((name, value) -> mod.getParameters().setValue(name, value));
        manager.resetActive();
        rebuildParameters();
        lblPresetNote.setText(preset.note());
    }

    // ══════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("monospace", 13));
        l.setStyle("-fx-text-fill: #93c5fd; -fx-font-weight: bold; -fx-padding: 8 0 4 0;");
        return l;
    }

    private Label referencesLabel() {
        Label label = new Label(
            "REFERENCIAS\n" +
            "Halliday, Resnick e Walker - Fundamentos de Fisica\n" +
            "Serway e Jewett - Principios de Fisica\n" +
            "Baraff e Witkin - Physically Based Modeling\n" +
            "Ericson - Real-Time Collision Detection\n" +
            "NASA - Planetary Fact Sheet\n" +
            "OpenJFX Documentation - JavaFX"
        );
        label.setWrapText(true);
        label.setFont(Font.font("monospace", 11));
        label.setStyle("-fx-text-fill: #94a3b8; -fx-background-color: #020617; -fx-border-color: #334155; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8;");
        return label;
    }

    private Label compactLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("monospace", 12));
        label.setStyle("-fx-text-fill: #cbd5e1;");
        return label;
    }

    private Separator sep() {
        Separator s = new Separator();
        s.setStyle("-fx-background-color: #1a1a35;");
        return s;
    }

    private Button styledBtn(String text) {
        Button b = new Button(text);
        b.setStyle(STYLE_BTN);
        b.setMinHeight(40);
        b.setFont(Font.font("monospace", 14));
        return b;
    }

    private TextField numericInput(String value) {
        TextField input = new TextField(value);
        input.setPrefWidth(92);
        input.setMinWidth(82);
        input.setMaxWidth(104);
        input.setFont(Font.font("monospace", 13));
        input.setStyle(
            "-fx-background-color: #020617; -fx-text-fill: #e0f2fe; -fx-border-color: #475569; " +
            "-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 6 8;"
        );
        return input;
    }

    private String formatParamValue(Parameter p) {
        if (p.hasOptions()) return p.getSelectedOption();
        double v = p.getValue();
        String unit = p.getUnit().isEmpty() ? "" : " " + p.getUnit();
        return p.getStep() < 0.5
            ? String.format("%.2f%s", v, unit)
            : String.format("%.1f%s", v, unit);
    }

    private String formatPlainValue(Parameter p) {
        double v = p.getValue();
        if (p.getStep() < 0.05) return String.format("%.3f", v);
        if (p.getStep() < 0.5) return String.format("%.2f", v);
        return String.format("%.1f", v);
    }

    private void applyTypedValue(TextField input, Slider slider, Parameter param, Label valLbl) {
        String raw = input.getText() == null ? "" : input.getText().trim().replace(',', '.');
        if (raw.isEmpty()) {
            input.setText(formatPlainValue(param));
            return;
        }

        try {
            double typed = Double.parseDouble(raw);
            double oldValue = param.getValue();
            param.setValue(typed);
            valLbl.setText(formatParamValue(param));
            input.setText(formatPlainValue(param));

            applyingTypedValue = true;
            slider.setValue(param.getValue());
            applyingTypedValue = false;

            if (Math.abs(oldValue - param.getValue()) > 1e-9) {
                manager.notifyParameterChanged(param.getName(), param.getValue());
            }
        } catch (NumberFormatException ex) {
            input.setText(formatPlainValue(param));
        } finally {
            applyingTypedValue = false;
        }
    }

    private double snapToStep(double value, double step) {
        return step > 0 ? Math.round(value / step) * step : value;
    }

    private String shortTabTitle(String displayName) {
        return switch (displayName) {
            case "Cabo de Guerra" -> "Cabo";
            case "Queda Livre" -> "Queda";
            case "Sistema Solar" -> "Solar";
            case "Movimento de Projetil", "Movimento de Projétil" -> "Projetil";
            default -> displayName.length() > 10 ? displayName.substring(0, 10) : displayName;
        };
    }

    // ── API pública ────────────────────────────────
    public void setOnResetRequested(Runnable r)  { this.onResetRequested  = r; }
    public void setOnLaunchRequested(Runnable r) { this.onLaunchRequested = r; }
    public void setOnPauseRequested(Runnable r)  { this.onPauseRequested  = r; }
    public void setOnStepRequested(Runnable r)   { this.onStepRequested   = r; }
    public void setOnExportRequested(Runnable r) { this.onExportRequested = r; }
    public void setOnTimeScaleChanged(DoubleConsumer c) { this.onTimeScaleChanged = c; }

    /** Atualiza o label de telemetria com dados em tempo real. */
    public void updateTelemetry(String text) {
        lblTelemetry.setText(text);
    }

    public void updateLabState(boolean paused, double timeScale, double simTime) {
        btnPause.setText(paused ? "Retomar" : "Pausar");
        btnPause.setStyle(paused ? STYLE_BTN_ACTIVE : STYLE_BTN);
        btnStep.setDisable(!paused);
        lblLabClock.setText(String.format("Tempo simulado: %.2f s | escala: %.2fx", simTime, timeScale));
        String desired = String.format(Locale.US, "%.2fx", timeScale);
        if (!desired.equals(cbTimeScale.getValue())) cbTimeScale.setValue(desired);
    }
}
