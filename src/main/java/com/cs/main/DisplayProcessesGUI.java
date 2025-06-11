package com.cs.main;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        setAlwaysOnTop(true);


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
        JLabel mainListLabel = new JLabel("Background Processes");
        mainListLabel.setFont(new Font("Arial", Font.BOLD, 16));
        JLabel sideListLabel = new JLabel("Hidden Applications");
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

        // add button to bottom right
        JButton actionButton = new JButton("Update data");
        actionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parseProcessToString();
                JOptionPane.showMessageDialog(DisplayProcessesGUI.this, "Loaded new data!");
            }
        });



        // Add buttons to bottom panel
        JButton revealButton = new JButton("Reveal");
        revealButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RemoveStealth.removeStealthByProcessName("cluely.exe");

                JOptionPane.showMessageDialog(DisplayProcessesGUI.this, "Reveal button clicked!");
            }
        });


        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(actionButton);
        bottomPanel.add(revealButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // dynamic population methods
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

    /**
     * Method used for changing/updating contents of the processes
     * */
    public void parseProcessToString() {
        ph = new ProcessHandler();
        ph.getProcessName();

        List<ProcessData> allProcesses = ph.getProcessList();

        // LHS list: all processes in readable format
        List<String> mainDescriptions = allProcesses.stream()
                .map(ProcessData::toString)
                .toList();
        updateMainList(mainDescriptions);

        // RHS List: only processes with exclusion flag
        List<String> excludedProcesses = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        for (ProcessData process : allProcesses) {
            String name = process.getProcessName();
            // avoid redundant checks
            if (seenNames.add(name)) {
                // checks if it is excluded from capture
                boolean isVisible = ph.checkProcessDisplayAffinity(name);
                if (!isVisible) {
                    excludedProcesses.add(name);
                }
            }
        }

        updateSideList(excludedProcesses);



    }

    public void display(){
        Timer autoRefreshTimer = new Timer(7000, e -> parseProcessToString());
        autoRefreshTimer.start();
    }




    public static void main(String[] args) {


        SwingUtilities.invokeLater(() -> {
            DisplayProcessesGUI gui = new DisplayProcessesGUI();
            gui.setVisible(true);

            // EXCLUDED FROM CAPTURE UPDATE DATA
            gui.parseProcessToString();
            gui.display();
        });
    }
}