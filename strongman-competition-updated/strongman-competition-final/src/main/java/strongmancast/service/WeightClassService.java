package strongmancast.service;

import org.springframework.stereotype.Service;
import strongmancast.model.Athlete;

@Service
public class WeightClassService {

    public String resolveDivision(Athlete athlete) {
        if (athlete.getDivision() != null && !athlete.getDivision().isBlank()) {
            return athlete.getDivision();
        }

        String gender = athlete.getGender();
        if (gender == null || gender.isBlank()) {
            return "Unassigned";
        }

        if ("Adaptive".equalsIgnoreCase(gender)) {
            return "Adaptive";
        }

        double bodyweight = athlete.getBodyweight();
        if ("Women".equalsIgnoreCase(gender)) {
            if (bodyweight <= 64.0) {
                return "Women Lightweight (LW)";
            }
            if (bodyweight <= 73.0) {
                return "Women Middleweight (MW)";
            }
            if (bodyweight <= 82.0) {
                return "Women Heavyweight (HW)";
            }
            return "Women Super Heavyweight (SHW)";
        }

        if ("Men".equalsIgnoreCase(gender)) {
            if (bodyweight <= 80.0) {
                return "Men Lightweight (LW)";
            }
            if (bodyweight <= 90.0) {
                return "Men Middleweight (MW)";
            }
            if (bodyweight <= 105.0) {
                return "Men Heavyweight (HW)";
            }
            return "Men Super Heavyweight (SHW)";
        }

        return "Unassigned";
    }
}
