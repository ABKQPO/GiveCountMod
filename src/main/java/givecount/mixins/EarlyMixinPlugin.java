package givecount.mixins;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EarlyMixinPlugin {

    public static List<String> getEarlyMixins(Set<String> loadedMods) {
        final List<String> mixins = new ArrayList<>();
        mixins.add("NEIServerUtils_Mixin");
        return mixins;
    }
}
