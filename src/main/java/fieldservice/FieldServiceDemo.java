package fieldservice;

public final class FieldServiceDemo {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: FieldServiceDemo <flag-key> <technician-id>");
        String flagKey = args[0];
        String technicianId = args[1];
        InfraiClient client = new InfraiClient(System.getenv("INFRAI_API_KEY"));
        client.getUser(technicianId);
        String assignment = client.getFlagValue(flagKey, technicianId);
        String variant = assignment.contains("follow-up") ? "follow-up" : "control";
        String result = new FieldServiceExperiment().observation("WO-1042", "dispatched", 3, true, variant);
        System.out.println(result);
    }
}
