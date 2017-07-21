package org.erlide.engine.model.root;

import java.util.Collection;

import org.erlide.engine.model.ErlModelException;
import org.erlide.engine.model.IErlElement;
import org.erlide.engine.model.IParent;

public interface IErlApplication extends IParent, IErlElement, IOpenable {

    Collection<IErlApplication> getDependencies() throws ErlModelException;

}
