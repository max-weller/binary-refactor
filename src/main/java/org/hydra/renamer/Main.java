package org.hydra.renamer;

import org.hydra.util.Log;

public class Main {
    public static void main(String[] args) {

        if (args == null || args.length < 2) {
            Log.error("Usage:%s configFile jarFile", Main.class.getName());
            System.exit(1);
        }

        String config = args[0];
        String jarFile = args[1];

        try {
            Renamer.rename(config, jarFile);
        } catch(Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        Log.debug("rename classes/methods/fields in %s based %s", jarFile, config);
    }
}
