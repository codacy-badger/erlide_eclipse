package org.erlide.test_support;

import org.erlide.runtime.events.ErlEvent;
import org.erlide.runtime.events.ErlangEventHandler;
import org.erlide.test_support.ui.suites.TestResultsView;

import com.ericsson.otp.erlang.OtpErlangObject;
import com.google.common.eventbus.Subscribe;

public class EunitEventHandler extends ErlangEventHandler {

    private final TestResultsView view;

    public EunitEventHandler(final TestResultsView view) {
        super("eunit");
        this.view = view;
    }

    @Subscribe
    public void handleEvent(final ErlEvent event) {
        if (!event.getTopic().equals(getTopic())) {
            return;
        }
        final OtpErlangObject data = event.getEvent();
        if (view != null) {
            view.notifyEvent(data);
        }
    }
}
