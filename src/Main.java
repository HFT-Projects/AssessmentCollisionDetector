class Main {
    private final static String PATH_INPUT_ASSESSMENTS = "resources/pruefungen.csv";
    private final static String PATH_INPUT_REGISTRATIONS = "resources/anmeldungen.csv";
    public final static String PATH_OUTPUT_COLLISIONS = "target/collisions.csv";

    public static void main(String[] args) {
        Assessment[] assessments = AssessmentsManager.loadAllAssessments(PATH_INPUT_ASSESSMENTS, PATH_INPUT_REGISTRATIONS);
        AssessmentsManager.loadRegistrationsIntoAssessments(assessments, PATH_INPUT_REGISTRATIONS);
        AssessmentsManager.loadCollisionsIntoAssessments(assessments);
        SaveManager.saveCollision(PATH_OUTPUT_COLLISIONS, assessments);

        System.out.println("completed");
    }
}