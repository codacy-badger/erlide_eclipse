package org.erlide.engine.internal;

import org.erlide.engine.services.SystemInfoService;
import org.erlide.runtime.rpc.IOtpRpc;
import org.erlide.util.erlang.OtpErlang;

import com.ericsson.otp.erlang.OtpErlangObject;

public class SystemInfo implements SystemInfoService {

    private final IOtpRpc ideBackend;

    public SystemInfo(final IOtpRpc backend) {
        ideBackend = backend;
    }

    @Override
    public String get() {
        try {
            final OtpErlangObject val = ideBackend.call("erlide_backend",
                    "get_system_info", "");
            return OtpErlang.asString(val);
        } catch (final Exception e) {
            return "System information could not be retrieved "
                    + "(node not monitored)... ";
        }
    }

}
