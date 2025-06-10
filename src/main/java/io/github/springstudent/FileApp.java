package io.github.springstudent;

import com.sun.net.httpserver.HttpServer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.net.InetSocketAddress;

/**
 * @author ZhouNing
 * @date 2025/6/9 16:01
 **/
public class FileApp {
    private static final String contextPath = "/httpFileShare";
    private JFrame mainFrame;
    private TrayIcon trayIcon;
    private JTable table;
    private DefaultTableModel tableModel;
    private HttpServer httpServer;
    private String serverIp;
    private Integer serverPort;

    public FileApp() {
        mainWindow();
        startHttp();
        trayIcon();
    }

    private void mainWindow() {
        mainFrame = new JFrame("HttpFileShare");
        mainFrame.setSize(900, 750);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout());
        mainFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(FileApp.class.getResource("/program48.png")));

        //分享按钮
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton shareButton = new JButton("Share");
        shareButton.addActionListener(e -> {
            share();
        });
        topPanel.add(shareButton);
        // 表格
        tableModel = new DefaultTableModel(new Object[]{"id", "file", "url", "operate"}, 0);
        table = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };
        table.setRowHeight(30);
        table.getColumn("operate").setCellRenderer(new ButtonRenderer());
        table.getColumn("operate").setCellEditor(new ButtonEditor(table, tableModel));
        // 设置列宽
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        table.getColumnModel().getColumn(2).setPreferredWidth(250);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    setToolTipText(value.toString());
                } else {
                    setToolTipText(null);
                }
                return c;
            }
        });
        //表格容器
        JScrollPane scrollPane = new JScrollPane(table);
        mainFrame.add(topPanel, BorderLayout.NORTH);
        mainFrame.add(scrollPane, BorderLayout.CENTER);
        mainFrame.setVisible(true);
    }

    private String fileHttpUrl(String fileId) {
        return "http://" + serverIp + ":" + serverPort + contextPath + "/" + fileId;
    }

    private void trayIcon() {
        if (!SystemTray.isSupported()) {
            System.err.println("don't support tray");
            return;
        }
        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().getImage(FileApp.class.getResource("/icon.png"));
        PopupMenu popup = new PopupMenu();
        MenuItem openItem = new MenuItem("main");
        openItem.addActionListener(e -> SwingUtilities.invokeLater(() -> mainFrame.setVisible(true)));
        MenuItem shareItem = new MenuItem("share");
        shareItem.addActionListener(e -> SwingUtilities.invokeLater(this::share));
        MenuItem exitItem = new MenuItem("exit");
        exitItem.addActionListener(e -> {
            tray.remove(trayIcon);
            stopHttp();
            System.exit(0);
        });
        popup.add(openItem);
        popup.add(shareItem);
        popup.addSeparator();
        popup.add(exitItem);
        trayIcon = new TrayIcon(image, "HttpFileShare", popup);
        trayIcon.setImageAutoSize(true);
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
        trayIcon.displayMessage("HttpFileShare", String.format("Share Your File Using Http:\nIP=%s,PORT=%s", serverIp, serverPort), TrayIcon.MessageType.INFO);
    }

    private void share() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(mainFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            String fileId = MixUtils.randomString(6);
            while (FileRegistry.contains(fileId)) {
                fileId = MixUtils.randomString(6);
            }
            String url = fileHttpUrl(fileId);
            FileRegistry.put(fileId, fileChooser.getSelectedFile());
            tableModel.addRow(new Object[]{fileId, filePath, url, "cancel"});
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
            if (trayIcon != null) {
                trayIcon.displayMessage("share success", url, TrayIcon.MessageType.INFO);
            }
        }
    }

    private void startHttp() {
        try {
            serverIp = MixUtils.getIp().getHostAddress();
            serverPort = MixUtils.getPort();
            httpServer = HttpServer.create(new InetSocketAddress(serverIp, serverPort), 0);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start HTTP server", e);
        }
        httpServer.createContext(contextPath, new FileHandler());
        httpServer.setExecutor(null);
        httpServer.start();
    }

    private void stopHttp() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    static class ButtonRenderer extends JPanel implements TableCellRenderer {
        private final JButton cancelBtn = new JButton("cancel");
        private final JButton copyBtn = new JButton("copy");

        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
            cancelBtn.setFocusable(false);
            copyBtn.setFocusable(false);
            add(cancelBtn);
            add(copyBtn);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }
            return this;
        }
    }

    static class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        private final JButton cancelBtn = new JButton("cancel");
        private final JButton copyBtn = new JButton("share");
        private JTable table;
        private DefaultTableModel model;

        public ButtonEditor(JTable table, DefaultTableModel model) {
            this.table = table;
            this.model = model;
            panel.add(cancelBtn);
            panel.add(copyBtn);
            // 取消分享按钮
            cancelBtn.addActionListener(e -> {
                int row = table.getEditingRow();
                fireEditingStopped();
                SwingUtilities.invokeLater(() -> {
                    if (row >= 0 && row < table.getRowCount()) {
                        String id = (String) model.getValueAt(row, 0);
                        FileRegistry.del(id);
                        ((DefaultTableModel) table.getModel()).removeRow(row);
                    }
                });
            });
            // 复制URL按钮
            copyBtn.addActionListener(e -> {
                int row = table.getEditingRow();
                if (row >= 0) {
                    String url = (String) model.getValueAt(row, 2);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
                    JOptionPane.showMessageDialog(table, "copy success:\n" + url);
                }
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }


    public static void main(String[] args) {
        new FileApp();
    }
}