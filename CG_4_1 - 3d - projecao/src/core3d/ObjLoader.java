package core3d;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ObjLoader {
	
	/**
	 * Le um .obj (v e f; faces com mais de 3 vertices sao triangularizadas),
	 * centraliza em X/Z, apoia o menor Y em 0, escala e gira giroY graus em torno de Y e move para (px, py, pz).
	 * Procura o arquivo no diretorio atual e na pasta acima.
	 */
	public static Modelo carrega(String nome, float escala, float giroY, float px, float py, float pz) throws IOException {
		File arq = new File(nome);
		if (!arq.exists()) {
			arq = new File("..", nome);
		}
		
		ArrayList<float[]> vertices = new ArrayList<>();
		ArrayList<int[]> faces = new ArrayList<>();
		
		try (BufferedReader br = new BufferedReader(new FileReader(arq))) {
			String linha;
			while ((linha = br.readLine()) != null) {
				linha = linha.trim();
				if (linha.startsWith("v ")) {
					String[] t = linha.split("\s+");
					vertices.add(new float[] {
						Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3]) });
				} else if (linha.startsWith("f ")) {
					String[] t = linha.split("\s+");
					int[] idx = new int[t.length - 1];
					for (int i = 1; i < t.length; i++) {
						int v = Integer.parseInt(t[i].split("/")[0]);
						idx[i - 1] = v > 0 ? v - 1 : vertices.size() + v;
					}
					faces.add(idx);
				}
			}
		}
		
		float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (float[] v : vertices) {
			minX = Math.min(minX, v[0]); maxX = Math.max(maxX, v[0]);
			minY = Math.min(minY, v[1]);
			minZ = Math.min(minZ, v[2]); maxZ = Math.max(maxZ, v[2]);
		}
		float cx = (minX + maxX) / 2;
		float cz = (minZ + maxZ) / 2;
		
		float rad = giroY * 0.017453f;
		float sin = (float)Math.sin(rad), cos = (float)Math.cos(rad);
		
		ArrayList<Ponto3D> pontos = new ArrayList<>();
		for (float[] v : vertices) {
			float dx = v[0] - cx, dz = v[2] - cz;
			float x = dx * cos + dz * sin;
			float z = -dx * sin + dz * cos;
			pontos.add(new Ponto3D(x * escala + px, (v[1] - minY) * escala + py, z * escala + pz));
		}
		
		Modelo m = new Modelo();
		for (int[] f : faces) {
			for (int i = 1; i < f.length - 1; i++) {
				m.triangulos.add(new Triangulo3D(pontos.get(f[0]), pontos.get(f[i]), pontos.get(f[i + 1])));
			}
		}
		System.out.println(nome + ": " + vertices.size() + " vertices, " + m.triangulos.size() + " triangulos");
		return m;
	}
}
