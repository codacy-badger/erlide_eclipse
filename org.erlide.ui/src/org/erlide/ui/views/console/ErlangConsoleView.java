package org.erlide.ui.views.console;

/*******************************************************************************
 * Copyright (c) 2004 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution.
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.part.ViewPart;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.runtime.ErlLogger;
import org.erlide.runtime.backend.Backend;
import org.erlide.runtime.backend.console.BackendShell;
import org.erlide.runtime.backend.console.ErlConsoleModel;
import org.erlide.runtime.backend.console.ErlConsoleModelListener;
import org.erlide.runtime.backend.console.IoRequest;
import org.erlide.runtime.backend.console.ErlConsoleModel.ConsoleEventHandler;
import org.erlide.runtime.backend.exceptions.BackendException;
import org.erlide.runtime.debug.ErlangProcess;

import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;

import erlang.ErlideBackend;

public class ErlangConsoleView extends ViewPart implements
		ErlConsoleModelListener {

	public static final String ID = "org.erlide.ui.views.console";

	private static final Color[] colors = {
			new Color(Display.getDefault(), 0xFF, 0xFF, 0xFF),
			new Color(Display.getDefault(), 0xCC, 0xFF, 0xFF),
			new Color(Display.getDefault(), 0xFF, 0xCC, 0xFF),
			new Color(Display.getDefault(), 0xFF, 0xFF, 0xCC),
			new Color(Display.getDefault(), 0xCC, 0xCC, 0xFF),
			new Color(Display.getDefault(), 0xCC, 0xFF, 0xCC),
			new Color(Display.getDefault(), 0xFF, 0xCC, 0xCC),
			new Color(Display.getDefault(), 0x99, 0xFF, 0xFF),
			new Color(Display.getDefault(), 0xFF, 0x99, 0xFF),
			new Color(Display.getDefault(), 0xFF, 0xFF, 0x99),
			new Color(Display.getDefault(), 0x99, 0xCC, 0xFF),
			new Color(Display.getDefault(), 0xCC, 0x99, 0xFF),
			new Color(Display.getDefault(), 0xFF, 0x99, 0xCC),
			new Color(Display.getDefault(), 0xFF, 0xCC, 0x99),
			new Color(Display.getDefault(), 0x99, 0xFF, 0xCC),
			new Color(Display.getDefault(), 0xCC, 0xFF, 0x99),
			new Color(Display.getDefault(), 0x99, 0x99, 0xFF),
			new Color(Display.getDefault(), 0xFF, 0x99, 0x99),
			new Color(Display.getDefault(), 0x99, 0xFF, 0x99) };

	StyledText consoleText;
	boolean fGroupByLeader;
	boolean fColored;
	final Set<OtpErlangPid> pids = new TreeSet<OtpErlangPid>();
	TableViewer consoleTable;
	ErlConsoleDocument fDoc;
	final List<String> history = new ArrayList<String>(10);
	StyledText consoleInput;
	SourceViewer consoleOutputViewer;
	SourceViewer consoleInputViewer;
	ErlConsoleModel model;
	BackendShell shell;
	Backend backend;

	public ErlangConsoleView() {
		super();
		model = new ErlConsoleModel();
		final ConsoleEventHandler handler = model.getHandler();
		backend = ErlangCore.getBackendManager().getIdeBackend();
		try {
			final Job j = new Job("shell opener") {
				@Override
				protected IStatus run(final IProgressMonitor monitor) {
					if (backend == null) {
						schedule(400);
					} else {
						backend.getEventDaemon().addListener(handler);
						shell = backend.getShellManager().openShell("main");
					}
					return Status.OK_STATUS;
				}
			};
			j.setSystem(true);
			j.setPriority(Job.SHORT);
			j.schedule(400);
		} catch (final Exception e) {
			ErlLogger.warn(e);
		}
		model.addListener(this);
		fDoc = new ErlConsoleDocument(model);
	}

	@Override
	public void dispose() {
		backend.getEventDaemon().removeListener(model.getHandler());
		model.dispose();
		super.dispose();
	}

	@Override
	public void createPartControl(final Composite parent) {
		parent.setLayout(new GridLayout());
		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolbar.setLayout(new RowLayout());

		final ToolItem refreshBtn = new ToolItem(toolbar, SWT.PUSH);
		refreshBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				refreshView();
			}

		});
		refreshBtn.setText("Refresh");
		new ToolItem(toolbar, SWT.SEPARATOR);

		final ToolItem colorCheck = new ToolItem(toolbar, SWT.CHECK);
		colorCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				fColored = !fColored;
				refreshView();
			}
		});
		colorCheck.setText("Colored");

		final ToolItem groupChk = new ToolItem(toolbar, SWT.CHECK);
		groupChk.setText("group");
		groupChk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				fGroupByLeader = !fGroupByLeader;
				groupChk.setText(fGroupByLeader ? "leader" : "sender");
				consoleTable.setInput(fDoc);
				refreshView();
			}
		});
		groupChk.setText(fGroupByLeader ? "leader" : "sender");

		final ToolItem filterItem = new ToolItem(toolbar, SWT.DROP_DOWN);
		filterItem.setText("Filter");

		final Menu filter = new Menu(toolbar);
		addDropDown(filterItem, filter);

		final ToolItem newItemToolItem = new ToolItem(toolbar, SWT.SEPARATOR);
		newItemToolItem.setText("New item");

		final TabFolder tabFolder = new TabFolder(parent, SWT.NONE);
		final GridData gridData = new GridData(SWT.FILL, SWT.FILL, false, true);
		gridData.widthHint = 475;
		gridData.heightHint = 290;
		tabFolder.setLayoutData(gridData);

		final TabItem plainTab = new TabItem(tabFolder, SWT.NONE);
		plainTab.setText("Plain");

		consoleOutputViewer = new SourceViewer(tabFolder, null, SWT.V_SCROLL
				| SWT.H_SCROLL | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER);
		consoleOutputViewer.setDocument(fDoc);
		consoleText = (StyledText) consoleOutputViewer.getControl();
		plainTab.setControl(consoleText);
		consoleOutputViewer
				.configure(new ErlangConsoleSourceViewerConfiguration());

		consoleText.setFont(JFaceResources.getTextFont());
		consoleText.setEditable(false);
		consoleText.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				try {
					final int ofs = consoleText.getOffsetAtLocation(new Point(
							e.x, e.y));
					final IoRequest req = model.findAtPos(ofs);
					clearMarks();
					if (req.getSender() != null) {
						final List<IoRequest> reqs = model.getAllFrom(req
								.getSender());
						markRequests(reqs);
					}
				} catch (final Exception ex) {
				}
			}
		});
		consoleText.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseHover(final MouseEvent e) {
				try {
					final int ofs = consoleText.getOffsetAtLocation(new Point(
							e.x, e.y));
					final IoRequest req = model.findAtPos(ofs);
					consoleText.setToolTipText("sent by "
							+ ErlangProcess.toLocalPid(req.getSender()));
				} catch (final Exception ex) {
				}
			}
		});
		consoleText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				boolean isHistoryCommand = ((e.stateMask & SWT.CTRL) == SWT.CTRL)
						&& ((e.keyCode == SWT.ARROW_UP) || (e.keyCode == SWT.ARROW_DOWN));
				if ((e.character != (char) 0) || isHistoryCommand) {
					createInputField(e.character);
					e.doit = false;
				}
			}
		});

		final TabItem tracerTab = new TabItem(tabFolder, SWT.NONE);
		tracerTab.setText("Tracer");

		consoleTable = new TableViewer(tabFolder, SWT.BORDER);
		final Table table = consoleTable.getTable();
		tracerTab.setControl(table);
		table.setHeaderVisible(true);
		consoleTable.setContentProvider(new IoRequestContentProvider());
		consoleTable.setLabelProvider(new IoRequestLabelProvider());
		consoleTable.setInput(fDoc);
		final Table tbl = (Table) consoleTable.getControl();
		tbl.setFont(JFaceResources.getTextFont());
		tbl.setLinesVisible(true);
		initializeToolBar();
	}

	void createInputField(char first) {
		if (first == SWT.ESC) {
			return;
		}
		consoleText.setSelection(consoleText.getCharCount());
		Rectangle rect = consoleText.getClientArea();
		Point relpos = consoleText.getLocationAtOffset(consoleText
				.getCharCount());

		final Shell container = new Shell(Display.getDefault(),
				SWT.APPLICATION_MODAL);
		container.setLayout(new FillLayout());
		consoleInputViewer = new SourceViewer(container, null, SWT.MULTI
				| SWT.WRAP | SWT.V_SCROLL);
		consoleInputViewer.setDocument(new Document());
		consoleInputViewer
				.configure(new ErlangConsoleSourceViewerConfiguration());
		consoleInput = (StyledText) consoleInputViewer.getControl();
		consoleInput.setParent(container);
		container.setAlpha(220);

		int b = 1;
		Point screenPos = consoleText.toDisplay(relpos.x - b, relpos.y - b);
		container.setLocation(screenPos);
		container.setSize(rect.width - relpos.x, rect.height - relpos.y);

		consoleInput.addKeyListener(new KeyAdapter() {
			int navIndex = history.size();

			@Override
			public void keyPressed(final KeyEvent e) {
				boolean historyMode = (e.stateMask & SWT.CTRL) == SWT.CTRL;
				if (e.keyCode == 13 && isInputComplete()) {
					sendInput();
					container.close();
					e.doit = false;
				} else if (e.keyCode == 13) {
					Rectangle loc = container.getBounds();
					int topIndex = consoleInput.getTopIndex();
					int lineCount = consoleInput.getLineCount();
					int lineHeight = consoleInput.getLineHeight();
					int visibleLines = loc.height / lineHeight;
					int maxLines = consoleText.getSize().y / lineHeight - 1;
					if (topIndex + visibleLines - 1 <= lineCount
							&& visibleLines < maxLines) {
						container.setBounds(loc.x, loc.y - lineHeight,
								loc.width, loc.height + lineHeight);
						consoleInput.setTopIndex(lineCount - visibleLines + 1);
					}
				} else if (historyMode && e.keyCode == SWT.ARROW_UP) {
					if (navIndex > 0) {
						navIndex--;
					}
					String s = (history.size() > navIndex) ? history
							.get(navIndex) : "";
					consoleInput.setText(s);
					consoleInput.setSelection(consoleInput.getText().length());
					fixPosition(container);
				} else if (historyMode && e.keyCode == SWT.ARROW_DOWN) {
					if (navIndex < history.size()) {
						navIndex++;
					}
					final String s = navIndex < history.size() ? history
							.get(navIndex) : "";
					consoleInput.setText(s);
					consoleInput.setSelection(consoleInput.getText().length());
					fixPosition(container);
				} else if (e.keyCode == SWT.ESC) {
					container.close();
				}
			}

		});
		consoleInput.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				// container.close();
			}
		});
		consoleInput.setFont(consoleText.getFont());
		consoleInput.setBackground(consoleText.getBackground());
		consoleInput.setWordWrap(true);

		if (first != 0) {
			consoleInput.setText("" + first);
		} else {
			final String s = history.size() > 0 ? history.get(0) : "";
			consoleInput.setText(s);
			fixPosition(container);
		}
		consoleInput.setSelection(consoleInput.getCharCount());

		container.setVisible(true);
		consoleInput.setFocus();
	}

	private void fixPosition(final Shell container) {
		Rectangle loc = container.getBounds();
		int lineCount = consoleInput.getLineCount();
		int lineHeight = consoleInput.getLineHeight();
		int visibleLines = loc.height / lineHeight;
		int maxLines = consoleText.getSize().y / lineHeight - 1;
		int lines = Math.max(Math.min(maxLines, lineCount) - visibleLines,
				visibleLines);
		if (visibleLines - 1 <= lineCount) {
			container.setBounds(loc.x, loc.y - lineHeight * lines, loc.width,
					loc.height + lineHeight * lines);
		}
	}

	boolean isInputComplete() {
		try {
			String str = consoleInput.getText();
			final OtpErlangObject o = ErlideBackend.parseString(str);
			if (o instanceof OtpErlangList && ((OtpErlangList) o).arity() == 0) {
				return false;
			}
		} catch (final BackendException e) {
			return false;
		}
		return true;
	}

	protected void sendInput() {
		String s = consoleInput.getText();
		input(s);
		consoleInput.setText("");
		refreshView();
	}

	private void updateTableView() {
		consoleTable.setInput(fDoc);
	}

	private void updateConsoleView() {
		consoleText.setRedraw(false);
		consoleText.setText("");
		for (final IoRequest req : model.getContentList()) {
			consoleText.append(req.getMessage());
			if (fColored) {
				markRequest(req);
			}
		}
		consoleText.setRedraw(true);
		consoleText.setSelection(consoleText.getCharCount());
	}

	Color getColor(final OtpErlangPid sender) {
		int ix = 0;
		for (final Object element : pids) {
			final OtpErlangPid pid = (OtpErlangPid) element;
			if (pid.equals(sender)) {
				break;
			}
			ix++;
		}
		if (ix < colors.length - 1) {
			return colors[ix % 19 + 1];
		}
		return colors[0];
	}

	private static void addDropDown(final ToolItem item, final Menu menu) {
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event event) {
				if (event.detail == SWT.ARROW) {
					final Rectangle rect = item.getBounds();
					Point pt = new Point(rect.x, rect.y + rect.height);
					pt = item.getParent().toDisplay(pt);
					menu.setLocation(pt.x, pt.y);
					menu.setVisible(true);
				}
			}
		});
	}

	public void input(String data) {
		model.input(data);
		shell.send(data);
		addToHistory(data.trim());
	}

	void refreshView() {
		if (consoleText.isDisposed()) {
			return;
		}
		try {
			updateConsoleView();
			updateTableView();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void addToHistory(final String in) {
		if (history.indexOf(in) != -1) {
			history.remove(in);
		}
		history.add(in);
		if (history.size() > 50) {
			history.remove(0);
		}
	}

	public List<String> getHistory() {
		return history;
	}

	final class IoRequestContentProvider implements IStructuredContentProvider {
		public void dispose() {
		}

		public void inputChanged(final Viewer viewer, final Object oldInput,
				final Object newInput) {
			fDoc = (ErlConsoleDocument) newInput;

			if (fDoc == null) {
				return;
			}

			final Table tbl = (Table) viewer.getControl();
			tbl.setRedraw(false);
			// TODO hmmm the flicker is still there....
			try {
				final TableColumn[] cs = tbl.getColumns();
				for (final TableColumn cc : cs) {
					cc.dispose();
				}

				for (final IoRequest req : model.getContentList()) {
					final OtpErlangPid pid = fGroupByLeader ? req.getLeader()
							: req.getSender();
					pids.add(pid);
				}

				for (final OtpErlangPid pid : pids) {
					TableColumn c;
					c = new TableColumn(tbl, SWT.NONE);
					c.setText(ErlangProcess.toLocalPid(pid));
					c.setData(pid);
					c.setWidth(100);
				}
			} finally {
				tbl.setRedraw(true);
			}

		}

		public Object[] getElements(final Object inputElement) {
			return model.getContentList().toArray();
		}
	}

	final class IoRequestLabelProvider implements ITableLabelProvider,
			IColorProvider {
		public void addListener(final ILabelProviderListener listener) {
		}

		public void dispose() {
		}

		public boolean isLabelProperty(final Object element,
				final String property) {
			return true;
		}

		public void removeListener(final ILabelProviderListener listener) {
		}

		public Image getColumnImage(final Object element, final int columnIndex) {
			return null;
		}

		public String getColumnText(final Object element, final int columnIndex) {
			if (element instanceof IoRequest) {
				final IoRequest req = (IoRequest) element;
				final Table tbl = (Table) consoleTable.getControl();
				final TableColumn c = tbl.getColumn(columnIndex);
				final OtpErlangPid pid = fGroupByLeader ? req.getLeader() : req
						.getSender();
				if (c.getData().equals(pid)) {
					return req.getMessage();
				}
				return null;
			}
			return null;
		}

		public Color getBackground(final Object element) {
			final IoRequest req = (IoRequest) element;
			if (fColored) {
				return getColor(fGroupByLeader ? req.getLeader() : req
						.getSender());
			}
			return null;
		}

		public Color getForeground(final Object element) {
			// IoRequest req = (IoRequest) element;
			return null;
		}

	}

	public void markRequests(final List<IoRequest> reqs) {
		for (final Object element0 : reqs) {
			final IoRequest element = (IoRequest) element0;
			markRequest(element);
		}
	}

	public void markRequest(final IoRequest req) {
		final StyleRange range = new StyleRange();
		range.start = req.getStart();
		range.length = req.getLength();
		range.background = getColor(fGroupByLeader ? req.getLeader() : req
				.getSender());
		consoleText.setStyleRange(range);
	}

	public void clearMarks() {
		final StyleRange range = new StyleRange();
		range.start = 0;
		range.length = consoleText.getCharCount();
		consoleText.setStyleRange(range);
	}

	public void setInput(final String str) {
		consoleInput.setText(str);
		consoleInput.setSelection(str.length());
	}

	@Override
	public void setFocus() {
	}

	private void initializeToolBar() {
		// IToolBarManager toolBarManager = getViewSite().getActionBars()
		// .getToolBarManager();
	}

	public void changed(ErlConsoleModel erlConsoleModel) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				refreshView();
			}
		});
	}

}
