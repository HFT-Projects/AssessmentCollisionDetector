import gui.MainGUI;

class Main {
    public static void main(String[] args) {
        if (ModuleLayer.boot().modules().stream().map(Module::getName).noneMatch(s -> s.contains("javafx")))
            throw new RuntimeException("javafx sdk not loaded properly. See README file.");
        MainGUI.run();
    }
}