# Documentação: cena 3D com arquivos .obj e câmera livre

Este documento descreve **o que foi adicionado ao projeto original** (wireframe com cubos e carro) para carregar arquivos `.obj` e navegar pela cena. As referências de linha são de `src/`.

## Respostas rápidas

| Pergunta | Resposta |
|---|---|
| Onde os arquivos `.obj` são lidos? | `core3d/ObjLoader.java`, método `carrega(...)` |
| Onde se escolhe quais `.obj` entram na cena e onde ficam? | `MainCanvas.java`, método `carregaCena()` (linha 87) |
| Onde a cena é renderizada (desenhada na tela)? | `MainCanvas.java`: `paint()` (linha 166) chama `desenhaModelo()` (linha 144) |
| Onde está a mecânica de locomoção? | `MainCanvas.simulaMundo()` (linha 100) lê as teclas e chama `Camera.move()` |
| Onde está a mecânica de girar a câmera? | Mouse: `mouseDragged` (linha 76). Setas: `simulaMundo()`. Ambos chamam `Camera.gira()` |
| Onde está a câmera? | `core3d/Camera.java` (posição, yaw, pitch); instanciada em `MainCanvas.java` linha 38 |
| Onde está a perspectiva? | `MainCanvas.telaX()` (linha 142) e `distFocal` (linha 40) |
| Onde entram as teclas/mouse? | `MainCanvas()` (construtor, linhas 43-85) |
| Onde se muda posição, escala ou rotação dos objetos? | Parâmetros em `carregaCena()` |

## Fluxo geral

```
MainClass          cria janela + MainCanvas e chama start()
  MainCanvas()     registra teclado/mouse e chama carregaCena()
    carregaCena()  ObjLoader lê cada .obj  ->  Modelo (lista de Triangulo3D)
  run() (thread)   repete: simulaMundo() -> paintImmediately() -> paint()
    simulaMundo()  teclas -> Camera.move() / Camera.gira()
    paint()        para cada Modelo -> desenhaModelo()
      desenhaModelo()  Camera.paraCamera() -> recortaNear() -> telaX() -> drawPolygon()
```

---

## 1. `core3d/ObjLoader.java` (novo): leitura dos .obj

**Responsável por:** transformar um arquivo `.obj` em um `Modelo` pronto para a cena.

`carrega(nome, escala, giroY, px, py, pz)`:

1. Procura o arquivo na pasta atual e, se não achar, em `..` (por isso os `.obj` podem ficar na pasta acima do projeto).
2. Lê apenas duas linhas do formato:
   - `v x y z`: vértices.
   - `f a/b/c ...`: faces. Usa só o primeiro número de cada item, o índice do vértice. Aceita índices negativos (relativos ao fim). Faces com mais de 3 vértices (quadriláteros, por exemplo) viram triângulos em leque.
3. Ignora normais, texturas, materiais (`.mtl`) e grupos.
4. Normaliza o modelo, porque cada `.obj` vem em uma escala e origem diferentes:
   - centraliza em X e Z (o centro da caixa envolvente vai para a origem);
   - apoia o menor Y em 0 (o modelo fica "no chão");
   - gira `giroY` graus em torno do eixo Y;
   - multiplica por `escala`;
   - move para (`px`, `py`, `pz`).
5. Devolve um `Modelo`.

## 2. `core3d/Modelo.java` (novo): contêiner

**Responsável por:** guardar os triângulos de um objeto carregado (`ArrayList<Triangulo3D>`). É o "objeto" da cena: a casa, cada cadeira, o tanque.

## 3. `core3d/Triangulo3D.java` (alterado): getters

**Responsável por:** foram adicionados `getA()`, `getB()` e `getC()` para que o `MainCanvas` leia os vértices e projete. O resto da classe é do projeto original.

## 4. `core3d/Camera.java` (novo): câmera livre

**Responsável por:** guardar onde o observador está e para onde olha, e converter pontos do mundo para a visão da câmera.

- Estado: `x, y, z` (posição), `yaw` (giro horizontal) e `pitch` (olhar para cima/baixo), em radianos. Com `yaw = 0` a câmera olha para **-Z**.
- `gira(dYaw, dPitch)`: soma aos ângulos e limita o `pitch` a cerca de ±89° (para não virar de cabeça para baixo).
- `move(frente, lado, subir)`: **locomoção**. Anda relativo ao `yaw`. A "frente" é projetada no plano horizontal, então olhar para cima não faz a câmera voar.
- `paraCamera(ponto)`: calcula onde o ponto do mundo fica no espaço da câmera, devolvendo `{x direita, y cima, z distância à frente}`. É a "matriz de visão", feita à mão com senos e cossenos em vez de `Mat4x4`.

## 5. `MainCanvas.java` (reescrito): cena, entrada, render

### 5.1 Montagem da cena: `carregaCena()` (linha 87)
**Responsável por:** chamar `ObjLoader.carrega` para cada objeto e guardar em `cena` (linha 37). Aqui ficam as posições: casa na origem, duas cadeiras dentro (piso em y = 0,67, x = ±3), tanque em (0, 0, 18) girado 90° para o canhão apontar para a porta. É o lugar para reposicionar objetos.

### 5.2 Entrada: teclado e mouse (construtor, linhas 43-85)
**Responsável por:** registrar o que o usuário está fazendo.
- Teclado: `teclas` (linha 33) é um conjunto das teclas seguradas. `keyPressed` adiciona, `keyReleased` remove. Isso permite segurar várias teclas ao mesmo tempo e ter movimento contínuo.
- Mouse: ao arrastar com o botão esquerdo, `mouseDragged` (linha 76) converte o deslocamento em pixels para ângulo (0,005 rad por pixel) e chama `camera.gira()`. O clique também pede o foco do teclado.

### 5.3 Locomoção e giro: `simulaMundo(diftime)` (linha 100)
**Responsável por:** a cada quadro, ler `teclas` e mexer na câmera.
- `W/S` = frente/trás, `A/D` = esquerda/direita (`camera.move`).
- Setas = girar (`camera.gira`).
- Multiplica pela duração do quadro (`diftime`), então a velocidade (8 unidades/s) e o giro (1,8 rad/s) não dependem do FPS.

### 5.4 Renderização (`paint`, `desenhaModelo`, `recortaNear`, `telaX`)
**Responsável por:** desenhar a cena como wireframe, com perspectiva.
- `paint()` (linha 166): limpa a tela de branco, define preto e chama `desenhaModelo` para cada item de `cena`. Também escreve o FPS e a posição da câmera.
- `desenhaModelo()` (linha 144), para cada triângulo:
  1. `camera.paraCamera()` nos 3 vértices;
  2. `recortaNear()`;
  3. `telaX()` para obter os pixels;
  4. `drawPolygon()` traça o contorno.
- `recortaNear()` (linha 123): corta o polígono no plano `z = near`. Sem isso, vértices atrás da câmera geram divisão por número negativo ou zero e desenham linhas erradas.
- `telaX()` (linha 142): **projeção em perspectiva**. `x_tela = W/2 + distFocal * x / z` e `y_tela = H/2 - distFocal * y / z`. Quanto mais longe (`z` maior), mais perto do centro. `distFocal` (linha 40) controla o campo de visão.

### 5.5 Loop principal: `start()` / `run()` (linhas 183-189)
**Responsável por:** rodar a cada quadro `simulaMundo` e depois `paintImmediately`, além de contar o FPS. Vem do projeto original, com o cálculo do tempo entre quadros (`diftime`) corrigido para alimentar a locomoção.

## 6. `MainClass.java` (alterado)
**Responsável por:** abrir a janela. O tamanho passou para 960x720 e a janela é exibida depois de receber o canvas, com o foco do teclado pedido para ele.

---

## Removido do projeto original
Cubos e carro feitos à mão, leitura do BMP, carregamento do `gato.jpg`, clique para criar triângulos e os controles antigos (Z/X/Q/E/1/2/3, que mexiam em `modelview` e `projecao`). `Mat4x4`, `Ponto3D` e `core2d` continuam no projeto, mas a cena nova não usa `Mat4x4` para projetar.

## Limitações
- Só wireframe: não há preenchimento, iluminação, cores nem texturas (os `.mtl` não existem na pasta).
- Não há colisão: dá para atravessar paredes.
- O eixo "para cima" assumido é Y (os três `.obj` fornecidos usam Y).
