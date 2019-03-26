package tanvd.grazi;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.*;
import org.jetbrains.annotations.*;
import tanvd.grazi.model.*;

import java.io.*;
import java.util.*;

@State(
        name = "GraziToolProjectSettings",
        storages = @Storage("grazi.xml")
)
public class GraziToolProjectSettings implements PersistentStateComponent<GraziToolProjectSettings.State> {
    private State myState = new State();
    private Set<String> loadingLanguages = new HashSet<>();

    public static GraziToolProjectSettings getInstance(@NotNull final Project project) {
        return ServiceManager.getService(project, GraziToolProjectSettings.class);
    }

    @Nullable
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public String getGraziHome() {
        return myState.graziHome;
    }

    public boolean getUaEnabled() {
        return myState.languages.contains("uk");
    }

    public void setUaEnabled(boolean uaEnabled) {
        if (uaEnabled) {
            myState.languages.add("uk");
        } else {
            myState.languages.remove("uk");
        }
    }

    public void loadLanguages() {
        final ArrayList<String> list = new ArrayList<>();
        if (!myState.languages.contains("uk")) {
            list.add("uk");
        }
        GrammarEngine.INSTANCE.setNoopLangs(list);
    }

    public void setGraziHome(String graziHome) {
        myState.graziHome = graziHome;
    }

    public static class State {
        public String graziHome = getDefaultGraziHome();
        public final Set<String> languages = new HashSet<>();
    }

    public static String getDefaultGraziHome() {
        File directory = new File(System.getProperty("user.home"), ".grazi");
        if (!directory.exists()) {
            directory.mkdir();
        }

        return directory.getAbsolutePath();
    }

}
