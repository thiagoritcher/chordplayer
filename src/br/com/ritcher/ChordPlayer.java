package br.com.ritcher;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import br.com.ritcher.ChordPlayer.Player;

public class ChordPlayer {
	private List<Long> times = new ArrayList<Long>();
	private List<Integer> transposes = new ArrayList<Integer>();
	private List<Character> chordlist = new ArrayList<Character>();
	private List<PlayPattern> playPatternList = new ArrayList<>();

	private List<String> notes = Arrays.asList("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");
	private MidiChannel[] channels;
	private int instrument = 0; // 0 is a piano, 9 is percussion, other channels are for other instruments

	private int volume = 50; // between 0 et 127
	private int duracao = 300;
	private int transpose = 0;

	private char setupvar;

	Synthesizer synth = null;
	private HashMap<Character, String[]> chords;
	private JLabel status;
	private JTextArea ta;
	private int currentPattern;

	Player player;
	
	class Player implements Runnable {
		public boolean paused = false;
		
		public void play(String[] c, long duracao) {
			toPlay.add(c);
			duracoes.add(duracao);
			if(!playing) {
				synchronized(thread) {
					thread.notifyAll();
				}
			}
		}

		public void play(String c, long duracao) {
			play(new String[] {c}, duracao);
		}

		public void stop() {
			toPlay.clear();
			duracoes.clear();
			synchronized (thread) {
				thread.notifyAll();
			}
		}
		
		public boolean playing = false;
		
		LinkedList<String[]> toPlay = new LinkedList<String[]>();
		LinkedList<Long> duracoes = new LinkedList<Long>();
		private Thread thread;
		
		@Override
		public void run() {
			thread = Thread.currentThread();
			synchronized (thread) {
				while(true) {
					while(paused) {
						try {
							thread.wait();
						} catch (InterruptedException e) {
						}
					}
					
					while(!toPlay.isEmpty()) {
						playing = true;
						String[] note = toPlay.poll();
						long d = duracoes.poll();
						
						for (int i = 0; i < note.length; i++) {
							channels[instrument].noteOn(id(note[i]) + transpose, volume);
						}
						try {
							thread.wait(d);
						} catch (InterruptedException e) {
						}
						for (int i = 0; i < note.length; i++) {
							channels[instrument].noteOff(id(note[i]) + transpose);
						}	
					}
					try {
						playing = false;
						thread.wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}

		private final String[] REST = new String[] {};
		public void rest(long duracao) {
			play(REST, duracao);
		}

		public void unpause() {
			this.paused = false;
			synchronized (thread) {
				thread.notifyAll();
			}
		}

		public void pause() {
			this.paused = true;
		}
	}
	
	public static void main(String[] args) throws MidiUnavailableException {
		new ChordPlayer().setup();
	}

	private void setup() throws MidiUnavailableException {
		setupChords();
		setupPlayPatterns();
		
		player = new Player();
		Thread play = new Thread(player);
		play.start();
		

		JFrame frame = new JFrame("Player");
		frame.setSize(500, 200);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				super.windowClosed(e);
				if (synth != null)
					synth.close();
			}
		});

		ta = new JTextArea();
		ta.requestFocus();
		ta.setLineWrap(true);
		frame.add(ta, BorderLayout.CENTER);

		status = new JLabel();
		frame.add(status, BorderLayout.SOUTH);
		updateStatus();

		ta.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_L) {
					ta.setText("");
					times.clear();
					chordlist.clear();
					transposes.clear();
					e.consume();
					return;
					
				} else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_P) {
					if(player.playing) {
						player.stop();
					}
					else {
						playContents();
					}
					
					e.consume();
					return;
				}

				char c = e.getKeyChar();
				if (c == 'v' || c == 'm' || c == 'n' || c == 'i' || c == 'l') {
					setupvar = c;
					e.consume();
				} else if (c == '-' || c == '=') {
					updateSetup(c);
					e.consume();
				}
				updateStatus();

				String[] cc = chords.get(c);
				if (cc == null) {
					return;
				}

				try {
					times.add(e.getWhen());
					chordlist.add(c);
					transposes.add(transpose);

					playPattern(cc);

				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

		});

		synth = MidiSystem.getSynthesizer();
		synth.open();
		channels = synth.getChannels();
		frame.setVisible(true);
	}

	private void setupChords() {
		chords = new HashMap<Character, String[]>();
		chords.put('a', new String[] { "3C", "4C", "4E", "4G" });
		chords.put('s', new String[] { "3D", "4D", "4F", "4A" });
		chords.put('d', new String[] { "3E", "4E", "4G", "4B" });
		chords.put('f', new String[] { "3F", "4F", "4A", "5C" });
		chords.put('g', new String[] { "3G", "4G", "4B", "5D" });
		chords.put('h', new String[] { "3A", "4A", "5C", "5E" });
		chords.put('j', new String[] { "3B", "4B", "5D", "5F" });

		chords.put('q', new String[] { "3C", "4C", "4E", "4G", "4A#" });
		chords.put('w', new String[] { "3D", "4D", "4F", "4A", "5C" });
		chords.put('e', new String[] { "3E", "4E", "4G", "4B", "5D" });
		chords.put('r', new String[] { "3F", "4F", "4G#", "5C" });
		chords.put('t', new String[] { "3G", "4G", "4B", "5D", "5F" });
		chords.put('y', new String[] { "3A", "4A", "5C", "5E", "5G" });
		chords.put('u', new String[] { "3B", "4B", "5D", "5F", "5A" });
	}

	private void setupPlayPatterns() {
		playPatternList.add(new PlayPattern() {
			@Override
			public void playPattern(Player s, String[] cc, long duracao) throws InterruptedException {
				s.play(cc, duracao);
			}
		});
		playPatternList.add(new PlayPattern() {
			@Override
			public void playPattern(Player s, String[] cc, long duracao) throws InterruptedException {
				s.play(cc, duracao);
				s.play(cc, duracao);
				s.play(cc, duracao);
				s.play(cc, duracao);
			}
		});
		playPatternList.add(new PlayPattern() {
			@Override
			public void playPattern(Player s, String[] cc, long duracao) throws InterruptedException {
				s.play(cc, duracao);
				s.play(cc, duracao);
				s.rest(duracao);
				s.play(cc, duracao);
			}
		});
		playPatternList.add(new PlayPattern() {
			@Override
			public void playPattern(Player s, String[] cc, long duracao) throws InterruptedException {
				s.play(cc, duracao);
				s.rest(duracao);
				s.play(cc, duracao);
				s.rest(duracao);
			}
		});
		playPatternList.add(new PlayPattern() {
			@Override
			public void playPattern(Player s, String[] cc, long duracao) throws InterruptedException {
				if (cc.length < 5) {
					s.play(cc[0], duracao);
					s.play(cc[2], duracao);
					s.play(cc[3], duracao);
					s.play(cc[2], duracao);
				} else {
					s.play(cc[0], duracao);
					s.play(cc[2], duracao);
					s.play(cc[4], duracao);
					s.play(cc[2], duracao);
				}
			}
		});
	}

	private void playPattern(String[] cc) throws InterruptedException {
		playPatternList.get(currentPattern).playPattern(this.player,  cc,  duracao);
	}

	private void updateSetup(char c) {
		if (setupvar == 'v') {
			volume += c == '=' ? 10 : -10;
		} else if (setupvar == 'n') {
			transpose += c == '=' ? 1 : -1;
		} else if (setupvar == 'm') {
			duracao += c == '=' ? 10 : -10;
		} else if (setupvar == 'i') {
			int i = instrument;
			instrument += c == '=' ? 1 : -1;
			if (instrument < 0 || instrument >= channels.length) {
				instrument = i;
			}
		} else if (setupvar == 'l') {
			int i = currentPattern;
			currentPattern += c == '=' ? 1 : -1;
			if (currentPattern < 0 || currentPattern >= playPatternList.size()) {
				currentPattern = i;
			}
		}
		updateStatus();
	}

	boolean playingContents = false;
	
	private void playContents() {
		player.pause();
		String text = ta.getText();
		for (int i = 0; i < text.length(); i++) {
			String[] cc = chords.get(text.charAt(i));
			if (cc == null) {
				continue;
			}
			try {
				playPattern(cc);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		player.unpause();
	}

	private void updateStatus() {
		status.setText("Setup:" + setupvar + " Trans(n):" + transpose + "(" + getTransposedNote() + ")" + " Volume(v):"
				+ volume + " Time(m):" + duracao + " Instr(i):" + instrument + " Pattern(l):" + currentPattern
				+ " Clear: Ctrl+l");
	}

	private String getTransposedNote() {
		int t = transpose;
		while (t < 0) {
			t += 12;
		}
		return notes.get(t % 12);
	}
//
//	private void play(String[] note, long duracao2) throws InterruptedException {
//		for (int i = 0; i < note.length; i++) {
//			channels[instrument].noteOn(id(note[i]) + transpose, volume);
//		}
//		Thread.sleep(duracao2);
//		for (int i = 0; i < note.length; i++) {
//			channels[instrument].noteOff(id(note[i]) + transpose);
//		}
//	}
//
//	/**
//	 * Plays the given note for the given duration
//	 */
//	private void play(String note, long duration) throws InterruptedException {
//		// * start playing a note
//		channels[instrument].noteOn(id(note) + transpose, volume);
//		// * wait
//		Thread.sleep(duration);
//		// * stop playing a note
//		channels[instrument].noteOff(id(note) + transpose);
//	}
//
//	/**
//	 * Plays nothing for the given duration
//	 */
//	private void rest(long duracao2) throws InterruptedException {
//		Thread.sleep(duracao2);
//	}
//
	/**
	 * Returns the MIDI id for a given note: eg. 4C -> 60
	 * 
	 * @return
	 */
	private int id(String note) {
		int octave = Integer.parseInt(note.substring(0, 1));
		return notes.indexOf(note.substring(1)) + 12 * octave + 12;
	}
}

interface PlayPattern {
	void playPattern(Player s, String[] cc, long duracao) throws InterruptedException;
}