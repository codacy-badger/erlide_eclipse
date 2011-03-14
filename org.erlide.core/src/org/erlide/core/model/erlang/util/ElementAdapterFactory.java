package org.erlide.core.model.erlang.util;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.erlide.core.ErlangCore;
import org.erlide.core.model.erlang.IErlElement;

public class ElementAdapterFactory implements IAdapterFactory {

    @SuppressWarnings("rawtypes")
    private static final Class[] ADAPTER_LIST = new Class[] { IErlElement.class };

    @SuppressWarnings("rawtypes")
    public Object getAdapter(final Object adaptableObject,
            final Class adapterType) {
        if (adapterType == IErlElement.class
                && adaptableObject instanceof IResource) {
            return ErlangCore.getModel().findElement(
                    (IResource) adaptableObject);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public Class[] getAdapterList() {
        return ADAPTER_LIST;
    }

}
