/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the Eclipse Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.ui.editors.erl.outline;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.erlide.engine.model.IErlElement;
import org.erlide.engine.model.root.IErlExternal;
import org.erlide.engine.model.root.IErlExternalRoot;
import org.erlide.engine.model.root.IErlModule;
import org.erlide.util.StringUtils;

public class ErlangLabelProvider implements ILabelProvider, IColorProvider {

    private static final int LABEL_LENGTH_LIMIT = 200;

    protected ListenerList<ILabelProviderListener> fListeners = new ListenerList<>(1);

    protected ErlangElementImageProvider fImageLabelProvider;

    private List<ILabelDecorator> fLabelDecorators;

    private int fImageFlags;

    private long fTextFlags;

    /**
     * Creates a new label provider with default flags.
     */
    public ErlangLabelProvider() {
        this(0, ErlangElementImageProvider.OVERLAY_ICONS);
    }

    /**
     * @param textFlags
     *            Flags defined in <code>ErlangElementLabels</code>.
     * @param imageFlags
     *            Flags defined in <code>ErlangElementImageProvider</code>.
     */
    public ErlangLabelProvider(final long textFlags, final int imageFlags) {
        fImageLabelProvider = new ErlangElementImageProvider();
        fLabelDecorators = null;

        fImageFlags = imageFlags;
        fTextFlags = textFlags;
    }

    /**
     * Adds a decorator to the label provider
     */
    public void addLabelDecorator(final ILabelDecorator decorator) {
        if (fLabelDecorators == null) {
            fLabelDecorators = new ArrayList<>(2);
        }
        fLabelDecorators.add(decorator);
    }

    /**
     * Sets the textFlags.
     *
     * @param textFlags
     *            The textFlags to set
     */
    public final void setTextFlags(final long textFlags) {
        fTextFlags = textFlags;
    }

    /**
     * Sets the imageFlags
     *
     * @param imageFlags
     *            The imageFlags to set
     */
    public final void setImageFlags(final int imageFlags) {
        fImageFlags = imageFlags;
    }

    /**
     * Gets the image flags. Can be overwriten by super classes.
     *
     * @return Returns a int
     */
    public final int getImageFlags() {
        return fImageFlags;
    }

    /**
     * Gets the text flags.
     *
     * @return Returns a int
     */
    public final long getTextFlags() {
        return fTextFlags;
    }

    /**
     * Evaluates the image flags for a element. Can be overwriten by super classes.
     *
     * @return Returns a int
     */
    protected int evaluateImageFlags(final Object element) {
        return getImageFlags();
    }

    /**
     * Evaluates the text flags for a element. Can be overwriten by super classes.
     *
     * @return Returns a int
     */
    protected long evaluateTextFlags(final Object element) {
        return getTextFlags();
    }

    protected Image decorateImage(final Image image0, final Object element) {
        Image image = image0;
        if (fLabelDecorators != null && image != null) {
            for (final ILabelDecorator decorator : fLabelDecorators) {
                image = decorator.decorateImage(image, element);
            }
        }
        return image;
    }

    @Override
    public Image getImage(final Object element) {
        final Image result = fImageLabelProvider.getImageLabel(element,
                evaluateImageFlags(element));

        return decorateImage(result, element);
    }

    protected String decorateText(final String text0, final Object element) {
        String text = text0;
        if (fLabelDecorators != null && !text.isEmpty()) {
            for (final ILabelDecorator decorator : fLabelDecorators) {
                text = decorator.decorateText(text, element);
            }
        }
        return text;
    }

    @Override
    public String getText(final Object element) {
        final String label = ErlangLabelProvider.getLabelString(element);
        return decorateText(label, element);
    }

    public static String getLabelString(final Object element) {
        String label;
        if (element instanceof IErlModule || element instanceof IErlExternalRoot
                || element instanceof IErlExternal) {
            label = ((IErlElement) element).getName();
        } else if (element instanceof IErlElement) {
            label = StringUtils.normalizeSpaces(element.toString());
        } else {
            label = element.toString();
        }
        if (label.length() > ErlangLabelProvider.LABEL_LENGTH_LIMIT) {
            int i = label.indexOf(',', ErlangLabelProvider.LABEL_LENGTH_LIMIT);
            if (i == -1) {
                i = ErlangLabelProvider.LABEL_LENGTH_LIMIT;
            }
            label = label.substring(0, i) + "...";
        }
        return label;
    }

    @Override
    public void dispose() {
        if (fLabelDecorators != null) {
            for (final ILabelDecorator decorator : fLabelDecorators) {
                decorator.dispose();
            }
            fLabelDecorators = null;
        }
        fImageLabelProvider.dispose();
    }

    @Override
    public void addListener(final ILabelProviderListener listener) {
        if (fLabelDecorators != null) {
            for (final ILabelDecorator decorator : fLabelDecorators) {
                decorator.addListener(listener);
            }
        }
        fListeners.add(listener);
    }

    @Override
    public boolean isLabelProperty(final Object element, final String property) {
        return true;
    }

    @Override
    public void removeListener(final ILabelProviderListener listener) {
        if (fLabelDecorators != null) {
            for (final ILabelDecorator decorator : fLabelDecorators) {
                decorator.removeListener(listener);
            }
        }
        fListeners.remove(listener);
    }

    public static ILabelDecorator[] getDecorators(final boolean errortick,
            final ILabelDecorator extra) {
        if (errortick) {
            if (extra == null) {
                return new ILabelDecorator[] {};
            }
            return new ILabelDecorator[] {
                    extra
            };
        }
        if (extra != null) {
            return new ILabelDecorator[] {
                    extra
            };
        }
        return null;
    }

    @Override
    public Color getForeground(final Object element) {
        return null;
    }

    @Override
    public Color getBackground(final Object element) {
        return null;
    }

    /**
     * Fires a label provider changed event to all registered listeners Only listeners
     * registered at the time this method is called are notified.
     *
     * @param event
     *            a label provider changed event
     *
     * @see ILabelProviderListener#labelProviderChanged
     */
    protected void fireLabelProviderChanged(final LabelProviderChangedEvent event) {
        final Object[] listeners = fListeners.getListeners();
        for (final Object element : listeners) {
            final ILabelProviderListener l = (ILabelProviderListener) element;
            SafeRunner.run(new SafeRunnable() {

                @Override
                public void run() {
                    l.labelProviderChanged(event);
                }
            });
        }
    }

}
