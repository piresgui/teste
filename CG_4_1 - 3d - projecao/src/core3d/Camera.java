package core3d;

/** Camera livre (FPS): posicao + yaw/pitch, olhando para -Z quando yaw=0. */
public class Camera {
	public float x, y, z;
	public float yaw = 0;    // radianos, gira em torno de Y
	public float pitch = 0;  // radianos, olhar para cima/baixo
	
	public Camera(float x, float y, float z) {
		this.x = x; this.y = y; this.z = z;
	}
	
	public void gira(float dYaw, float dPitch) {
		yaw += dYaw;
		pitch += dPitch;
		float lim = 1.55f;
		if (pitch > lim) pitch = lim;
		if (pitch < -lim) pitch = -lim;
	}
	
	/** Move em relacao a direcao do olhar (frente/lado) e verticalmente no mundo. */
	public void move(float frente, float lado, float subir) {
		float sy = (float)Math.sin(yaw), cy = (float)Math.cos(yaw);
		// frente projetada no plano horizontal, para nao "voar" ao olhar para cima
		x += sy * frente + cy * lado;
		z += -cy * frente + sy * lado;
		y += subir;
	}
	
	/** Converte um ponto do mundo para o espaco da camera (x direita, y cima, z = distancia a frente). */
	public float[] paraCamera(Ponto3D p) {
		float sy = (float)Math.sin(yaw), cy = (float)Math.cos(yaw);
		float sp = (float)Math.sin(pitch), cp = (float)Math.cos(pitch);
		float dx = p.x - x, dy = p.y - y, dz = p.z - z;
		
		float rx = cy, ry = 0, rz = sy;
		float ux = -sy * sp, uy = cp, uz = cy * sp;
		float fx = sy * cp, fy = sp, fz = -cy * cp;
		
		return new float[] {
			dx * rx + dy * ry + dz * rz,
			dx * ux + dy * uy + dz * uz,
			dx * fx + dy * fy + dz * fz
		};
	}
}
