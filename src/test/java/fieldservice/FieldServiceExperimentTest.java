package fieldservice;

public final class FieldServiceExperimentTest {
    public static void main(String[] args) {
        FieldServiceExperiment experiment = new FieldServiceExperiment();
        String first = experiment.stableVariant("field-service-photo-followup", "tech-17");
        String second = experiment.stableVariant("field-service-photo-followup", "tech-17");
        if (!first.equals(second)) throw new AssertionError("technician moved between variants");
        if (!(first.equals("control") || first.equals("follow-up"))) throw new AssertionError("unknown variant");
        System.out.println("PASS: tech-17 stays in " + first);
    }
}
