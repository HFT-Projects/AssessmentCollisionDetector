import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class FileGUI extends JDialog {

    private JTextField PATH_INPUT_ASSESSMENTS = new JTextField();
    private JTextField PATH_INPUT_REGISTRATIONS = new JTextField();
    private JTextField PATH_OUTPUT_COLLISIONS= new JTextField();





    public FileGUI(){
        setTitle("Dateien auswählen");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 800, 600);
        setLayout(new GridBagLayout());
        GridBagLayout gbl_Dialog = new GridBagLayout();

    }

}
