package me.mrcsparker.edireader;

import com.ericsson.otp.erlang.OtpNode;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Properties;

@Slf4j
@RequiredArgsConstructor
class AppModule extends AbstractModule {

    /** Configuration properties for the application. */
    private final Properties properties;

    /**
     * Configures a {@link Binder} via the exposed methods.
     */
    @Override
    protected void configure() {
        Names.bindProperties(binder(), properties);
    }

    @Provides
    @Singleton
    private OtpNode providesOtpNode(
            @Named("erlang.self")   final String self,
            @Named("erlang.cookie") final String cookie) {
        try {
            OtpNode otpNode = new OtpNode(self, cookie);
            //OtpNode otpNode = new OtpNode(self);

            log.info("OtpNode created.");
            log.info("OtpNode node: {}", self);
            log.info("OtpNode cookie: {}", cookie);

            return otpNode;
        } catch (final IOException e) {
            log.error("Could not start OTP node.", e);
            log.error("'epmd' must be running, try 'epmd -daemon'.");
            System.exit(-1);
            return null;
        }
    }
}
