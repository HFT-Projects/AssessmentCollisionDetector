import java.awt.EventQueue;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.*;

//MAde with WindowBuilder (Eclipse)
public class ExamGUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private final ButtonGroup SortGroup = new ButtonGroup();
    private JTextField SearchField;
    private static JRadioButton StandardRadio;
    private static JRadioButton ExamNumberRadio;
    private static JRadioButton ExamNameRadio;
    private static Assessment [] Assessments_Current;
    private static Assessment [] Assessment_BackUP;
    private static Assessment [] search_Assessments ;
    public static DefaultTableModel tableList;
    private JTable  table;
    private JComboBox comboBox;




        /**
         * Launch the application.
         */
        public static void main(String[] args) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        ExamGUI frame = new ExamGUI();
                        frame.setVisible(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        /**
         * Create the frame.
         */
        public ExamGUI() {
            setTitle("Prüfungsübersicht");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setBounds(100, 100, 1300, 900);
            contentPane = new JPanel();
            contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

            setContentPane(contentPane);
            GridBagLayout gbl_contentPane = new GridBagLayout();
            gbl_contentPane.columnWidths = new int[]{0, 0, 0, 0, 0, 0, 0};
            gbl_contentPane.rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0};
            gbl_contentPane.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0};
            gbl_contentPane.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
            contentPane.setLayout(gbl_contentPane);


            JLabel EinlesenLabel = new JLabel("Die CSV-Datei einlesen:");
            EinlesenLabel.setHorizontalAlignment(SwingConstants.LEFT);
            EinlesenLabel.setFont(new Font("Times New Roman", Font.BOLD, 15));
            GridBagConstraints gbc_EinlesenLabel = new GridBagConstraints();
            gbc_EinlesenLabel.fill = GridBagConstraints.BOTH;
            gbc_EinlesenLabel.insets = new Insets(0, 0, 5, 5);
            gbc_EinlesenLabel.gridwidth = 2;
            gbc_EinlesenLabel.gridx = 0;
            gbc_EinlesenLabel.gridy = 0;
            contentPane.add(EinlesenLabel, gbc_EinlesenLabel);

            JButton ReadButton = new JButton("Einlesen");
            ReadButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        Assessments_Current = Main.runProcessing();
                        Assessment_BackUP = Assessments_Current;
                        search_Assessments = Assessments_Current;
                        table_Assessments(Assessments_Current, true);
                        fill_DropDown();
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(null, "Das Programm konnte nicht ausgeführt werden");
                    }
                }
            });
            ReadButton.setFont(new Font("Times New Roman", Font.BOLD, 12));
            GridBagConstraints gbc_ReadButton = new GridBagConstraints();
            gbc_ReadButton.insets = new Insets(0, 0, 5, 5);
            gbc_ReadButton.gridx = 2;
            gbc_ReadButton.gridy = 0;
            contentPane.add(ReadButton, gbc_ReadButton);

            SearchField = new JTextField();
            SearchField.setFont(new Font("Times New Roman", Font.PLAIN, 12));
            GridBagConstraints gbc_searchField = new GridBagConstraints();
            gbc_searchField.gridwidth = 3;
            gbc_searchField.insets = new Insets(0, 0, 5, 5);
            gbc_searchField.fill = GridBagConstraints.HORIZONTAL;
            gbc_searchField.gridx = 3;
            gbc_searchField.gridy = 0;
            contentPane.add(SearchField, gbc_searchField);
            SearchField.setColumns(10);

            JButton SearchButton = new JButton("Suchen");
            SearchButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    comboBox.setSelectedIndex(0);
                    String searchText = SearchField.getText();
                    if(isNumber(searchText)){
                        searchAssessment(null, Long.valueOf(searchText));
                    } else {
                        searchAssessment(searchText, null);
                    }

                }
            });
            SearchButton.setFont(new Font("Times New Roman", Font.BOLD, 12));
            GridBagConstraints gbc_SearchButton = new GridBagConstraints();
            gbc_SearchButton.insets = new Insets(0, 0, 5, 0);
            gbc_SearchButton.fill = GridBagConstraints.BOTH;
            gbc_SearchButton.gridx = 6;
            gbc_SearchButton.gridy = 0;
            contentPane.add(SearchButton, gbc_SearchButton);

            JLabel DropDownLabel = new JLabel("Auswählen:");
            DropDownLabel.setHorizontalAlignment(SwingConstants.LEFT);
            DropDownLabel.setFont(new Font("Times New Roman", Font.BOLD, 15));
            GridBagConstraints gbc_DropDownLabel = new GridBagConstraints();
            gbc_DropDownLabel.fill = GridBagConstraints.BOTH;
            gbc_DropDownLabel.gridwidth = 2;
            gbc_DropDownLabel.insets = new Insets(0, 0, 5, 5);
            gbc_DropDownLabel.gridx = 0;
            gbc_DropDownLabel.gridy = 1;
            contentPane.add(DropDownLabel, gbc_DropDownLabel);

            comboBox = new JComboBox();
            comboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(!comboBox.getSelectedItem().equals("")){
                        String selected = (String) comboBox.getSelectedItem();
                        String [] comboBox_Text = selected.split(" ");
                        /*
                        Long NumberComboBox = comboBox_Text[0] == null ? 0 :  Long.valueOf(comboBox_Text[0]);
                        searchAssessment(comboBox_Text[1],NumberComboBox);
                        */
                        searchAssessment(comboBox_Text[1],null);
                    } else{
                        searchAssessment("", null);
                    }
                }
            });
            GridBagConstraints gbc_comboBox = new GridBagConstraints();
            gbc_comboBox.gridwidth = 3;
            gbc_comboBox.insets = new Insets(0, 0, 5, 5);
            gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
            gbc_comboBox.gridx = 3;
            gbc_comboBox.gridy = 1;
            contentPane.add(comboBox, gbc_comboBox);

            JLabel SortLabel = new JLabel("Sortieren nach:");
            SortLabel.setHorizontalAlignment(SwingConstants.LEFT);
            SortLabel.setFont(new Font("Times New Roman", Font.BOLD, 15));
            GridBagConstraints gbc_SortLabel = new GridBagConstraints();
            gbc_SortLabel.anchor = GridBagConstraints.WEST;
            gbc_SortLabel.gridwidth = 2;
            gbc_SortLabel.insets = new Insets(0, 0, 5, 5);
            gbc_SortLabel.gridx = 0;
            gbc_SortLabel.gridy = 2;
            contentPane.add(SortLabel, gbc_SortLabel);

            StandardRadio = new JRadioButton("Standard");
            StandardRadio.addActionListener(e -> SortRadio());

            StandardRadio.setSelected(true);
            StandardRadio.setFont(new Font("Times New Roman", Font.BOLD, 11));
            SortGroup.add(StandardRadio);
            StandardRadio.setHorizontalAlignment(SwingConstants.LEFT);
            GridBagConstraints gbc_StandardRadio = new GridBagConstraints();
            gbc_StandardRadio.anchor = GridBagConstraints.WEST;
            gbc_StandardRadio.insets = new Insets(0, 0, 5, 5);
            gbc_StandardRadio.gridx = 2;
            gbc_StandardRadio.gridy = 2;
            contentPane.add(StandardRadio, gbc_StandardRadio);

            JButton ShuffleButton = new JButton("Shuffle");
            ShuffleButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                }
            });
            ShuffleButton.setFont(new Font("Times New Roman", Font.BOLD, 12));
            GridBagConstraints gbc_ShuffleButton = new GridBagConstraints();
            gbc_ShuffleButton.insets = new Insets(0, 0, 5, 0);
            gbc_ShuffleButton.fill = GridBagConstraints.BOTH;
            gbc_ShuffleButton.gridx = 6;
            gbc_ShuffleButton.gridy = 2;
            contentPane.add(ShuffleButton, gbc_ShuffleButton);

            ExamNumberRadio = new JRadioButton("Prüf.Nr");
            ExamNumberRadio.addActionListener(e -> SortRadio());

            ExamNumberRadio.setFont(new Font("Times New Roman", Font.BOLD, 11));
            SortGroup.add(ExamNumberRadio);
            ExamNumberRadio.setHorizontalAlignment(SwingConstants.LEFT);
            GridBagConstraints gbc_ExamNumberRadio = new GridBagConstraints();
            gbc_ExamNumberRadio.anchor = GridBagConstraints.WEST;
            gbc_ExamNumberRadio.insets = new Insets(0, 0, 5, 5);
            gbc_ExamNumberRadio.gridx = 2;
            gbc_ExamNumberRadio.gridy = 3;
            contentPane.add(ExamNumberRadio, gbc_ExamNumberRadio);

            ExamNameRadio = new JRadioButton("Name");
            ExamNameRadio.addActionListener(e -> SortRadio());

            ExamNameRadio.setFont(new Font("Times New Roman", Font.BOLD, 11));
            SortGroup.add(ExamNameRadio);
            ExamNameRadio.setHorizontalAlignment(SwingConstants.LEFT);
            GridBagConstraints gbc_ExamNameRadio = new GridBagConstraints();
            gbc_ExamNameRadio.anchor = GridBagConstraints.WEST;
            gbc_ExamNameRadio.insets = new Insets(0, 0, 5, 5);
            gbc_ExamNameRadio.gridx = 2;
            gbc_ExamNameRadio.gridy = 4;
            contentPane.add(ExamNameRadio, gbc_ExamNameRadio);

            //Creating a table with necessary columnnames
            String[] columnNames = {"Fach 1", "Lfd. Nr.", "Fach 1", "Fach 2", "Datum / Uhrzeit", "Kollision", "Zeitabstand"};
            tableList = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            table = new JTable(tableList);
            table.setFont(new Font("Times New Roman", Font.PLAIN, 11));
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            table.getTableHeader().setReorderingAllowed(false);// Blocks drag and drop of TableHeader
            table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS); // oder AUTO_RESIZE_ALL_COLUMNS




            JScrollPane scrollPane = new JScrollPane(table);
            GridBagConstraints gbc_scrollPane = new GridBagConstraints();
            gbc_scrollPane.gridwidth = 7;
            gbc_scrollPane.insets = new Insets(0, 0, 0, 5);
            gbc_scrollPane.fill = GridBagConstraints.BOTH;
            gbc_scrollPane.gridx = 0;
            gbc_scrollPane.gridy = 5;
            gbc_scrollPane.weightx = 1.0;
            gbc_scrollPane.weighty = 1.0;
            contentPane.add(scrollPane, gbc_scrollPane);
        }

        //To sort the list
        public static void SortRadio(){
            Assessments_Current = search_Assessments;
            if(Assessments_Current != null) {
                if(StandardRadio.isSelected()){
                    table_Assessments(Assessments_Current, true);

                } else if (ExamNameRadio.isSelected()) {
                    Arrays.sort(Assessments_Current, (x,y) -> x.getName().compareTo(y.getName()));
                    table_Assessments(Assessments_Current, false);

                } else {
                    Arrays.sort(Assessments_Current, (x, y) -> {
                        Long xNum = x.getNumber();
                        Long yNum = y.getNumber();
                        if (xNum == null && yNum == null) return 0;
                        if (xNum == null) return 1; // oder -1, je nachdem ob nulls oben oder unten stehen sollen
                        if (yNum == null) return -1;
                        return Long.compare(xNum, yNum);
                    });
                    table_Assessments(Assessments_Current, false);

                }

            }

        }

        public static void table_Assessments(Assessment [] a, boolean standardSort){
            tableList.setRowCount(0);

            List<String> lines = SaveManager.formatCollision(a, false, standardSort);

            // Paste die lines into tableList
            for (String line : lines) {
                String[] values = line.split(";", -1); // for empty lines
                tableList.addRow(values);
            }
            System.out.println("Rows: " + tableList.getRowCount());

        }

        public static void searchAssessment(String SText, Long SNumber) {

            Assessments_Current = Assessment_BackUP;
            String searchText = SText.trim();
            Long searchNumber = SNumber;

            if (searchText != null && searchNumber == null) {
                int quantity = 0;
                for (Assessment p : Assessments_Current) {
                    if (p.getName().toLowerCase().contains(searchText.toLowerCase())) {
                        quantity++;
                    }
                }
                search_Assessments = new Assessment[quantity];
                quantity = 0;
                for (Assessment p : Assessments_Current) {
                    if (p.getName().toLowerCase().contains(searchText.toLowerCase())) {
                        search_Assessments[quantity++] = p;
                    }
                }
            } else if (searchText == null && searchNumber != null) {
                int quantity = 0;
                for (Assessment p : Assessments_Current) {
                    if (Objects.equals(p.getNumber(),searchNumber )) {
                        quantity++;
                    }
                }
                search_Assessments = new Assessment[quantity];
                quantity = 0;
                for (Assessment p : Assessments_Current) {
                    if (Objects.equals(p.getNumber(),searchNumber )) {
                        search_Assessments[quantity++] = p;
                    }
                }
            } else {
                int quantity = 0;
                for (Assessment p : Assessments_Current) {
                    if (p.getName().toLowerCase().contains(searchText.toLowerCase())) {
                        if (Objects.equals(p.getNumber(),searchNumber )) {
                            quantity++;
                        }

                    }
                }
                search_Assessments = new Assessment[quantity];
                quantity = 0;
                for (Assessment p : Assessments_Current) {
                    if (p.getName().toLowerCase().contains(searchText.toLowerCase())) {
                        if (Objects.equals(p.getNumber(),searchNumber )) {
                            search_Assessments[quantity++] = p;
                        }

                    }

                }

            }
            SortRadio();
        }

    public void fill_DropDown(){
            Assessment [] sortedAssesments = Assessments_Current;
            comboBox.addItem("");
            Arrays.sort(sortedAssesments,(x,y) -> x.getName().compareTo(y.getName()) );
            for(Assessment p: sortedAssesments){
                String entry = p.getNumber() + " " + p.getName();
                comboBox.addItem(entry);

            }
    }

    public static boolean isNumber(String input) {
        if (input == null || input.isEmpty()) return false;
        try {
            Long.parseLong(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }



    }


