import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

public class MainClass {
	public static void main(String[] args) {
		MainCanvas meuCanvas = new MainCanvas();
		
		JFrame f = new JFrame();
		f.setTitle("Cena 3D - OBJ");
		f.setSize(960, 720);
		f.getContentPane().add(meuCanvas);
		f.setVisible(true);
		meuCanvas.requestFocusInWindow();

		f.addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent e) {
		        System.exit(0);
		    }
		});
		
		meuCanvas.start();
	}
}
