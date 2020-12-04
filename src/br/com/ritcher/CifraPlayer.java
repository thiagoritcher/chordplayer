package br.com.ritcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;

public class CifraPlayer {
	private JTextArea ta;
	private JTextArea tc;
	private Cifra cifra;

	private void setup() {
		JFrame frame = new JFrame("Cifra Player");
		frame.setSize(500, 200);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				super.windowClosed(e);
			}
		});

		Border border = BorderFactory.createLineBorder(Color.BLACK);

		ta = new JTextArea();
		ta.requestFocus();
		ta.setLineWrap(true);
		ta.setBorder(border);

		JScrollPane scroll = new JScrollPane(ta);
		frame.add(scroll, BorderLayout.CENTER);

		tc = new JTextArea();
		tc.requestFocus();
		tc.setLineWrap(true);
		tc.setBorder(border);

		frame.add(tc, BorderLayout.SOUTH);

		tc.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_P) {
					cifra.cifra = ta.getText();
					cifra.processa();
					System.out.println(cifra);
				}
			}
		});
		
		cifra = new Cifra();
		frame.setVisible(true);
		
	}

	public static void main(String[] args) {
		new CifraPlayer().setup();
	}

	class Player implements Runnable {
		
		private Synthesizer synth;
		private MidiChannel[] channels;
		private int instrument;

		private void setup() throws MidiUnavailableException {
			synth = MidiSystem.getSynthesizer();
			synth.open();
			channels = synth.getChannels();
			
			instrument = 0;
			
		}
		
		String currentChord;
		
		@Override
		public void run() {
			synchronized (this) {
			 //TODO Play the chords	
			}
		}
		
		private List<String> notes = Arrays.asList("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");
		
		private int id(String note) {
			int octave = Integer.parseInt(note.substring(0, 1));
			return notes.indexOf(note.substring(1)) + 12 * octave + 12;
		}
	}
	
	class Cifra {
		String cifra;
	
		List<Chord> chords;
		
		private void processa() {
			chords = new ArrayList<CifraPlayer.Cifra.Chord>();
			ChordLine cl = new ChordLine();
			String[] split = cifra.split("\\n");
			int currentPos = 0;
			for (int i = 0; i < split.length; i++) {
				if(cl.isChordLine(split[i])) {
					String[] c = split[i].split("\\s+");
					int p = 0;
					for (int j = 0; j < c.length; j++) {
						p = split[i].indexOf(c[j], p); 
						
						if(cl.isChord(c[j])) {
							Chord cc = new Chord(c[j], currentPos + p);
							chords.add(cc);
						}
					}
				}
				currentPos += split[i].length();
			}
		}
		
		@Override
		public String toString() {
			return chords.toString();
		}
		
		class Chord {
			public Chord(String string, int i) {
				this.chord = string;
				this.position = i;
			}
			
			String chord;
			int position;
			
			@Override
			public String toString() {
				return "Chord [chord=" + chord + ", position=" + position + "]";
			}
		}
	}
}
