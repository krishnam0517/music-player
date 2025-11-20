import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javazoom.jl.player.advanced.*;

public class music_player extends JFrame {
    JTextField searchField;
    DefaultListModel<String> playlistModel, songModel;
    JList<String> playlistList, songList;
    Map<String, List<String>> playlists = new HashMap<>();
    String currentPlaylist = null, currentSong = null;

    AdvancedPlayer player;
    Thread playThread;
    boolean isPaused = false;
    long pauseLocation = 0;
    long songTotalLength = 0;
    File currentFile;
    FileInputStream fis;
    int progressValue = 0;
    boolean isResuming = false;

    JButton playBtn, stopBtn, nextBtn, prevBtn, pauseBtn;
    JLabel nowPlayingLabel;
    JProgressBar progressBar;
    Timer progressTimer;

    public music_player() {
        setTitle("🎧 Music Player");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLayout(new BorderLayout(10, 10));


        Color bg = new Color(20, 20, 30);
        Color panelDark = new Color(30, 30, 45);
        Color accent = new Color(0, 255, 180);
        Color accent2 = new Color(120, 80, 255);
        Color textColor = new Color(230, 230, 230);
        getContentPane().setBackground(bg);


        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setBackground(panelDark);
        top.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel searchLabel = new JLabel("🔍 Search:");
        searchLabel.setForeground(Color.WHITE);
        searchField = new JTextField();
        searchField.addActionListener(e -> searchSong());
        searchField.setBackground(new Color(45, 45, 65));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(accent);
        searchBtn.setFocusPainted(false);
        top.add(searchLabel, BorderLayout.WEST);
        top.add(searchField, BorderLayout.CENTER);
        top.add(searchBtn, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);


        JPanel left = new JPanel(new BorderLayout(5, 5));
        left.setBackground(panelDark);
        left.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel plabel = new JLabel("🎶 Playlists");
        plabel.setForeground(Color.WHITE);
        playlistModel = new DefaultListModel<>();
        playlistList = new JList<>(playlistModel);
        styleList(playlistList, textColor);
        JScrollPane plScroll = new JScrollPane(playlistList);
        JButton addPlBtn = new JButton("+ Add Playlist");
        addPlBtn.setBackground(accent2);
        addPlBtn.setForeground(Color.WHITE);
        addPlBtn.setFocusPainted(false);
        left.add(plabel, BorderLayout.NORTH);
        left.add(plScroll, BorderLayout.CENTER);
        left.add(addPlBtn, BorderLayout.SOUTH);
        add(left, BorderLayout.WEST);


        JPanel center = new JPanel(new BorderLayout(5, 5));
        center.setBackground(panelDark);
        center.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel slabel = new JLabel("🎧 Songs");
        slabel.setForeground(Color.WHITE);
        songModel = new DefaultListModel<>();
        songList = new JList<>(songModel);
        styleList(songList, textColor);
        JScrollPane sScroll = new JScrollPane(songList);
        center.add(slabel, BorderLayout.NORTH);
        center.add(sScroll, BorderLayout.CENTER);


        JPanel right = new JPanel(new BorderLayout(5, 5));
        right.setBackground(panelDark);
        right.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel npLabel = new JLabel("🎵 Now Playing");
        npLabel.setForeground(Color.WHITE);
        nowPlayingLabel = new JLabel("None");
        nowPlayingLabel.setForeground(accent);
        nowPlayingLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        right.add(npLabel, BorderLayout.NORTH);
        right.add(nowPlayingLabel, BorderLayout.CENTER);


        JPanel mid = new JPanel(new GridLayout(1, 2, 10, 0));
        mid.setBackground(bg);
        mid.add(center);
        mid.add(right);
        add(mid, BorderLayout.CENTER);


        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(panelDark);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        controls.setBackground(panelDark);

        prevBtn = controlButton("⏮️");
        playBtn = controlButton("▶️");
        pauseBtn = controlButton("⏸️");
        stopBtn = controlButton("⏹️");
        nextBtn = controlButton("⏭️");

        controls.add(prevBtn);
        controls.add(playBtn);
        controls.add(pauseBtn);
        controls.add(stopBtn);
        controls.add(nextBtn);

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(950, 10));
        progressBar.setForeground(accent2);
        progressBar.setBackground(new Color(40, 40, 50));
        progressBar.setBorderPainted(false);

        bottom.add(controls, BorderLayout.NORTH);
        bottom.add(progressBar, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);


        addPlBtn.addActionListener(e -> addPlaylistFromFolder());
        playlistList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                currentPlaylist = playlistList.getSelectedValue();
                loadSongs();
            }
        });
        songList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && currentPlaylist != null) {
                    currentSong = songList.getSelectedValue();
                    playSelectedSong(false);
                }
            }
        });
        searchBtn.addActionListener(e -> searchSong());
        playBtn.addActionListener(e -> {
            if (isPaused) resumeSong();
            else if (currentSong != null) playSelectedSong(false);
        });
        stopBtn.addActionListener(e -> stopSong());
        pauseBtn.addActionListener(e -> pauseSong());
        nextBtn.addActionListener(e -> nextSong());
        prevBtn.addActionListener(e -> prevSong());
    }

    public void styleList(JList<String> list, Color textColor) {
        list.setBackground(new Color(40, 40, 55));
        list.setForeground(textColor);
        list.setSelectionBackground(new Color(70, 70, 100));
        list.setSelectionForeground(Color.WHITE);
        list.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    public JButton controlButton(String icon) {
        JButton b = new JButton(icon);
        b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        b.setBackground(new Color(45, 45, 65));
        b.setForeground(Color.WHITE);
        b.setBorder(new LineBorder(new Color(80, 80, 120), 1, true));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(60, 60));
        return b;
    }


    public void addPlaylistFromFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            String name = folder.getName();
            List<String> songs = new ArrayList<>();
            for (File f : Objects.requireNonNull(folder.listFiles())) {
                if (f.getName().toLowerCase().endsWith(".mp3"))
                    songs.add(f.getAbsolutePath());
            }
            if (songs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No mp3 files in this folder!");
                return;
            }
            playlists.put(name, songs);
            playlistModel.addElement(name);
            currentPlaylist = name;
            loadSongs();
        }
    }

    public void loadSongs() {
        songModel.clear();
        if (currentPlaylist == null) return;
        for (String s : playlists.get(currentPlaylist))
            songModel.addElement(new File(s).getName());
    }

    public void searchSong() {
        if (currentPlaylist == null) return;
        String q = searchField.getText().toLowerCase();
        songModel.clear();
        for (String s : playlists.get(currentPlaylist))
            if (new File(s).getName().toLowerCase().contains(q))
                songModel.addElement(new File(s).getName());
    }

    public void playSelectedSong(boolean resume) {
        stopSong();
        try {
            currentFile = playlists.get(currentPlaylist).stream()
                    .filter(p -> new File(p).getName().equals(currentSong))
                    .map(File::new).findFirst().orElse(null);
            if (currentFile == null) return;
            nowPlayingLabel.setText(currentSong);
            fis = new FileInputStream(currentFile);
            songTotalLength = fis.available();
            if (resume) {
                fis.skip(songTotalLength - pauseLocation);
                isResuming = true;
            } else {
                isResuming = false;
                progressValue = 0;
            }
            player = new AdvancedPlayer(fis);
            playThread = new Thread(() -> {
                try {
                    player.play();
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()));
                }
            });
            playThread.start();
            startProgress();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void startProgress() {
        stopProgress();
        if (!isResuming)
            progressValue = 0;
        progressBar.setValue(progressValue);
        progressTimer = new Timer(800, e -> {
            if (player == null) return;
            progressValue++;
            progressBar.setValue(progressValue);
            if (progressValue >= 100)
                progressTimer.stop();
        });
        progressTimer.start();
    }

    public void stopProgress() {
        if (progressTimer != null) {
            progressTimer.stop();
            progressBar.setValue(0);
        }
    }

    public void stopSong() {
        try {
            if (player != null) player.close();
            if (playThread != null && playThread.isAlive()) playThread.interrupt();
            if (fis != null) fis.close();
        } catch (Exception ignored) {}
        stopProgress();
    }

    public void pauseSong() {
        try {
            if (player != null) {
                pauseLocation = fis.available();
                player.close();
                isPaused = true;
                isResuming = true;
                stopProgress();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void resumeSong() {
        if (currentFile != null && isPaused) {
            isPaused = false;
            playSelectedSong(true);
        }
    }

    public void nextSong() {
        isResuming = false;
        progressValue = 0;
        if (currentPlaylist == null || currentSong == null) return;
        int idx = songModel.indexOf(currentSong);
        if (idx < songModel.size() - 1) {
            currentSong = songModel.get(idx + 1);
            songList.setSelectedIndex(idx + 1);
            playSelectedSong(false);
        }
    }

    public void prevSong() {
        isResuming = false;
        progressValue = 0;
        if (currentPlaylist == null || currentSong == null) return;
        int idx = songModel.indexOf(currentSong);
        if (idx > 0) {
            currentSong = songModel.get(idx - 1);
            songList.setSelectedIndex(idx - 1);
            playSelectedSong(false);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            music_player p = new music_player();
            p.setVisible(true);
            p.setLocationRelativeTo(null);
        });
    }
}
