package com.gvaneyck.runesorter;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.gvaneyck.util.Callback;
import com.gvaneyck.util.Tuple;

public class SorterWindow extends JFrame {
    private static final long serialVersionUID = -4909493574753556479L;

    public static final int MIN_WIDTH = 250;
    public static final int MIN_HEIGHT = 520;
    
    public static final int EXPAND_WIDTH = 250;
    
    private final JButton btnSort = new JButton("Sort Alphabetically");
    private final JButton btnMoveUp = new JButton("Move Up");
    private final JButton btnMoveDown = new JButton("Move Down");

    private final JSeparator sep1 = new JSeparator(JSeparator.HORIZONTAL);

    private final JLabel lblRunePages = new JLabel("Rune Pages");
    private final RunePageListModel lstRuneModel = new RunePageListModel();
    private final JList lstRunePages = new JList(lstRuneModel);
    private final JScrollPane lstRuneScroll = new JScrollPane(lstRunePages);

    private final JLabel lblMasteryPages = new JLabel("Mastery Pages");
    private final MasteryPageListModel lstMasteryModel = new MasteryPageListModel();
    private final JList lstMasteryPages = new JList(lstMasteryModel);
    private final JScrollPane lstMasteryScroll = new JScrollPane(lstMasteryPages);
    
    private final JLabel lblSide1 = new JLabel("");
    
    private final JSeparator sep2 = new JSeparator(JSeparator.HORIZONTAL);
    
    private final JLabel lblSearch = new JLabel("Search & Copy");
    private final JTextField txtSearch = new JTextField();
    
    private final JLabel lblRunePages2 = new JLabel("Rune Pages");
    private final RunePageListModel lstRuneModel2 = new RunePageListModel();
    private final JList lstRunePages2 = new JList(lstRuneModel2);
    private final JScrollPane lstRuneScroll2 = new JScrollPane(lstRunePages2);
    
    private final JLabel lblSide2 = new JLabel("");
    
    private final JButton btnCopy = new JButton("^^^ Copy Page ^^^");

    private int lastSelectedPage = -1;
    private int lastSelectedMasteries = -1;
    private boolean changingSelection = false;
    private boolean isExpanded = false;
    
    private int width = MIN_WIDTH;
    private int height = MIN_HEIGHT;

    // Callbacks
    public Callback runeSorterListener;
    public Callback masterySorterListener;
    public Callback runeSwapListener;
    public Callback masterySwapListener;
    public Callback runeSelectListener;
    public Callback masterySelectListener;
    
    public Callback searchListener;
    public Callback runeSelectListener2;
    
    public Callback copyListener;

    public SorterWindow() {
        setTitle("Rune/Mastery Page Manager");
        
        // GUI settings
        lstRunePages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lblSide1.setVerticalAlignment(SwingConstants.TOP);
        lblSide2.setVerticalAlignment(SwingConstants.TOP);

        // Initially grey out buttons
        btnSort.setEnabled(false);
        btnMoveUp.setEnabled(false);
        btnMoveDown.setEnabled(false);
        btnCopy.setVisible(false);

        // Add the items
        Container pane = getContentPane();
        pane.setLayout(null);

        pane.add(btnSort);
        pane.add(btnMoveUp);
        pane.add(btnMoveDown);

        pane.add(sep1);

        pane.add(lblRunePages);
        pane.add(lstRuneScroll);
        pane.add(lblMasteryPages);
        pane.add(lstMasteryScroll);
        
        pane.add(lblSide1);
        
        pane.add(sep2);
        
        pane.add(lblSearch);
        pane.add(txtSearch);

        pane.add(lblRunePages2);
        pane.add(lstRuneScroll2);
        
        pane.add(lblSide2);
        
        pane.add(btnCopy);
        
        // Minor Z order edit
        pane.setComponentZOrder(btnCopy, 0);
        pane.setComponentZOrder(sep2, 1);

        // Listeners
        btnSort.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (lastSelectedPage != -1)
                    if (runeSorterListener != null)
                        runeSorterListener.callback(null);

                if (lastSelectedMasteries != -1)
                    if (masterySorterListener != null)
                        masterySorterListener.callback(null);
            }
        });

        btnMoveUp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (lastSelectedPage != -1 && lastSelectedPage != 0) {
                    if (runeSwapListener != null)
                        runeSwapListener.callback(new Tuple(lastSelectedPage, lastSelectedPage - 1));

                    lastSelectedPage--;
                    lstRunePages.setSelectedIndex(lastSelectedPage);
                }
                if (lastSelectedMasteries != -1 && lastSelectedMasteries != 0) {
                    if (masterySwapListener != null)
                        masterySwapListener.callback(new Tuple(lastSelectedPage, lastSelectedPage - 1));

                    lastSelectedMasteries--;
                    lstMasteryPages.setSelectedIndex(lastSelectedMasteries);
                }
            }
        });

        btnMoveDown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (lastSelectedPage != -1 && lastSelectedPage != lstRunePages.getModel().getSize() - 1) {
                    if (runeSwapListener != null)
                        runeSwapListener.callback(new Tuple(lastSelectedPage, lastSelectedPage + 1));

                    lastSelectedPage++;
                    lstRunePages.setSelectedIndex(lastSelectedPage);
                }
                if (lastSelectedMasteries != -1 && lastSelectedMasteries != lstMasteryPages.getModel().getSize() - 1) {
                    if (masterySwapListener != null)
                        masterySwapListener.callback(new Tuple(lastSelectedPage, lastSelectedPage + 1));

                    lastSelectedMasteries++;
                    lstMasteryPages.setSelectedIndex(lastSelectedMasteries);
                }
            }
        });

        lstRunePages.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (changingSelection)
                    return;

                changingSelection = true;
                lastSelectedPage = lstRunePages.getSelectedIndex();
                lstMasteryPages.clearSelection();
                lastSelectedMasteries = -1;
                if (!lblSide2.getText().equals(""))
                    btnCopy.setVisible(true);
                changingSelection = false;
                
                if (runeSelectListener != null)
                    runeSelectListener.callback(lastSelectedPage);
            }
        });

        lstMasteryPages.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (changingSelection)
                    return;

                changingSelection = true;
                lastSelectedMasteries = lstMasteryPages.getSelectedIndex();
                lastSelectedPage = -1;
                lstRunePages.clearSelection();
                btnCopy.setVisible(false);
                changingSelection = false;

                if (masterySelectListener != null)
                    masterySelectListener.callback(lastSelectedPage);
            }
        });
        
        txtSearch.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n' && searchListener != null)
                    searchListener.callback(txtSearch.getText());
            }
            
            public void keyReleased(KeyEvent e) { }
            public void keyPressed(KeyEvent e) { }
        });
        
        lstRunePages2.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (runeSelectListener2 != null)
                    runeSelectListener2.callback(lstRunePages2.getSelectedIndex());
                if (lastSelectedPage != -1)
                    btnCopy.setVisible(true);
            }
        });
        
        btnCopy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!lblSide1.getText().equals("") && !lblSide2.getText().equals("") && copyListener != null) {
                    copyListener.callback(new Tuple(lastSelectedPage, lstRunePages2.getSelectedIndex()));
                }
            }
        });

        pane.addHierarchyBoundsListener(new HierarchyBoundsListener() {
            public void ancestorMoved(HierarchyEvent e) {}

            public void ancestorResized(HierarchyEvent e) {
                Dimension d = getSize();
                width = d.width;
                height = d.height;
                doMyLayout();
            }
        });

        // Window settings
        setSize(width, height);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }
    
    public void enableButtons() {
        btnSort.setEnabled(true);
        btnMoveUp.setEnabled(true);
        btnMoveDown.setEnabled(true);
    }
    
    public void disableButtons() {
        btnSort.setEnabled(false);
        btnMoveUp.setEnabled(false);
        btnMoveDown.setEnabled(false);
    }
    
    public void updateRunePages(List<RunePage> pages) {
        lstRuneModel.clear();
        for (RunePage page : pages)
            lstRuneModel.add(page);
        lstRuneModel.update();
    }
    
    public void updateMasteryPages(List<MasteryPage> pages) {
        lstMasteryModel.clear();
        for (MasteryPage page : pages)
            lstMasteryModel.add(page);
        lstMasteryModel.update();
    }
    
    public void updateRunePages2(List<RunePage> pages) {
        lstRuneModel2.clear();
        lstRunePages2.clearSelection();
        if (pages != null)
            for (RunePage page : pages)
                lstRuneModel2.add(page);
        lstRuneModel2.update();
        runeSelectListener2.callback(-1);
    }
    
    public void setInfo1(String text) {
        lblSide1.setText(text);
        adjustSize();
    }
    
    public void setInfo2(String text) {
        lblSide2.setText(text);
        adjustSize();
    }
    
    private void adjustSize() {
        if (isExpanded && lblSide1.getText().equals("") && lblSide2.getText().equals("")) {
            isExpanded = false;
            width -= EXPAND_WIDTH;
            setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
            setSize(width, height);
        }
        if (!isExpanded && (!lblSide1.getText().equals("") || !lblSide2.getText().equals(""))) {
            isExpanded = true;
            width += EXPAND_WIDTH;
            setMinimumSize(new Dimension(MIN_WIDTH + EXPAND_WIDTH, MIN_HEIGHT));
            setSize(width, height);
        }
    }
    
    private void doMyLayout() {
        Insets i = getInsets();
        int twidthfull = width - i.left - i.right;
        int twidth = twidthfull - (isExpanded ? EXPAND_WIDTH : 0);
        int theight = height - i.top - i.bottom;

        btnSort.setBounds(5, 5, twidth - 10, 24);
        btnMoveUp.setBounds(5, 34, twidth / 2 - 10, 24);
        btnMoveDown.setBounds(5 + twidth / 2, 34, twidth / 2 - 10, 24);

        sep1.setBounds(5, 63, twidth - 10, 5);

        int scrollHeight = (theight - 165) / 3;
        lblRunePages.setBounds(5, 65, twidth - 9, 15);
        lstRuneScroll.setBounds(5, 80, twidth - 9, scrollHeight);
        lblMasteryPages.setBounds(5, 80 + scrollHeight, twidth - 9, 15);
        lstMasteryScroll.setBounds(5, 95 + scrollHeight, twidth - 9, scrollHeight);
        
        lblSide1.setBounds(twidth + 5, 5, EXPAND_WIDTH - 10, 95 + 2 * scrollHeight);
        
        sep2.setBounds(5, 100 + 2 * scrollHeight, twidthfull - 10, 5);
        
        lblSearch.setBounds(5, 105 + 2 * scrollHeight, twidth - 9, 15);
        txtSearch.setBounds(5, 120 + 2 * scrollHeight, twidth - 9, 24);
        
        lblRunePages2.setBounds(5, 145 + 2 * scrollHeight, twidth - 9, 15);
        lstRuneScroll2.setBounds(5, 160 + 2 * scrollHeight, twidth - 9, scrollHeight);
        
        lblSide2.setBounds(twidth + 5, 120 + 2 * scrollHeight, EXPAND_WIDTH - 10, scrollHeight + 30);
        
        btnCopy.setBounds(twidth + 45, 87 + 2 * scrollHeight, EXPAND_WIDTH - 90, 24);
    }
}
