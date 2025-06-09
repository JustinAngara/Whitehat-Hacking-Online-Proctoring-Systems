package com.cs.main;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class DisplayProcessesGUI extends JFrame {
    private ProcessHandler ph;
    private DefaultListModel<String> mainListModel;
    private DefaultListModel<String> sideListModel;
    private JList<String> mainList;
    private JList<String> sideList;

    public DisplayProcessesGUI() {
        setTitle("Display Processes");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Display Processes", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        add(titleLabel, BorderLayout.NORTH);

        mainListModel = new DefaultListModel<>();
        mainList = new JList<>(mainListModel);
        JScrollPane mainScrollPane = new JScrollPane(mainList);

        sideListModel = new DefaultListModel<>();
        sideList = new JList<>(sideListModel);
        JScrollPane sideScrollPane = new JScrollPane(sideList);

        mainList.setFont(new Font("Monospaced", Font.PLAIN, 14));
        sideList.setFont(new Font("Monospaced", Font.PLAIN, 14));

        // Labels above each list
        JLabel mainListLabel = new JLabel("Process Details");
        mainListLabel.setFont(new Font("Arial", Font.BOLD, 16));
        JLabel sideListLabel = new JLabel("Process Names");
        sideListLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(mainListLabel, BorderLayout.NORTH);
        mainPanel.add(mainScrollPane, BorderLayout.CENTER);

        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.add(sideListLabel, BorderLayout.NORTH);
        sidePanel.add(sideScrollPane, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.add(mainPanel, BorderLayout.CENTER);
        centerPanel.add(sidePanel, BorderLayout.EAST);

        mainScrollPane.setPreferredSize(new Dimension(600, 500));
        sideScrollPane.setPreferredSize(new Dimension(180, 500));

        add(centerPanel, BorderLayout.CENTER);

        // Add button to bottom right
        JButton actionButton = new JButton("Perform Action");
        actionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(DisplayProcessesGUI.this, "Action button clicked!");
            }
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(actionButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // Dynamic population methods
    public void updateMainList(List<String> items) {
        mainListModel.clear();
        for (String item : items) {
            mainListModel.addElement(item);
        }
    }

    public void updateSideList(List<String> processes) {
        sideListModel.clear();
        for (String process : processes) {
            sideListModel.addElement(process);
        }
    }


    public void parseProcessToString(){
        ph = new ProcessHandler();
        ph.getProcessName();
//        ph.checkProcessDisplayAffinity("java.exe");
        System.out.println("This will be the process handler:\n"+ph.getProcessList());

    }

    public static void main(String[] args) {


        SwingUtilities.invokeLater(() -> {
            DisplayProcessesGUI gui = new DisplayProcessesGUI();
            gui.setVisible(true);

            // Example population
            gui.updateMainList(List.of("Process ID: 1234", "Memory Usage: 120MB", "Thread Count: 10"));
            gui.updateSideList(List.of("explorer.exe", "chrome.exe", "javaw.exe"));



            gui.updateMainList(List.of("Process ID: 5343234234324", "Memory Usage: 120MB", "Thread Count: 10"));
            gui.updateSideList(List.of("explorer.exe", "chr23423432ome.exe", "javaw.exe"));
        });
    }
}