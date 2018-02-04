import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.event.*;

public class BeatBox {

JFrame theFrame;
JPanel mainPanel;
ArrayList<JCheckBox> checkboxList;
JList<String> incomingList;
JTextField userMessage;
int nextNum;
Vector<String> listVector = new Vector<String>();
String userName;
ObjectOutputStream out;
ObjectInputStream in;
HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();

Sequencer sequencer;
Sequence sequence;
Sequence mySequence = null;
Track track;
String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"};
int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};

public static void main(String[] args) {
new BeatBox().startUp(setUserID());
}//end main

public static String setUserID() {
JFrame frame = new JFrame();
frame.setVisible(false);
while(true) {
String s = (String) JOptionPane.showInputDialog(frame, "Choose User ID (12 or less characters): ", "Cyber BeatBox", JOptionPane.PLAIN_MESSAGE, null, null, "");
if((s != null) && (s.length() > 0) && (s.length() < 13)) {
frame.dispose();
return s;
}
}
}

public void startUp(String name) {
boolean isOnline = true;
userName = name;
//open connection to server
try {
Socket sock = new Socket("127.0.0.1", 4242);
out = new ObjectOutputStream(sock.getOutputStream());
in = new ObjectInputStream(sock.getInputStream());
Thread remote = new Thread(new RemoteReader());
remote.start();
} catch(Exception ex) {
System.out.println("couldn't connect - you'll have to play alone.");
isOnline = false;
}
setUpMidi();
buildGUI(isOnline);
}//close startUp

public void buildGUI(boolean isOnline) {
if (isOnline) {
theFrame = new JFrame("Cyber BeatBox - Online");
} else {
theFrame = new JFrame("Cyber BeatBox - Offline");
}
theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
BorderLayout layout = new BorderLayout();
JPanel background = new JPanel(layout);
background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

checkboxList = new ArrayList<JCheckBox>();
Box buttonBox = new Box(BoxLayout.Y_AXIS);

//add buttons
JButton start = new JButton("Start");
start.addActionListener(new MyStartListener());
buttonBox.add(start);

JButton stop = new JButton("Stop");
stop.addActionListener(new MyStopListener());
buttonBox.add(stop);

JButton upTempo = new JButton("Tempo Up");
upTempo.addActionListener(new MyUpTempoListener());
buttonBox.add(upTempo);

JButton downTempo = new JButton("Tempo Down");
downTempo.addActionListener(new MyDownTempoListener());
buttonBox.add(downTempo);

JButton save = new JButton("Save");
save.addActionListener(new MySaveListener());
buttonBox.add(save);

JButton load = new JButton("Load");
load.addActionListener(new MyLoadListener());
buttonBox.add(load);

JButton sendIt = new JButton("Send It");
sendIt.addActionListener(new MySendListener());
buttonBox.add(sendIt);

userMessage = new JTextField();
buttonBox.add(userMessage);

incomingList = new JList<String>();
incomingList.addListSelectionListener(new MyListSelectionListener());
incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
JScrollPane theList = new JScrollPane(incomingList);
buttonBox.add(theList);
incomingList.setListData(listVector);//no data to start with

Box nameBox = new Box(BoxLayout.Y_AXIS);
for (int i = 0;i < 16;i++) {
nameBox.add(new Label(instrumentNames[i]));
}

//build frame
background.add(BorderLayout.EAST, buttonBox);
background.add(BorderLayout.WEST, nameBox);
theFrame.getContentPane().add(background);

GridLayout grid = new GridLayout(16,16);
grid.setVgap(1);
grid.setHgap(2);
mainPanel = new JPanel(grid);
background.add(BorderLayout.CENTER, mainPanel);

//add checkboxes to arraylist AND gui (set unchecked)
for (int i = 0;i < 256;i++) {
JCheckBox c = new JCheckBox();
c.setSelected(false);
checkboxList.add(c);
mainPanel.add(c);
}

setUpMidi();

theFrame.setBounds(50,50,300,300);
theFrame.pack();
theFrame.setVisible(true);
}//close buildGUI

public void setUpMidi() {
try {
sequencer = MidiSystem.getSequencer();
sequencer.open();
sequence = new Sequence(Sequence.PPQ, 4);
track = sequence.createTrack();
sequencer.setTempoInBPM(120);
} catch(Exception e) {e.printStackTrace();}
}//close setUpMidi

public void buildTrackAndStart() {
ArrayList<Integer> trackList = null;
sequence.deleteTrack(track);
track = sequence.createTrack();

for (int i = 0;i < 16;i++) {
trackList = new ArrayList<Integer>();
for (int j = 0;j < 16;j++) {
JCheckBox jc = (JCheckBox) checkboxList.get(j + (16*i));
if (jc.isSelected()) {
int key = instruments[i];
trackList.add(new Integer(key));
} else {
trackList.add(null);
}//end if
}//close inner loop
makeTracks(trackList);
track.add(makeEvent(176,1,127,0,16));
}//close outer loop

track.add(makeEvent(192,9,1,0,15));
try {
sequencer.setSequence(sequence);
sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
sequencer.start();
sequencer.setTempoInBPM(120);
} catch(Exception e) {e.printStackTrace();}
}//close buildTrackAndStart

public class MyStartListener implements ActionListener {
public void actionPerformed(ActionEvent a) {
buildTrackAndStart();
}
}//close inner class

public class MyStopListener implements ActionListener {
public void actionPerformed(ActionEvent a) {
sequencer.stop();
}
}//close inner class

public class MyUpTempoListener implements ActionListener {
public void actionPerformed(ActionEvent a) {
float tempoFactor = sequencer.getTempoFactor();
sequencer.setTempoFactor((float) (tempoFactor * 1.03));
}
}//close inner class

public class MyDownTempoListener implements ActionListener {
public void actionPerformed(ActionEvent a) {
float tempoFactor = sequencer.getTempoFactor();
sequencer.setTempoFactor((float) (tempoFactor * .97));
}
}//close inner class

public class MySaveListener implements ActionListener {
public void actionPerformed(ActionEvent a) {
boolean[] checkboxState = new boolean[256];
for (int i = 0;i < 256;i++) {
JCheckBox check = (JCheckBox) checkboxList.get(i);
if (check.isSelected()) {
checkboxState[i] = true;
}
}
JFileChooser fileSave = new JFileChooser();
fileSave.showSaveDialog(theFrame);
saveFile(fileSave.getSelectedFile(), checkboxState);
}
}//close inner class

public class MyLoadListener implements ActionListener {
public void actionPerformed(ActionEvent a) {
JFileChooser fileLoad = new JFileChooser();
fileLoad.showOpenDialog(theFrame);
loadFile(fileLoad.getSelectedFile());
}
}//close inner class

public class MySendListener implements ActionListener {
public void actionPerformed(ActionEvent a) {
boolean[] checkboxState = new boolean[256];
for (int i = 0;i < 256;i++) {
JCheckBox check = (JCheckBox) checkboxList.get(i);
if (check.isSelected()) {
checkboxState[i] = true;
}
}
String messageToSend = null;
try {
out.writeObject(userName + nextNum++ + ": " + userMessage.getText());
out.writeObject(checkboxState);
} catch(Exception ex) {
System.out.println("Unable to send to server.");
}
userMessage.setText("");
}
}//close MySendListener

public class MyListSelectionListener implements ListSelectionListener {
public void valueChanged (ListSelectionEvent le) {
if (!le.getValueIsAdjusting()) {
String selected = (String) incomingList.getSelectedValue();
if(selected != null) {
//now go to the map, and change the sequence
boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
changeSequence(selectedState);
sequencer.stop();
buildTrackAndStart();
}
}
}//close valueChanged
}//close MyListSelectionListener

public class RemoteReader implements Runnable {
boolean[] checkboxState = null;
String nameToShow = null;
Object obj = null;
public void run() {
try {
while ((obj=in.readObject()) != null) {
System.out.println("got an object from server");
System.out.println(obj.getClass());
String nameToShow = (String) obj;
checkboxState = (boolean[]) in.readObject();
otherSeqsMap.put(nameToShow, checkboxState);
listVector.add(nameToShow);
incomingList.setListData(listVector);
}
} catch(Exception ex) {ex.printStackTrace();}
}//close run
}//close RemoteReader

private void saveFile(File file, boolean[] cbs) {
try {
ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file));
os.writeObject(cbs);
os.close();
} catch(Exception ex) {ex.printStackTrace();}
}//close saveFile

private void loadFile(File file) {
//load checkbox map
boolean[] checkboxState = null;
try {
ObjectInputStream is = new ObjectInputStream(new FileInputStream(file));
checkboxState = (boolean[]) is.readObject();
is.close();
} catch(Exception ex) {ex.printStackTrace();}
//apply checkbox map to gui
for (int i = 0;i < 256;i++) {
JCheckBox check = (JCheckBox) checkboxList.get(i);
if (checkboxState[i]) {
check.setSelected(true);
} else {
check.setSelected(false);
}
}
sequencer.stop();
buildTrackAndStart();
}

public void changeSequence(boolean[] checkboxState) {
for (int i = 0;i < 256;i++) {
JCheckBox check = (JCheckBox) checkboxList.get(i);
if (checkboxState[i]) {
check.setSelected(true);
} else {
check.setSelected(false);
}
}
}//close changeSequence

public void makeTracks(ArrayList<Integer> list) {
Iterator it = list.iterator();
for(int i = 0;i < 16;i++) {
Integer num = (Integer) it.next();
if (num != null) {
int numKey = num.intValue();
track.add(makeEvent(144,9,numKey,100,i));
track.add(makeEvent(128,9,numKey,100,i+1));
}
}
}//close makeTracks

public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
MidiEvent event = null;
try {
ShortMessage a = new ShortMessage();
a.setMessage(comd, chan, one, two);
event = new MidiEvent(a, tick);
} catch(Exception e) {e.printStackTrace();}
return event;
}//close makeEvent
}//close class