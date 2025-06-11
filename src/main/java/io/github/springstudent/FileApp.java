package io.github.springstudent;

import com.sun.net.httpserver.HttpServer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.Scanner;

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
    private boolean headless;

    public FileApp() {
        this.serverIp = MixUtils.getIp();
        this.serverPort = MixUtils.getPort();
        this.headless = GraphicsEnvironment.isHeadless();
        if (!headless) {
            mainWindow();
            trayIcon();
        }
        startHttp();
        new Thread(this::consoleInputLoop, "httpFileShareCmd").start();
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
        JButton shareButton = new JButton("share");
        shareButton.addActionListener(e -> {
            share();
        });
        topPanel.add(shareButton);
        // Cancel All 按钮
        JButton cancelAllButton = new JButton("cancel all");
        cancelAllButton.addActionListener(e -> {
            FileRegistry.clear();
            tableModel.setRowCount(0);
        });
        topPanel.add(cancelAllButton);
        // 表格
        tableModel = new DefaultTableModel(new Object[]{"id", "path", "url", "operate"}, 0);
        table = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };
        table.setRowHeight(30);
        table.getColumn("operate").setCellRenderer(new ButtonRenderer());
        table.getColumn("operate").setCellEditor(new ButtonEditor(table, tableModel, mainFrame));
        // 设置列宽
        table.getColumnModel().getColumn(0).setPreferredWidth(20);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        table.getColumnModel().getColumn(2).setPreferredWidth(230);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);
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
        MenuItem shareItem = new MenuItem("share");
        shareItem.addActionListener(e -> SwingUtilities.invokeLater(this::share));
        MenuItem exitItem = new MenuItem("exit");
        exitItem.addActionListener(e -> {
            tray.remove(trayIcon);
            stopHttp();
            System.exit(0);
        });
        popup.add(shareItem);
        popup.addSeparator();
        popup.add(exitItem);
        trayIcon = new TrayIcon(image, "HttpFileShare", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 检查是否为鼠标左键点击
                if (e.getButton() == MouseEvent.BUTTON1) {
                    SwingUtilities.invokeLater(() -> mainFrame.setVisible(true));
                }
            }
        });
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
        trayIcon.displayMessage("HttpFileShare", String.format("Share Your File Using Http:\nIP=%s,PORT=%s", serverIp, serverPort), TrayIcon.MessageType.INFO);
    }

    private void share() {
        if (headless) {
            System.out.println("Cannot open file chooser in headless mode. Use command line: share [file1] [file2] ...");
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true); // 允许多选
        int result = fileChooser.showOpenDialog(mainFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            String lastUrl = null;
            File[] files = fileChooser.getSelectedFiles();
            for (File file : files) {
                String filePath = file.getAbsolutePath();
                String fileId = MixUtils.randomString(6);
                while (FileRegistry.contains(fileId)) {
                    fileId = MixUtils.randomString(6);
                }
                String url = fileHttpUrl(fileId);
                FileRegistry.put(fileId, file);
                tableModel.addRow(new Object[]{fileId, filePath, url, "cancel"});
                lastUrl = url;
            }
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(lastUrl), null);
            if (trayIcon != null && files.length > 0) {
                trayIcon.displayMessage("share success", files.length + " files shared", TrayIcon.MessageType.INFO);
            }
        }
    }

    private void startHttp() {
        try {
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

        private final JButton qrBtn = new JButton("qrcode");

        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
            cancelBtn.setFocusable(false);
            copyBtn.setFocusable(false);
            qrBtn.setFocusable(false);
            add(cancelBtn);
            add(copyBtn);
            add(qrBtn);
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

        private final JButton qrBtn = new JButton("qrcode");
        private JFrame mainFrame;

        private JTable table;
        private DefaultTableModel model;

        public ButtonEditor(JTable table, DefaultTableModel model, JFrame mainFrame) {
            this.table = table;
            this.model = model;
            this.mainFrame = mainFrame;
            panel.add(cancelBtn);
            panel.add(copyBtn);
            panel.add(qrBtn);

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
            //二维码
            qrBtn.addActionListener(e -> {
                int row = table.getEditingRow();
                if (row >= 0) {
                    try {
                        // 生成二维码图片
                        int size = 300;
                        BufferedImage image = MixUtils.qrCode((String) model.getValueAt(row, 2));
                        // UI 组件构建
                        JLabel qrLabel = new JLabel(new ImageIcon(image));
                        JButton downloadButton = new JButton("Download");
                        JDialog dialog = new JDialog(mainFrame, "QR Code", true);
                        dialog.setLayout(new BorderLayout());
                        dialog.add(qrLabel, BorderLayout.CENTER);
                        dialog.add(downloadButton, BorderLayout.SOUTH);
                        dialog.setSize(size + 50, size + 100);
                        dialog.setLocationRelativeTo(mainFrame);

                        downloadButton.addActionListener(ev -> {
                            try {
                                JFileChooser fileChooser = new JFileChooser();
                                fileChooser.setSelectedFile(new File("qrcode.png"));
                                int ret = fileChooser.showSaveDialog(dialog);
                                if (ret == JFileChooser.APPROVE_OPTION) {
                                    File outputFile = fileChooser.getSelectedFile();
                                    ImageIO.write(image, "png", outputFile);
                                    JOptionPane.showMessageDialog(dialog, "Saved to:\n" + outputFile.getAbsolutePath());
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(dialog, "Failed to save QR code:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                        dialog.setVisible(true);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(mainFrame, "Failed to generate QR code:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
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

    private void consoleInputLoop() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Console command mode ready. Type `help` for usage.");
            while (true) {
                System.out.print("> ");
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                java.util.List<String> tokens = MixUtils.parseCmdArgs(line);
                if (tokens.isEmpty()) continue;
                String cmd = tokens.get(0).toLowerCase();
                java.util.List<String> args = tokens.subList(1, tokens.size());
                switch (cmd) {
                    case "share":
                        if (args.isEmpty()) {
                            System.out.println("Usage: share [file1] [file2] ...");
                            break;
                        }
                        for (String path : args) {
                            File file = new File(path);
                            if (!file.exists() || !file.isFile()) {
                                System.out.println("Invalid file: " + path);
                                continue;
                            }
                            String fileId = MixUtils.randomString(6);
                            while (FileRegistry.contains(fileId)) fileId = MixUtils.randomString(6);
                            FileRegistry.put(fileId, file);
                            String url = fileHttpUrl(fileId);
                            if (!headless) {
                                String finalFileId = fileId;
                                SwingUtilities.invokeLater(() ->
                                        tableModel.addRow(new Object[]{finalFileId, file.getAbsolutePath(), url, "cancel"}));
                            }
                            System.out.println("id: " + fileId);
                            System.out.println("path: " + file.getAbsolutePath());
                            System.out.println("url: " + url);
                        }
                        break;
                    case "cancel":
                        if (args.size() == 1 && "all".equalsIgnoreCase(args.get(0))) {
                            FileRegistry.clear();
                            if (!headless) SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
                            System.out.println("All shares canceled.");
                            break;
                        }
                        if (args.isEmpty()) {
                            System.out.println("Usage: cancel [fileId1] [fileId2] ... or cancel all");
                            break;
                        }
                        for (String id : args) {
                            if (FileRegistry.contains(id)) {
                                FileRegistry.del(id);
                                if (!headless) {
                                    String finalId = id;
                                    SwingUtilities.invokeLater(() -> removeRowById(finalId));
                                }
                                System.out.println("Canceled: " + id);
                            } else {
                                System.out.println("No such id: " + id);
                            }
                        }
                        break;
                    case "list":
                        System.out.println("Current shared files:");
                        for (String id : FileRegistry.list()) {
                            File f = FileRegistry.get(id);
                            System.out.printf("id: %s\npath: %s\nurl: %s\n\n", id, f.getAbsolutePath(), fileHttpUrl(id));
                        }
                        break;
                    case "exit":
                        System.out.println("Shutting down...");
                        stopHttp();
                        System.exit(0);
                        return;
                    case "help":
                        System.out.println("Commands:\n  share [file1] [file2] ... - Share one or more files\n  cancel [id1] [id2] ...    - Cancel share(s) by fileId\n  cancel all                - Cancel all shared files\n  list                      - List shared files\n  exit                      - Quit program\n");
                        break;
                    default:
                        System.out.println("Unknown command: " + cmd + " (type `help` for usage)");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeRowById(String id) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (id.equals(tableModel.getValueAt(i, 0))) {
                tableModel.removeRow(i);
                break;
            }
        }
    }

    public static void main(String[] args) {
        String ip = null;
        Integer port = null;
        if (args.length > 0) {
            ip = args[0];
        }
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port argument: " + args[1] + ", using default");
            }
        }
        if (ip != null) {
            System.setProperty("httpFileShare.ip", ip);
        }
        if (port != null) {
            System.setProperty("httpFileShare.port", port.toString());
        }
        new FileApp();
    }

}