import gui.MainGUI;

class Main {
    public static void main(String[] args) {
        // ATTENTION: the program would also START WITHOUT the javafx sdk included but this would lead to undefined behavior and bugs in the gui.
        // therefore this chack assures that the javafx sdk was imported properly.
        if (ModuleLayer.boot().modules().stream().map(Module::getName).noneMatch(s -> s.contains("javafx")))
            throw new RuntimeException("javafx sdk not loaded properly. See README file.");
        MainGUI.run();
    }
}