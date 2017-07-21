package org.erlide.engine.model.root;

public class OTPProjectConfigurator extends ProjectConfigurator {

    public OTPProjectConfigurator() {
        super("otp", null);
    }

    @Override
    public ErlangProjectProperties getConfiguration() {
        return ErlangProjectProperties.DEFAULT;
    }

    @Override
    public void setConfiguration(final ErlangProjectProperties info) {
    }

}
