package skadistats.clarity.processor.entities;

import skadistats.clarity.model.DTClass;
import skadistats.clarity.util.Predicate;

public class ClassMatcher implements Predicate<DTClass> {
    public boolean apply(DTClass dtClass) {
        return true;
    }
}
