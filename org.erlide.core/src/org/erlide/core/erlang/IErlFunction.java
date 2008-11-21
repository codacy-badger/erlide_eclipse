/*******************************************************************************
 * Copyright (c) 2004 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.core.erlang;

import java.util.List;

import org.erlide.core.util.ErlangFunction;

/**
 * 
 * 
 * @author Vlad Dumitrescu
 */
public interface IErlFunction extends IErlMember, IParent {

	int getArity();

	boolean isExported();

	List<IErlFunctionClause> getClauses();

	ErlangFunction getFunction();

	String getNameWithArity();

	/**
	 * @return the function name with _ for each parameter, used for completion
	 */
	String getNameWithParameters();
}
