package hobo.graphics;

import hobo.DrawCardDecision;
import hobo.DrawMissionsDecision;
import hobo.Mission;
import hobo.Visualization;
import hobo.State;
import hobo.PlayerState;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class MissionsPanel extends JPanel implements Visualization {
	private ArrayList<Image> missionsImage;
	private final JComboBox missionsCombo;
	private final GamePanel gamePanel;
	
	public MissionsPanel(final GamePanel gamePanel) {
		this.gamePanel = gamePanel;
		
		final MissionsPanel mp = this;
		missionsCombo = new JComboBox();
		missionsCombo.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				mp.repaint();			
			}
		});
		setPreferredSize(new Dimension(200, 150));
		add(missionsCombo);
		
		JButton missionsButton = new JButton("Draw Mission Cards");
		missionsButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				gamePanel.drawMissions();
			}
		});
		add(missionsButton);
		
		setLayout(new FlowLayout());
	}
	
	@Override public void paintComponent(Graphics arg0) {
		super.paintComponent(arg0);
		Graphics2D g2 = (Graphics2D) arg0;
		int i = missionsCombo.getSelectedIndex();
		if (i >= 0)
			g2.drawImage(missionsImage.get(i), 0, 50, this);
	}

	@Override public void reflect(State s) {
		PlayerState ps = s.currentPlayerState();
		missionsImage = new ArrayList<Image>();
		missionsCombo.removeAllItems();
		for (Mission m: ps.missions) {
			missionsImage.add(getToolkit().getImage("src/missions/"+m.imagePath));
			missionsCombo.addItem(m.source + " - " + m.destination + " ("+m.value+")");
		}
		if (ps.drawn_missions != null && gamePanel.awaitingDecision())
			new MissionChooserFrame(ps.drawn_missions, gamePanel);
	}
}
