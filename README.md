# Physics3D Engine — Java

Motor de física e gráficos 3D escrito do zero em Java com JavaFX.
Desenhe formas 2D no painel esquerdo e elas se tornam objetos 3D com física simulada.

---

## Estrutura do Projeto

```
Physics3DEngine/
├── src/main/java/engine/
│   ├── Main.java                   ← Entry point
│   ├── core/
│   │   └── Engine.java             ← Loop principal, JavaFX Application
│   ├── math/
│   │   ├── Vec3.java               ← Vetor 3D imutável
│   │   ├── Mat4.java               ← Matriz 4×4 (transforms, perspectiva)
│   │   └── ColorRGBA.java          ← Cor RGBA com shading
│   ├── physics/
│   │   ├── RigidBody.java          ← Corpo rígido (Euler semi-implícito)
│   │   └── PhysicsWorld.java       ← Simulação com fixed timestep 120Hz
│   ├── renderer/
│   │   ├── Mesh.java               ← Geometrias 3D (Box, Sphere, Cone, Torus...)
│   │   ├── Camera.java             ← Câmera orbital/livre com projeção perspectiva
│   │   └── Renderer3D.java         ← Pipeline software (Painter's + flat shading)
│   ├── scene/
│   │   ├── SceneObject.java        ← Objeto (malha + corpo + material)
│   │   └── ObjectFactory.java      ← Fábrica de objetos com presets
│   └── ui/
│       ├── DrawingCanvas.java      ← Canvas 2D de entrada de formas
│       └── Toolbar.java            ← Painel de controle lateral
├── pom.xml                         ← Build Maven
├── run.sh                          ← Script Linux/macOS
└── run.bat                         ← Script Windows
```

---

## Requisitos

- **Java 17+** (recomendado Java 21)
- **JavaFX 21** (incluído via Maven automaticamente)
- **Maven 3.6+** (recomendado) — ou JavaFX SDK manual

---

## Como Compilar e Executar

### Opção 1 — Gradle Wrapper (recomendado)

```bash
# Windows
.\gradlew.bat run

# Linux / macOS
./gradlew run
```

Para abrir direto em um modulo especifico:

```bash
./gradlew run --args="--module=projectile"
./gradlew run --args="--module=collisions"
./gradlew run --args="--module=solar_system"
./gradlew run --args="--module=tug_of_war"
```

### Opção 2 — Maven

```bash
# Linux / macOS
./run.sh

# Windows
run.bat

# Ou diretamente:
mvn clean javafx:run
```

### Opção 3 — IntelliJ IDEA

1. Abrir o projeto (`File → Open` → selecionar a pasta)
2. Importar como projeto Gradle
3. Run → `engine.Main`

### Opção 4 — VS Code

1. Instalar extensão "Extension Pack for Java"
2. Abrir a pasta do projeto
3. Pressionar F5 (Run Java)

### Opção 5 — javac manual

```bash
# Baixar JavaFX SDK: https://openjfx.io
export JAVAFX_HOME=/caminho/para/javafx-sdk-21

# Compilar
find src -name "*.java" | xargs javac \
  --module-path $JAVAFX_HOME/lib \
  --add-modules javafx.controls \
  -d out

# Executar
java \
  --module-path $JAVAFX_HOME/lib \
  --add-modules javafx.controls \
  -cp out \
  engine.Main
```

---

## Como Usar

### Simulacoes com estados fisicos

- `Colisoes`: escolha `Material A`, `Material B`, raios e velocidade. A massa e calculada por densidade x volume, e a resposta usa restituição, atrito, amortecimento e dureza do material.
- `Lancamento de Projeteis`: escolha material do projetil, material do alvo e meio (`Vacuo`, `Ar`, `Agua`, `Oleo`, `Gel`). O meio aplica arrasto proporcional a velocidade ao quadrado e altera o efeito da gravidade por empuxo aproximado.
- `Plano Inclinado`: compara componente do peso e atrito na rampa para mostrar quando o bloco fica parado ou acelera.
- `Pendulo`: integra o pendulo simples nao linear e mostra comprimento, angulo, velocidade tangencial e periodo aproximado.
- `Mola`: simula a lei de Hooke em um oscilador massa-mola com rigidez, massa e deslocamento inicial editaveis.
- `Sistema Solar`: observe Mercurio ate Netuno em orbitas heliocentricas, texturas planetarias, rotacao axial, trilhas, brilho solar, aneis de Saturno e Urano e foco de camera. As luas maiores de Terra, Marte e planetas gigantes orbitam seus planetas em escala visual comprimida. Aplique situacoes como Sol mais massivo, Sol menos massivo, aceleracao, frenagem e impacto radial no planeta selecionado.
- Materiais incluidos: `Gelatina`, `Borracha`, `Madeira`, `Pedra`, `Aco`, `Vidro`, `Espuma`.

### Abas da interface

| Aba | Função |
|-----|--------|
| Módulos | Simulações educacionais com parâmetros e telemetria |
| Desenhar | Sandbox da engine: o desenho cria objetos 3D reais no `PhysicsWorld` |

### Modo laboratorio

- O painel de simulacoes tem presets repetiveis para comparar queda, projetil, colisao, rampa, pendulo e mola.
- `Pausar`, `Retomar` e `Passo unico` controlam o tempo simulado sem desligar a gravidade escolhida.
- A escala do tempo permite observar o mesmo experimento em `0.25x`, `0.50x`, `1.00x`, `2.00x` ou `4.00x`.
- O HUD da cena mostra o modulo ativo, tempo simulado, estado da gravidade e as medidas principais do experimento.
- `Exportar CSV` grava as amostras exibidas no grafico para usar em relatorios e comparacoes.
- `Modo leve` reduz objetos pequenos, sombras pesadas e excesso de triangulos para melhorar FPS.

### Painel Esquerdo — Desenho 2D

| Ação                  | Resultado                         |
|-----------------------|-----------------------------------|
| Arrastar (Retângulo)  | Cria um cubo/caixa 3D            |
| Arrastar (Círculo)    | Cria uma esfera 3D               |
| Arrastar (Triângulo)  | Cria um cone 3D                  |
| Arrastar (Livre)      | Acumula tracos; contornos fechados viram solidos extrudados |

### Cena 3D — Controle de Câmera

| Ação              | Efeito              |
|-------------------|---------------------|
| Arrastar (mouse)  | Orbitar ao redor    |
| Scroll do mouse   | Zoom in/out         |
| WASD              | Andar na cena       |
| Q / E             | Descer / subir      |

### Toolbar — Controles

| Controle       | Função                                      |
|----------------|---------------------------------------------|
| Modo Desenho   | Forma que será desenhada no painel 2D       |
| Forma 3D       | Sobrescreve o tipo geométrico criado        |
| Material físico | Preset com densidade, atrito, restituição e amortecimento |
| Cor            | Cor do material do próximo objeto           |
| 🎲 Aleatório   | Cria objeto com forma/física aleatórios     |
| 💥 Explodir!   | Aplica força explosiva em todos os objetos  |
| 🗑 Limpar      | Remove todos os objetos da cena             |
| ⟳ Reset Câmera | Volta câmera à posição inicial              |
| Wireframe      | Modo aramado                                |
| Grade          | Mostra/esconde grade do chão                |
| Sombras        | Sombras planares projetadas                 |
| Vetores        | Mostra posição, velocidade e gravidade do objeto selecionado |
| ⬇ Gravidade   | Liga/desliga gravidade                      |
| Objeto selecionado | Edita massa, posição e velocidade de objetos do sandbox |
| Salvar/Carregar cena | Persiste cenas do sandbox, inclusive objetos desenhados |

O grafico lateral acompanha grandezas especificas por modulo: altura e velocidade na queda e no projetil, percurso na rampa, angulo no pendulo, deslocamento na mola e velocidades dos corpos na colisao. No sandbox ele continua medindo altura e velocidade do objeto observado.

---

## Arquitetura Técnica


Os prints do programa com explicacao de cada tela estao em [`docs/PRINTS_DO_PROGRAMA.md`](docs/PRINTS_DO_PROGRAMA.md).

### Física (PhysicsWorld + RigidBody)
- **Fixed Timestep**: 120 Hz (8 sub-passos máx por frame)
- **Integração**: Euler semi-implícito (velocity Verlet)
- **Colisões**: Esfera-esfera com impulso de separação
- **Ambiente**: Chão e paredes com coeficiente de restituição
- **Sleep system**: Corpos dormem quando velocidade < threshold

### Renderer (Renderer3D + Camera)
- **Pipeline software**: sem OpenGL/Vulkan
- **Painter's Algorithm**: ordenação por profundidade
- **Back-face Culling**: descarta faces voltadas para longe
- **Flat Shading**: shading Phong por face (2 luzes + ambiente)
- **Sombras planares**: projeção no plano Y = -5
- **Camera orbital e livre**: coordenadas esfericas com arraste para orbitar e `WASD` + `Q/E` para percorrer a cena

### Geometrias disponíveis (Mesh)
- Box (cubo/paralelepípedo)
- Sphere (UV sphere)
- Cone
- Cylinder
- Tetrahedron
- Octahedron
- Icosahedron
- Torus

---

## Extensibilidade

Para adicionar uma nova forma geométrica:
1. Adicionar método `createXxx()` em `Mesh.java`
2. Adicionar enum em `ObjectFactory.ShapeType`
3. Adicionar case no switch de `ObjectFactory.buildMesh()`
4. Adicionar item no ComboBox em `Toolbar.java`

Para adicionar novo preset de física:
1. Adicionar enum em `ObjectFactory.PhysicsPreset`
2. Adicionar toggle em `Toolbar.java`

---

## Referencias usadas

- Halliday, D.; Resnick, R.; Walker, J. `Fundamentos de Fisica`.
- Serway, R. A.; Jewett, J. W. `Principios de Fisica`.
- Baraff, D.; Witkin, A. `Physically Based Modeling: Principles and Practice`.
- Ericson, C. `Real-Time Collision Detection`.
- NASA/JPL SSD. `Approximate Positions of the Planets`, para elementos keplerianos J2000 usados nas orbitas do sistema solar.
- NASA/JPL SSD. `Planetary Physical Parameters`, para periodos siderais de rotacao e periodos orbitais dos planetas.
- NASA NSSDC. `Planetary Fact Sheet`, para razoes fisicas e parametros orbitais complementares dos planetas.
- NASA/JPL. `Solar System Simulator - Texture Maps`, para mapas visuais planetarios. Mapas dos gigantes gasosos sao representativos e as texturas nao substituem analise cientifica.
- NASA/JPL SSD. `Planetary Satellite Mean Elements`, para periodos e distancias orbitais de satelites naturais.
- OpenJFX Documentation, para a interface JavaFX.

---

## Licença
MIT — use livremente.
