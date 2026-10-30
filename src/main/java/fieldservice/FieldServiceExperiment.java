package fieldservice;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class FieldServiceExperiment {
    public String stableVariant(String experimentKey, String technicianId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((experimentKey + ":" + technicianId).getBytes(StandardCharsets.UTF_8));
            return (digest[0] & 1) == 0 ? "control" : "follow-up";
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public String observation(String workOrderId, String dispatchStatus, int photoCount,
                              boolean technicianFollowedUp, String variant) {
        return "workOrder=" + workOrderId + ", dispatchStatus=" + dispatchStatus
                + ", photoCount=" + photoCount + ", technicianFollowedUp="
                + technicianFollowedUp + ", variant=" + variant;
    }
}
