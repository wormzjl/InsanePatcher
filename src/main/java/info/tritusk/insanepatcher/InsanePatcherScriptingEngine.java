package info.tritusk.insanepatcher;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;

public final class InsanePatcherScriptingEngine {

    static final Logger LOG = LogManager.getLogger("InsanePatcherScriptingEngine");

    static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("insanepatcher.debug", "false"));

    private static final ScriptEngine ENGINE = new ScriptEngineManager(null).getEngineByName("nashorn");

    private static final Map<String, Reader> TRANSFORMERS = new HashMap<>();

    private static boolean firstTime = false;

    private InsanePatcherScriptingEngine() {}

    static void setupInsaneScripting(File mcLocation) {
        final File scripts = new File(mcLocation, "insane_patchers");
        if ((!scripts.exists() || !scripts.isDirectory()) && scripts.mkdir()) {
            firstTime = true;
            return;
        }

        Properties cfg = new Properties();
        try (InputStream stream = FileUtils.openInputStream(new File(scripts, "target.properties"))) {
            cfg.load(stream);
            cfg.stringPropertyNames().forEach(className -> {
                try {
                    TRANSFORMERS.putIfAbsent(className, Files.newReader(new File(scripts, cfg.getProperty(className)), Charsets.UTF_8));
                } catch (FileNotFoundException ignored) {}
            });
        } catch (IOException failOnLoad) {
            LOG.error("Failed on setting up config, please manually setup your config file.");
        }
    }

    static void process(String transformedName, ClassNode node) {
        try (Reader reader = TRANSFORMERS.remove(transformedName)){
            ENGINE.setBindings(ENGINE.createBindings(), ScriptContext.ENGINE_SCOPE);
            ENGINE.put("node", node);
            ENGINE.eval(reader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static boolean shouldTransform(String transformedName) {
        return !firstTime && TRANSFORMERS.containsKey(transformedName);
    }
}
