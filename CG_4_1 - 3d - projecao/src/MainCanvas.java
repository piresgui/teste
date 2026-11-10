import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;

import core3d.Camera;
import core3d.Modelo;
import core3d.ObjLoader;
import core3d.Ponto3D;
import core3d.Triangulo3D;

public class MainCanvas extends JPanel implements Runnable {
	int W = 640;
	int H = 480;

	Thread runner;
	boolean ativo = true;

	int framecount = 0;
	int fps = 0;
	Font f = new Font("", Font.PLAIN, 16);

	Set<Integer> teclas = new HashSet<>();
	int ultimoMouseX, ultimoMouseY;
	boolean arrastando = false;

	ArrayList<Modelo> cena = new ArrayList<>();
	Camera camera = new Camera(0, 4, 28);

	float distFocal = 500;
	float near = 0.1f;

	public MainCanvas() {
		setSize(W, H);
		setFocusable(true);

		carregaCena();

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				teclas.add(e.getKeyCode());
			}

			@Override
			public void keyReleased(KeyEvent e) {
				teclas.remove(e.getKeyCode());
			}
		});

		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				requestFocusInWindow();
				ultimoMouseX = e.getX();
				ultimoMouseY = e.getY();
				arrastando = true;
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				arrastando = false;
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (!arrastando) return;
				camera.gira((e.getX() - ultimoMouseX) * 0.005f, -(e.getY() - ultimoMouseY) * 0.005f);
				ultimoMouseX = e.getX();
				ultimoMouseY = e.getY();
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	private void carregaCena() {
		try {
			cena.add(ObjLoader.carrega("medieval house.obj", 1.0f, 0, 0, 0, 0));
			// cadeiras sobre o piso do celeiro (y = 0.67), viradas para a porta (+z)
			cena.add(ObjLoader.carrega("chair_01.obj", 3.0f, 0, -3, 0.67f, -0.5f));
			cena.add(ObjLoader.carrega("chair_01.obj", 3.0f, 0, 3, 0.67f, -0.5f));
			// tanque de frente para a porta, canhao (+x do modelo) apontando para -z
			cena.add(ObjLoader.carrega("tank.obj", 0.05f, 90, 0, 0, 18));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void simulaMundo(long diftime) {
		float dt = diftime / 1000.0f;
		float vel = 8 * dt;
		float giro = 1.8f * dt;

		if (teclas.contains(KeyEvent.VK_SHIFT)) vel *= 3;

		float frente = 0, lado = 0;
		if (teclas.contains(KeyEvent.VK_W)) frente += vel;
		if (teclas.contains(KeyEvent.VK_S)) frente -= vel;
		if (teclas.contains(KeyEvent.VK_D)) lado += vel;
		if (teclas.contains(KeyEvent.VK_A)) lado -= vel;
		camera.move(frente, lado, 0);

		float dYaw = 0, dPitch = 0;
		if (teclas.contains(KeyEvent.VK_LEFT)) dYaw -= giro;
		if (teclas.contains(KeyEvent.VK_RIGHT)) dYaw += giro;
		if (teclas.contains(KeyEvent.VK_UP)) dPitch += giro;
		if (teclas.contains(KeyEvent.VK_DOWN)) dPitch -= giro;
		camera.gira(dYaw, dPitch);
	}

	/** Recorta o poligono (em espaco da camera) contra o plano z = near. */
	private ArrayList<float[]> recortaNear(float[][] v) {
		ArrayList<float[]> out = new ArrayList<>();
		for (int i = 0; i < v.length; i++) {
			float[] a = v[i];
			float[] b = v[(i + 1) % v.length];
			boolean aDentro = a[2] >= near;
			boolean bDentro = b[2] >= near;
			if (aDentro) out.add(a);
			if (aDentro != bDentro) {
				float t = (near - a[2]) / (b[2] - a[2]);
				out.add(new float[] {
					a[0] + t * (b[0] - a[0]),
					a[1] + t * (b[1] - a[1]),
					near });
			}
		}
		return out;
	}

	private int[] telaX(float[] p) { return new int[] { (int)(W / 2 + distFocal * p[0] / p[2]), (int)(H / 2 - distFocal * p[1] / p[2]) }; }
	
	private void desenhaModelo(Modelo m, Graphics2D g) {
		for (Triangulo3D tri : m.triangulos) {
			float[][] v = {
				camera.paraCamera(tri.getA()),
				camera.paraCamera(tri.getB()),
				camera.paraCamera(tri.getC()) };
			
			ArrayList<float[]> poly = recortaNear(v);
			if (poly.size() < 3) continue;
			
			int[] xs = new int[poly.size()];
			int[] ys = new int[poly.size()];
			for (int i = 0; i < poly.size(); i++) {
				int[] p = telaX(poly.get(i));
				xs[i] = p[0];
				ys[i] = p[1];
			}
			g.drawPolygon(xs, ys, xs.length);
		}
	}
	
	@Override
	public void paint(Graphics g0) {
		Graphics2D g = (Graphics2D)g0;
		W = getWidth();
		H = getHeight();

		g.setColor(Color.white);
		g.fillRect(0, 0, W, H);
		
		g.setColor(Color.black);
		for (Modelo m : cena) {
			desenhaModelo(m, g);
		}
		
		g.setFont(f);
		g.drawString("FPS " + fps + "  pos: " + (int)camera.x + "," + (int)camera.y + "," + (int)camera.z, 10, 20);
	}
	
	public void start() {
		runner = new Thread(this);
		runner.start();
	}

	@Override
	public void run() {
		long time = System.currentTimeMillis();
		long segundo = time / 1000;
		long diftime = 0;
		while (ativo) {
			simulaMundo(diftime);
			paintImmediately(0, 0, getWidth(), getHeight());

			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long newtime = System.currentTimeMillis();
			long novoSegundo = newtime / 1000;
			diftime = newtime - time;
			time = newtime;
			framecount++;
			if (novoSegundo != segundo) {
				fps = framecount;
				framecount = 0;
				segundo = novoSegundo;
			}
		}
	}
}
