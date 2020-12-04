package br.com.ritcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

public class TimePlayer {

	public static void main(String[] args) throws MidiUnavailableException, IOException {
		
		Synthesizer synth = MidiSystem.getSynthesizer();
		synth.open();
		MidiChannel[] channels = synth.getChannels();
		new TimePlayer(channels[0]).play(args[0]);
		synth.close();
	}
	
	class Note {
		int time;
		int note = 60;
		String noteName = "c";
		boolean rest = false;
		@Override
		public String toString() {
			return (rest? "r":"") + noteName+ ":"+String.valueOf(time) ;
		}
	}
	
	
	private Pattern notePattern;
	private int[] noteLevels;
	MidiChannel channel = null;

	private void play(String file) throws MidiUnavailableException, IOException {
		List<Note> notes = new ArrayList<Note>();
		Pattern pattern = Pattern.compile("(-?[1-4]?[a-g][#b]?)(r|\\.)+([0-9]+)");
		BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
		StringBuffer contents = new StringBuffer();
		String line;
		while((line = reader.readLine()) != null) {
			contents.append(line + " ");
		}
		reader.close();
		
		String[] tokens = contents.toString().split("[\\s]+");
		
		int bpm = Integer.parseInt(tokens[0]);
		for (int i = 1; i < tokens.length; i++) {
			Matcher matcher = pattern.matcher(tokens[i]);
			if(!matcher.matches()) {
				continue;
			}
			Note n = new Note();
			n.noteName = matcher.group(1);
			n.note = noteValue(n.noteName);
			n.rest = "r".equals(matcher.group(2));
			n.time = Integer.parseInt(matcher.group(3));
			
			notes.add(n);
		}
		
		
		System.out.println(bpm);
		int time4 = (4*60*1000)/bpm;
		System.out.println(time4);
		synchronized (this) {
			for (Iterator<Note> iterator = notes.iterator(); iterator.hasNext();) {
				Note n = iterator.next();
				if(!n.rest) {
					channel.noteOn(n.note, 100);
				}
				try {
					System.out.println(n);
					Thread.sleep(time4/n.time);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(!n.rest) {
					channel.noteOff(n.note, 100);
				}
			}
			try {
				Thread.sleep(time4/4);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public TimePlayer(MidiChannel channel) {
		this.channel = channel;
		this.notePattern = Pattern.compile("(-?[1-4]?)([a-g])([#b]?)");
		this.noteLevels = new int[] {9, 11, 0, 2, 4, 5, 7};
	}
	
	private int noteValue(String group) {
		Matcher matcher = notePattern.matcher(group);
		if(!matcher.matches()) {
			return 0;
		}
		String levelG = matcher.group(1);
		String noteG = matcher.group(2);
		String sharpG = matcher.group(3);
	
		int level = 60;
		if(!"".equals(levelG)) {
			level = 60 + Integer.parseInt(levelG) * 12;
		}
		
		if(!"".equals(noteG)) {
			level += noteLevels[noteG.toLowerCase().charAt(0) - 'a'];
		}
		
		if(!"".equals(sharpG)) {
			level += "#".equals(sharpG)? 1: -1;
		}
		
		return level;
		
	}

}
