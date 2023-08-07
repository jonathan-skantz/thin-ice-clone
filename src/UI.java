import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.text.DefaultFormatter;

public class UI {

    private static final Font fontConfig = new Font("verdana", Font.PLAIN, 12);

    private static final int SLIDER_WIDTH = 100;
    private static final int SLIDER_HEIGHT = 250;

    // save as fields in order to modify when setting size
    private static JLabel pathLengthLabel;

    public static JCheckBox[] hostCheckboxes = new JCheckBox[5];
    public static JSpinner hostHintLength;

    public static JButton buttonMazeConfig;

    private static JSlider[] amountSliders = new JSlider[3];
    private static JLabel[] amountLabels = new JLabel[3];
    private static final String[] labelPrefixes = new String[] {"Ground: ", "Doubles: ", "Walls: "};

    private static JCheckBox endMustBeDoubleCheckbox;

    // constraints
    private static Insets insets = new Insets(5, 5, 5, 5);

    private static GridBagConstraints horizontalGbcCol1 = new GridBagConstraints();
    private static GridBagConstraints horizontalGbcCol2 = new GridBagConstraints();

    private static GridBagConstraints verticalGbc = new GridBagConstraints();
    
    public static JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

    static {
        horizontalGbcCol1.gridx = 0;
        horizontalGbcCol1.insets = insets;

        horizontalGbcCol2.gridx = 1;
        horizontalGbcCol2.insets = insets;

        verticalGbc.gridx = 0;
        verticalGbc.anchor = GridBagConstraints.CENTER;
    }

    public static void setupConfigs() {

        // set default fonts for components
        UIDefaults defaults = UIManager.getDefaults();
        for (Object key : defaults.keySet()) {
            if (defaults.get(key) instanceof Font) {
                defaults.put(key, fontConfig);
            }
        }

        setupCompetitiveButton();
        setupKeyConfig();
        setupMazeConfig();

        buttons.setSize(Window.width, buttons.getPreferredSize().height);
        buttons.setLocation(0, Window.height - buttons.getHeight());
        buttons.setOpaque(false);
        Window.sprites.add(buttons);
    }

    private static void setupCompetitiveButton() {
       
        JPanel panelCompetitive = new JPanel();
        panelCompetitive.setLayout(new BoxLayout(panelCompetitive, BoxLayout.Y_AXIS));
        panelCompetitive.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttons.add(getConfigPopupButton("Competitive", panelCompetitive));
        
        // --- connection ---
        final Border borderPadding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        JPanel panelConnection = new JPanel();
        panelConnection.setAlignmentX(Component.LEFT_ALIGNMENT);

        Border b = BorderFactory.createEmptyBorder(0, 0, 20, 0);
        Border comb = BorderFactory.createCompoundBorder(b, BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Connection"), borderPadding));
        panelConnection.setBorder(comb);
        panelConnection.setLayout(new BoxLayout(panelConnection, BoxLayout.Y_AXIS));
        panelCompetitive.add(panelConnection);

        // local gamemode
        JCheckBox cbLocal = new JCheckBox("Local");
        JCheckBox cbJoin = new JCheckBox("Join (IP:port): ");
        JCheckBox cbHost = new JCheckBox("Host: " + OnlineServer.LOCAL_IP + ":");

        cbLocal.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            Config.multiplayerOffline = selected;
            Config.multiplayer = Config.multiplayerOnline || Config.multiplayerOffline;
            cbJoin.setEnabled(!selected);
            cbHost.setEnabled(!selected);

            if (selected) {
                Main.setToLocalGamemode();
            }
            else {
                Main.setToSingleplayer();
            }
        });
        cbLocal.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelConnection.add(cbLocal);

        // online: join
        FlowLayout subPanelLayout = new FlowLayout(FlowLayout.LEFT, cbLocal.getInsets().left, cbLocal.getInsets().top);
        Border emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);
        JPanel subPanel = new JPanel(subPanelLayout);
        
        JTextField textFieldJoin = new JTextField(OnlineServer.LOCAL_IP + ":12345", 15);
        cbJoin.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            Config.multiplayerOnline = selected;
            Config.multiplayer = Config.multiplayerOnline || Config.multiplayerOffline;
            cbLocal.setEnabled(!selected);
            cbHost.setEnabled(!selected);

            textFieldJoin.setEnabled(!selected);

            if (selected) {

                InetSocketAddress address = stringToInetSocketAddress(textFieldJoin.getText());

                if (address == null) {
                    cbJoin.setSelected(false);  // immediately revert UI changes
                }
                else {
                    OnlineClient.connect(address);
                }
            }
            else {
                OnlineClient.disconnect();
            }
        });
        cbJoin.setBorder(emptyBorder);
        subPanel.add(cbJoin);
        subPanel.add(textFieldJoin);
        subPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelConnection.add(subPanel);
        
        // online: host
        subPanel = new JPanel(subPanelLayout);
        JSpinner textFieldHost = getNumberSpinner(12345, 1, 65535, 1, 75);
        cbHost.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            Config.multiplayerOnline = selected;
            Config.multiplayer = Config.multiplayerOnline || Config.multiplayerOffline;
            cbLocal.setEnabled(!selected);
            cbJoin.setEnabled(!selected);
            textFieldHost.setEnabled(!selected);

            if (selected) {
                int port = (int)(double)textFieldHost.getValue();
                OnlineServer.open(port);
            }
            else {
                OnlineServer.close();
            }
        });
        cbHost.setBorder(emptyBorder);
        subPanel.add(cbHost);
        subPanel.add(textFieldHost);
        subPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelConnection.add(subPanel);


        // --- Settings ----
        JPanel panelSettings = new JPanel();
        panelSettings.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelSettings.setLayout(new BoxLayout(panelSettings, BoxLayout.Y_AXIS));
        panelSettings.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Host settings"), borderPadding));
        panelCompetitive.add(panelSettings);

        Config.Host[] settings = Config.Host.values();
        for (int i=0; i<settings.length-1; i++) {       // NOTE: <-1 since first has different callback and last is HINT_LENGTH (not togglable)
            hostCheckboxes[i] = new JCheckBox(settings[i].toString());
            hostCheckboxes[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            hostCheckboxes[i].setSelected(settings[i].enabled);
            panelSettings.add(hostCheckboxes[i]);

            final int ii = i;

            if (i == 0) {
                hostCheckboxes[i].addItemListener(e -> {
                    // adds a 2nd
                    Main.mazeRight.updateMirror();
                });
            }

            else if (i == 1) {
                hostCheckboxes[i].addItemListener(e -> {
                    Main.mazeLeft.updateAnimationsEnabled();
                    Main.mazeRight.updateAnimationsEnabled();
                });

            }

            // common for all (this is also called first)
            hostCheckboxes[i].addItemListener(e -> {
                settings[ii].enabled = e.getStateChange() == ItemEvent.SELECTED;
                OnlineServer.send(Config.Host.getSettings());
            });
        }
        
        // hint length spinner
        subPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        subPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel("Hint length: ");
        label.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 5));
        subPanel.add(label);

        hostHintLength = getNumberSpinner(Config.Host.HINT_LENGTH.number, 0, 1, 0.05f, 60);
        hostHintLength.addChangeListener(e -> {
            try {
                Config.Host.HINT_LENGTH.number = (float)hostHintLength.getValue();
            }
            catch (ClassCastException e1) {
                Config.Host.HINT_LENGTH.number = (float)(double)hostHintLength.getValue();
            }
            OnlineServer.send(Config.Host.getSettings());
        });
        
        subPanel.add(hostHintLength);
        panelSettings.add(subPanel);
    }
    
    private static JSpinner getNumberSpinner(float current, float min, float max, float step, int width) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(current, min, max, step));
        spinner.setPreferredSize(new Dimension(width, spinner.getPreferredSize().height));
        
        JFormattedTextField textField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        DefaultFormatter formatter = (DefaultFormatter) textField.getFormatter();
        formatter.setAllowsInvalid(false);
        return spinner;
    }

    private static InetSocketAddress stringToInetSocketAddress(String ipAndPort) {

        int i = ipAndPort.indexOf(':');

        if (i == -1) {
            return null;
        }

        try {
            int port = Integer.valueOf(ipAndPort.substring(i+1));
            
            String ip = ipAndPort.substring(0, i);
            InetSocketAddress address = new InetSocketAddress(ip, port);
    
            if (address.isUnresolved()) {
                return null;
            }
            return address;
        }
        catch (NumberFormatException e) {
            return null;
        }

    }

    public static void applyHostSettings(HashMap<Config.Host, Object> settings) {
        
        Config.Host[] localSettings = Config.Host.values();
        
        for (int i=0; i<localSettings.length-1; i++) {
            JCheckBox cb = hostCheckboxes[i];
            localSettings[i].enabled = (boolean)settings.get(localSettings[i]);
            cb.setEnabled(false);
            cb.setSelected(localSettings[i].enabled);
        }
        Config.Host.HINT_LENGTH.number = (float)settings.get(Config.Host.HINT_LENGTH);
        hostHintLength.setEnabled(false);
        hostHintLength.setValue(Config.Host.HINT_LENGTH.number);
        
        Main.mazeLeft.updateAnimationsEnabled();
        Main.mazeRight.updateAnimationsEnabled();
    }

    public static void setHostSettingsEnabled(boolean enabled) {
        for (JCheckBox cb : hostCheckboxes) {
            cb.setEnabled(enabled);
        }
        hostHintLength.setEnabled(enabled);
    }

    public static void setConfigsEnabled(boolean enabled) {
        for (Component btn : buttons.getComponents()) {
            btn.setEnabled(enabled);
        }
    }

    // ---------- KEY CONFIG ----------

    private static void setupKeyConfig() {

        JPanel panel = new JPanel(new GridBagLayout());

        setupKeyConfigKeybinds(panel);
        setupKeyConfigContinuousMovement(panel);

        // add config button to bottom left
        buttons.add(getConfigPopupButton("Key config", panel));
    }

    private static void setupKeyConfigKeybinds(JPanel panel) {
      
        // new label in 1st column, new btn in 2nd column
        for (KeyHandler.Action action : KeyHandler.Action.values()) {

            JLabel label = new JLabel(action.name());
            JButton btn = new JButton(KeyEvent.getKeyText(action.keyCode));
            
            label.setPreferredSize(new Dimension(125, label.getPreferredSize().height));
            btn.setPreferredSize(new Dimension(100, btn.getPreferredSize().height));
            
            panel.add(label, horizontalGbcCol1);
            panel.add(btn, horizontalGbcCol2);

            btn.addActionListener(e -> {
                // when clicked, change text to "?" and start listening for key press
                btn.setText("?");

                btn.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        // change control and stop listening
                        action.keyCode = e.getKeyCode();
                        btn.setText(KeyEvent.getKeyText(action.keyCode));
                        btn.removeKeyListener(this);
                        MazeContainer.Status.onNewControls();
                    }
                });
            });
        }

    }

    private static void setupKeyConfigContinuousMovement(JPanel panel) {
        
        // checkbox for continous key presses
        JLabel label = new JLabel("Allow continuous");
        label.setPreferredSize(new Dimension(125, label.getPreferredSize().height));

        JCheckBox cb = new JCheckBox(String.valueOf(KeyHandler.allowContinuous), KeyHandler.allowContinuous);
        cb.setPreferredSize(new Dimension(100, cb.getPreferredSize().height));

        cb.addItemListener(e -> {
            KeyHandler.allowContinuous = cb.isSelected();
        });

        panel.add(label, horizontalGbcCol1);
        panel.add(cb, horizontalGbcCol2);
    }



    // ---------- MAZE CONFIG ----------

    private static void setupMazeConfig() {

        JPanel panel = new JPanel(new GridBagLayout());
        
        // create surrounding border
        Border borderColor = BorderFactory.createLineBorder(new Color(150, 150, 150), 1, true);
        Border borderPad = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        Border border = BorderFactory.createCompoundBorder(borderPad, borderColor);
        
        JPanel leftCol = new JPanel();
        JPanel rightCol = new JPanel();

        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));

        // add node types subpanel
        JPanel panelNodeTypes = setupMazeConfigNodeTypes();
        panelNodeTypes.setBorder(BorderFactory.createTitledBorder(border, "Node types"));
        leftCol.add(panelNodeTypes);
        
        // add path length
        pathLengthLabel = new JLabel("Resulting path length: " + MazeGen.pathLength);
        JPanel container = new JPanel();
        container.add(pathLengthLabel);
        leftCol.add(container);
        
        // add sizes subpanel
        JPanel panelSizes = setupMazeConfigSizes();
        panelSizes.setBorder(BorderFactory.createTitledBorder(border, "Size"));
        rightCol.add(panelSizes);
        
        // add misc settings
        JPanel panelMisc = setupMazeMisc();
        panelMisc.setBorder(BorderFactory.createTitledBorder(border, "Miscellaneous"));
        rightCol.add(panelMisc);
        
        // finally, add left and right columns
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTH;

        panel.add(leftCol, c);
        panel.add(rightCol, c);

        // add config button to bottom right
        buttonMazeConfig = getConfigPopupButton("Maze config", panel);
        buttons.add(buttonMazeConfig);

    }

    private static JPanel setupMazeConfigNodeTypes() {

        JPanel subPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        
        // --- row 1: sliders, row 2: labels ---
        for (int i=0; i<3; i++) {
            MazeGen.Amount type = MazeGen.Amount.priority.get(i);

            c.gridy = 0;
            amountSliders[i] = getNewSlider(type.getMin(), type.getMax(), type.get());
            subPanel.add(amountSliders[i], c);
            
            c.gridy = 1;
            amountLabels[i] = new JLabel(labelPrefixes[i] + amountSliders[i].getValue());
            subPanel.add(amountLabels[i], c);

            int ii = i;
            amountSliders[i].addChangeListener(e -> {
                type.set(amountSliders[ii].getValue());
                updateNodeTypesSliders();
                updateEnabledEndMustBeDoubleCheckbox();
            });
        }

        amountSliders[2].setEnabled(false);

        // --- row 3: priority radio buttons ---
        JPanel row3 = setupMazeConfigRadioButtons();
        row3.setPreferredSize(new Dimension(subPanel.getPreferredSize().width, 100));
        row3.setBorder(BorderFactory.createTitledBorder("Priority"));

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 3;
        subPanel.add(row3, c);

        // --- row 4: checkboxes ---
        JPanel row4 = setupMazeConfigCheckboxes();
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 3;
        subPanel.add(row4, c);

        return subPanel;
    }

    private static JPanel setupMazeConfigRadioButtons() {
        
        JPanel row = new JPanel(new GridLayout(3, 1));
        
        ArrayList<JRadioButton[]> rbRows = new ArrayList<>();

        for (int y=0; y<3; y++) {

            ButtonGroup rbGroup = new ButtonGroup();
            JPanel rbPanel = new JPanel(new GridLayout(1, 3));
            row.add(rbPanel);

            JRadioButton[] rbRow = new JRadioButton[3];
            rbRows.add(rbRow);

            for (int x=0; x<3; x++) {
                JRadioButton rb = new JRadioButton(String.valueOf(y+1));
                rb.setSelected(MazeGen.Amount.priorityDefault.get(x) == MazeGen.Amount.priority.get(y));
                rb.setHorizontalAlignment(SwingConstants.CENTER);
                rbGroup.add(rb);
                rbPanel.add(rb);

                rbRows.get(y)[x] = rb;

                int iCol = x;

                rb.addItemListener(e -> {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        
                        int priority = rbRows.indexOf(rbRow);
                        MazeGen.Amount typePressed = MazeGen.Amount.priorityDefault.get(iCol);

                        int iSwap = typePressed.setPriority(priority);
                        JRadioButton[] rowChange = rbRows.get(iSwap);

                        MazeGen.Amount swapType = MazeGen.Amount.priority.get(iSwap);
                        JRadioButton rbChange = rowChange[MazeGen.Amount.priorityDefault.indexOf(swapType)];
                        
                        rbChange.setSelected(true);    // also deselects current

                        updateNodeTypesSliders();
                    }
                });
            }
        }

        return row;
    }

    private static JPanel setupMazeConfigCheckboxes() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));

        JCheckBox endCanBeDoubleCheckbox = new JCheckBox("End can be double");
        endCanBeDoubleCheckbox.setSelected(MazeGen.endCanBeDouble);
        JPanel endCanAndToolTip = getPanelWithToolTipAndCheckBox(endCanBeDoubleCheckbox,
                                "Requires at least one double node to have effect");

        endMustBeDoubleCheckbox = new JCheckBox("End must be double");
        endMustBeDoubleCheckbox.setSelected(MazeGen.endMustBeDouble);
        JPanel endMustAndToolTip = getPanelWithToolTipAndCheckBox(endMustBeDoubleCheckbox,
                            "Requires \"End can be double\" and at least one double node");

        updateEnabledEndMustBeDoubleCheckbox();

        row.add(Box.createVerticalStrut(25));
        row.add(endCanAndToolTip);
        row.add(Box.createVerticalStrut(5));
        row.add(endMustAndToolTip);
        row.add(Box.createVerticalStrut(5));

        // add callbacks
        endCanBeDoubleCheckbox.addItemListener(e -> {
            MazeGen.setEndCanBeDouble(e.getStateChange() == ItemEvent.SELECTED);

            MazeGen.Amount.DOUBLES.set(amountSliders[1].getValue());
            updateNodeTypesSliders();
            updateEnabledEndMustBeDoubleCheckbox();
        });
        
        endMustBeDoubleCheckbox.addItemListener(e -> {
            MazeGen.setEndMustBeDouble(e.getStateChange() == ItemEvent.SELECTED);
        });

        return row;
    }

    private static void updateEnabledEndMustBeDoubleCheckbox() {
        if (MazeGen.endCanBeDouble && MazeGen.Amount.DOUBLES.get() > 0) {
            endMustBeDoubleCheckbox.setEnabled(true);
        }
        else {
            endMustBeDoubleCheckbox.setEnabled(false);
        }
        endMustBeDoubleCheckbox.setSelected(MazeGen.endMustBeDouble);
    }

    private static JPanel setupMazeConfigSizes() {

        JPanel subPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);

        // --- row 1: sliders ---
        JSlider widthSlider = getNewSlider(Config.MAZE_WIDTH_MIN, Config.MAZE_WIDTH_MAX, MazeGen.width);
        JSlider heightSlider = getNewSlider(Config.MAZE_HEIGHT_MIN, Config.MAZE_HEIGHT_MAX, MazeGen.height);

        c.gridy = 0;
        subPanel.add(widthSlider, c);
        subPanel.add(heightSlider, c);

        // --- row 2: labels ---
        JLabel widthLabel = new JLabel("Width: " + widthSlider.getValue());
        JLabel heightLabel = new JLabel("Height: " + heightSlider.getValue());
        
        c.gridy = 1;
        subPanel.add(widthLabel, c);
        subPanel.add(heightLabel, c);

        // add callbacks
        widthSlider.addChangeListener(e -> {
            int val = widthSlider.getValue();
            widthLabel.setText("Width: " + val);
            MazeGen.setWidth(val);
            updateNodeTypesSliders();
        });
        
        heightSlider.addChangeListener(e -> {
            int val = heightSlider.getValue();
            heightLabel.setText("Height: " + val);
            MazeGen.setHeight(val);
            updateNodeTypesSliders();
        });


        return subPanel;
    }

    private static void setHintTypeText(JButton btn) {
        if (Config.hintTypeLongest) {
            btn.setText("Hint path type: Longest path");
        }
        else {
            btn.setText("Hint path type: Shortest path");
        }
    }

    private static JPanel setupMazeMisc() {

        JPanel subPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        
        // --- btn 1: hint type ---
        JButton btnHintType = new JButton();

        setHintTypeText(btnHintType);
        c.gridy = 0;
        subPanel.add(btnHintType, c);
        
        // add callback
        btnHintType.addActionListener(e -> {
            Config.hintTypeLongest = !Config.hintTypeLongest;
            setHintTypeText(btnHintType);
        });

        // --- checkboxes ---
        JPanel checkboxes = new JPanel();
        checkboxes.setLayout(new BoxLayout(checkboxes, BoxLayout.Y_AXIS));
        
        // --- checkbox 1: doubles are placed first ---
        JCheckBox cb = new JCheckBox("Doubles are placed first (faster)");
        cb.setSelected(MazeGen.doublesArePlacedFirst);
        JPanel panel = getPanelWithToolTipAndCheckBox(cb, "Place doubles one after another right after start node");
        cb.addItemListener(e -> {
            MazeGen.doublesArePlacedFirst = e.getStateChange() == ItemEvent.SELECTED;
        });
        checkboxes.add(Box.createVerticalStrut(5));
        checkboxes.add(panel);

        // --- checkbox 2: change types during backtrack
        cb = new JCheckBox("Change types during backtrack");
        cb.setSelected(MazeGen.tryChangeNodeType);
        panel = getPanelWithToolTipAndCheckBox(cb,
        "Change type from double to ground or ground to double during generation, instead of trying another path");
        cb.addItemListener(e -> {
            MazeGen.tryChangeNodeType = e.getStateChange() == ItemEvent.SELECTED;
        });
        checkboxes.add(Box.createVerticalStrut(5));
        checkboxes.add(panel);

        c.gridy = 2;
        subPanel.add(checkboxes, c);

        return subPanel;
    }
    
    private static void updateNodeTypesSliders() {

        for (int i=0; i<amountSliders.length; i++) {
            MazeGen.Amount type = MazeGen.Amount.priorityDefault.get(i);
            amountSliders[i].setMinimum(type.getMin());
            amountSliders[i].setMaximum(type.getMax());
            amountLabels[i].setText(labelPrefixes[i] + type.get());
            if (amountSliders[i].getValue() != type.get()) {
                amountSliders[i].setValue(type.get());
            }
            
            if (MazeGen.Amount.priority.get(2) == MazeGen.Amount.priorityDefault.get(i)) {
                amountSliders[i].setEnabled(false);
            }
            else {
                amountSliders[i].setEnabled(true);
            }
        }
        
        pathLengthLabel.setText("Resulting path length: " + MazeGen.pathLength);
        
    }

    private static JSlider getNewSlider(int min, int max, int currentVal) {

        JSlider slider = new JSlider(JSlider.VERTICAL, min, max, currentVal);

        slider.setMajorTickSpacing(3);
        slider.setMinorTickSpacing(1);
        slider.setSnapToTicks(true);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setPreferredSize(new Dimension(SLIDER_WIDTH, SLIDER_HEIGHT));

        return slider;
    }

    private static JPanel getPanelWithToolTipAndCheckBox(Component cb, String tip) {

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        JLabel toolTip = new JLabel("?");
        toolTip.setPreferredSize(new Dimension(15, 15));
        
        if (tip == null) {
            // take up invisible space to horizontally align
            toolTip.setForeground(new Color(0, 0, 0, 0));
        }
        else {
            toolTip.setOpaque(true);
            toolTip.setBackground(Color.YELLOW);
            toolTip.setToolTipText(tip);
            toolTip.setHorizontalAlignment(SwingConstants.CENTER);
        }
        
        panel.add(toolTip);
        panel.add(cb);

        return panel;
    }

    private static JButton getConfigPopupButton(String title, JPanel contentPane) {
        
        // setup button that opens a dialog
        JButton btn = new JButton(title);
        btn.setSize(btn.getPreferredSize());

        // open popup with `contentPane`
        btn.addActionListener(e -> {
            JDialog dialog = new JDialog(Window.frame, title, true);
            dialog.setResizable(false);
            dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            dialog.setContentPane(contentPane);
            dialog.setLocationRelativeTo(Window.frame);
            dialog.pack();
            dialog.setVisible(true);

            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    Window.frame.requestFocus();  // prevents focus back to the config btn and rather the game canvas
                }
            });
        });

        return btn;
    }

}
