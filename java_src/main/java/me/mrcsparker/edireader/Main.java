package me.mrcsparker.edireader;

import com.google.inject.Guice;
import com.google.inject.Injector;

import java.util.Properties;

public final class Main {
    public static void main(String args[]) {
        final Properties properties = new Properties();

        // load startup arguments into properties configuration
        properties.put("erlang.node",   (args.length >= 1) ? args[0] : "nonode@nohost");
        properties.put("erlang.self",   (args.length >= 2) ? args[1] : "__edireader__nonode@nohost");
        properties.put("erlang.cookie", (args.length >= 3) ? args[2] : "nocookie");
        properties.put("erlang.registered_proc_name", (args.length >= 4) ? args[3] : "edireader_java_server");

        final Injector injector = Guice.createInjector(new AppModule(properties));
        injector.getInstance(JavaServer.class).startAsync().awaitRunning();

        System.out.println("READY");
    }
}